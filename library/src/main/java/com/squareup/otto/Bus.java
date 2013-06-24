/*
 * Copyright (C) 2012 Square, Inc.
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squareup.otto;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;


/**
 * Dispatches events to listeners, and provides ways for listeners to register themselves.
 *
 * <p>The Bus allows publish-subscribe-style communication between components without requiring the components to
 * explicitly register with one another (and thus be aware of each other).  It is designed exclusively to replace
 * traditional Android in-process event distribution using explicit registration or listeners. It is <em>not</em> a
 * general-purpose publish-subscribe system, nor is it intended for interprocess communication.
 *
 * <h2>Receiving Events</h2>
 * To receive events, an object should:
 * <ol>
 * <li>Expose a public method, known as the <i>event handler</i>, which accepts a single argument of the type of event
 * desired;</li>
 * <li>Mark it with a {@link com.squareup.otto.Subscribe} annotation;</li>
 * <li>Pass itself to an Bus instance's {@link #register(Object)} method.
 * </li>
 * </ol>
 *
 * <h2>Posting Events</h2>
 * To post an event, simply provide the event object to the {@link #post(Object)} method.  The Bus instance will
 * determine the type of event and route it to all registered listeners.
 *
 * <p>Events are routed based on their type &mdash; an event will be delivered to any handler for any type to which the
 * event is <em>assignable.</em>  This includes implemented interfaces, all superclasses, and all interfaces implemented
 * by superclasses.
 *
 * <p>When {@code post} is called, all registered handlers for an event are run in sequence, so handlers should be
 * reasonably quick.  If an event may trigger an extended process (such as a database load), spawn a thread or queue it
 * for later.
 *
 * <h2>Handler Methods</h2>
 * Event handler methods must accept only one argument: the event.
 *
 * <p>Handlers should not, in general, throw.  If they do, the Bus will wrap the exception and
 * re-throw it.
 *
 * <p>The Bus by default enforces that all interactions occur on the main thread.  You can provide an alternate
 * enforcement by passing a {@link ThreadEnforcer} to the constructor.
 *
 * <h2>Producer Methods</h2>
 * Producer methods should accept no arguments and return their event type. When a subscriber is registered for a type
 * that a producer is also already registered for, the subscriber will be called with the return value from the
 * producer.
 *
 * <h2>Dead Events</h2>
 * If an event is posted, but no registered handlers can accept it, it is considered "dead."  To give the system a
 * second chance to handle dead events, they are wrapped in an instance of {@link com.squareup.otto.DeadEvent} and
 * reposted.
 *
 * <p>This class is safe for concurrent use.
 *
 * @author Cliff Biffle
 * @author Jake Wharton
 */
public class Bus {
  public static final String DEFAULT_IDENTIFIER = "default";

  /** All registered event handlers, indexed by event type. */
  private final ConcurrentMap<Class<?>, Set<EventHandler>> handlersByType =
          new ConcurrentHashMap<Class<?>, Set<EventHandler>>();

  /** All registered event producers, index by event type. */
  private final ConcurrentMap<Class<?>, EventProducer> producersByType =
          new ConcurrentHashMap<Class<?>, EventProducer>();

  /** Identifier used to differentiate the event bus instance. */
  private final String identifier;

  /** Thread enforcer for register, unregister, and posting events. */
  private final ThreadEnforcer enforcer;

  /** Used to find handler methods in register and unregister. */
  private final HandlerFinder handlerFinder;

  /** Queues of events for the current thread to dispatch. */
  private final ThreadLocal<ConcurrentLinkedQueue<EventWithHandler>> eventsToDispatch =
      new ThreadLocal<ConcurrentLinkedQueue<EventWithHandler>>() {
        @Override protected ConcurrentLinkedQueue<EventWithHandler> initialValue() {
          return new ConcurrentLinkedQueue<EventWithHandler>();
        }
      };

  /** True if the current thread is currently dispatching an event. */
  private final ThreadLocal<Boolean> isDispatching = new ThreadLocal<Boolean>() {
    @Override protected Boolean initialValue() {
      return false;
    }
  };

  /** Creates a new Bus named "default" that enforces actions on the main thread. */
  public Bus() {
    this(DEFAULT_IDENTIFIER);
  }

  /**
   * Creates a new Bus with the given {@code identifier} that enforces actions on the main thread.
   *
   * @param identifier a brief name for this bus, for debugging purposes.  Should be a valid Java identifier.
   */
  public Bus(String identifier) {
    this(ThreadEnforcer.MAIN, identifier);
  }

  /**
   * Creates a new Bus named "default" with the given {@code enforcer} for actions.
   *
   * @param enforcer Thread enforcer for register, unregister, and post actions.
   */
  public Bus(ThreadEnforcer enforcer) {
    this(enforcer, DEFAULT_IDENTIFIER);
  }

  /**
   * Creates a new Bus with the given {@code enforcer} for actions and the given {@code identifier}.
   *
   * @param enforcer Thread enforcer for register, unregister, and post actions.
   * @param identifier A brief name for this bus, for debugging purposes.  Should be a valid Java identifier.
   */
  public Bus(ThreadEnforcer enforcer, String identifier) {
    this(enforcer, identifier, HandlerFinder.ANNOTATED);
  }

  /**
   * Test constructor which allows replacing the default {@code HandlerFinder}.
   *
   * @param enforcer Thread enforcer for register, unregister, and post actions.
   * @param identifier A brief name for this bus, for debugging purposes.  Should be a valid Java identifier.
   * @param handlerFinder Used to discover event handlers and producers when registering/unregistering an object.
   */
  Bus(ThreadEnforcer enforcer, String identifier, HandlerFinder handlerFinder) {
    this.enforcer =  enforcer;
    this.identifier = identifier;
    this.handlerFinder = handlerFinder;
  }

  @Override public String toString() {
    return "[Bus \"" + identifier + "\"]";
  }

  /**
   * Registers all handler methods on {@code object} to receive events and producer methods to provide events.
   * <p>
   * If any subscribers are registering for types which already have a producer they will be called immediately
   * with the result of calling that producer.
   * <p>
   * If any producers are registering for types which already have subscribers, each subscriber will be called with
   * the value from the result of calling the producer.
   *
   * @param object object whose handler methods should be registered.
   * @throws NullPointerException if the object is null.
   */
  public void register(Object object) {
    if (object == null) {
      throw new NullPointerException("Object to register must not be null.");
    }
    enforcer.enforce(this);

    Map<Class<?>, EventProducer> foundProducers = handlerFinder.findAllProducers(object);
    for (Class<?> type : foundProducers.keySet()) {

      final EventProducer producer = foundProducers.get(type);
      EventProducer previousProducer = producersByType.putIfAbsent(type, producer);
      //checking if the previous producer existed
      if (previousProducer != null) {
        throw new IllegalArgumentException("Producer method for type " + type
          + " found on type " + producer.target.getClass()
          + ", but already registered by type " + previousProducer.target.getClass() + ".");
      }
      Set<EventHandler> handlers = handlersByType.get(type);
      if (handlers != null && !handlers.isEmpty()) {
        for (EventHandler handler : handlers) {
          dispatchProducerResultToHandler(handler, producer);
        }
      }
    }

    Map<Class<?>, Set<EventHandler>> foundHandlersMap = handlerFinder.findAllSubscribers(object);
    for (Class<?> type : foundHandlersMap.keySet()) {
      Set<EventHandler> handlers = handlersByType.get(type);
      if (handlers == null) {
        //concurrent put if absent
        Set<EventHandler> handlersCreation = new CopyOnWriteArraySet<EventHandler>();
        handlers = handlersByType.putIfAbsent(type, handlersCreation);
        if (handlers == null) {
            handlers = handlersCreation;
        }
      }
      final Set<EventHandler> foundHandlers = foundHandlersMap.get(type);
      handlers.addAll(foundHandlers);
    }

    for (Map.Entry<Class<?>, Set<EventHandler>> entry : foundHandlersMap.entrySet()) {
      Class<?> type = entry.getKey();
      EventProducer producer = producersByType.get(type);
      if (producer != null && producer.isValid()) {
        Set<EventHandler> foundHandlers = entry.getValue();
        for (EventHandler foundHandler : foundHandlers) {
          if (!producer.isValid()) {
            break;
          }
          if (foundHandler.isValid()) {
            dispatchProducerResultToHandler(foundHandler, producer);
          }
        }
      }
    }
  }

  private void dispatchProducerResultToHandler(EventHandler handler, EventProducer producer) {
    Object event = null;
    try {
      event = producer.produceEvent();
    } catch (InvocationTargetException e) {
      throwRuntimeException("Producer " + producer + " threw an exception.", e);
    }
    if (event == null) {
      return;
    }
    dispatch(event, handler);
  }

  /**
   * Unregisters all producer and handler methods on a registered {@code object}.
   *
   * @param object object whose producer and handler methods should be unregistered.
   * @throws IllegalArgumentException if the object was not previously registered.
   * @throws NullPointerException if the object is null.
   */
  public void unregister(Object object) {
    if (object == null) {
      throw new NullPointerException("Object to unregister must not be null.");
    }
    enforcer.enforce(this);

    Map<Class<?>, EventProducer> producersInListener = handlerFinder.findAllProducers(object);
    for (Map.Entry<Class<?>, EventProducer> entry : producersInListener.entrySet()) {
      final Class<?> key = entry.getKey();
      EventProducer producer = getProducerForEventType(key);
      EventProducer value = entry.getValue();

      if (value == null || !value.equals(producer)) {
        throw new IllegalArgumentException(
            "Missing event producer for an annotated method. Is " + object.getClass()
                + " registered?");
      }
      producersByType.remove(key).invalidate();
    }

    Map<Class<?>, Set<EventHandler>> handlersInListener = handlerFinder.findAllSubscribers(object);
    for (Map.Entry<Class<?>, Set<EventHandler>> entry : handlersInListener.entrySet()) {
      Set<EventHandler> currentHandlers = getHandlersForEventType(entry.getKey());
      Collection<EventHandler> eventMethodsInListener = entry.getValue();

      if (currentHandlers == null || !currentHandlers.containsAll(eventMethodsInListener)) {
        throw new IllegalArgumentException(
            "Missing event handler for an annotated method. Is " + object.getClass()
                + " registered?");
      }

      for (EventHandler handler : currentHandlers) {
        if (eventMethodsInListener.contains(handler)) {
          handler.invalidate();
        }
      }
      currentHandlers.removeAll(eventMethodsInListener);
    }
  }

  /**
   * Posts an event to all registered handlers.  This method will return successfully after the event has been posted to
   * all handlers, and regardless of any exceptions thrown by handlers.
   *
   * <p>If no handlers have been subscribed for {@code event}'s class, and {@code event} is not already a
   * {@link DeadEvent}, it will be wrapped in a DeadEvent and reposted.
   *
   * @param event event to post.
   * @throws NullPointerException if the event is null.
   */
  public void post(Object event) {
    if (event == null) {
      throw new NullPointerException("Event to post must not be null.");
    }
    enforcer.enforce(this);

    Set<Class<?>> dispatchTypes = flattenHierarchy(event.getClass());

    boolean dispatched = false;
    for (Class<?> eventType : dispatchTypes) {
      Set<EventHandler> wrappers = getHandlersForEventType(eventType);

      if (wrappers != null && !wrappers.isEmpty()) {
        dispatched = true;
        for (EventHandler wrapper : wrappers) {
          enqueueEvent(event, wrapper);
        }
      }
    }

    if (!dispatched && !(event instanceof DeadEvent)) {
      post(new DeadEvent(this, event));
    }

    dispatchQueuedEvents();
  }

  /**
   * Queue the {@code event} for dispatch during {@link #dispatchQueuedEvents()}. Events are queued in-order of
   * occurrence so they can be dispatched in the same order.
   */
  protected void enqueueEvent(Object event, EventHandler handler) {
    eventsToDispatch.get().offer(new EventWithHandler(event, handler));
  }

  /**
   * Drain the queue of events to be dispatched. As the queue is being drained, new events may be posted to the end of
   * the queue.
   */
  protected void dispatchQueuedEvents() {
    // don't dispatch if we're already dispatching, that would allow reentrancy and out-of-order events. Instead, leave
    // the events to be dispatched after the in-progress dispatch is complete.
    if (isDispatching.get()) {
      return;
    }

    isDispatching.set(true);
    try {
      while (true) {
        EventWithHandler eventWithHandler = eventsToDispatch.get().poll();
        if (eventWithHandler == null) {
          break;
        }

        if (eventWithHandler.handler.isValid()) {
          dispatch(eventWithHandler.event, eventWithHandler.handler);
        }
      }
    } finally {
      isDispatching.set(false);
    }
  }

  /**
   * Dispatches {@code event} to the handler in {@code wrapper}.  This method is an appropriate override point for
   * subclasses that wish to make event delivery asynchronous.
   *
   * @param event event to dispatch.
   * @param wrapper wrapper that will call the handler.
   */
  protected void dispatch(Object event, EventHandler wrapper) {
    try {
      wrapper.handleEvent(event);
    } catch (InvocationTargetException e) {
      throwRuntimeException(
          "Could not dispatch event: " + event.getClass() + " to handler " + wrapper, e);
    }
  }

  /**
   * Retrieves the currently registered producer for {@code type}.  If no producer is currently registered for
   * {@code type}, this method will return {@code null}.
   *
   * @param type type of producer to retrieve.
   * @return currently registered producer, or {@code null}.
   */
  EventProducer getProducerForEventType(Class<?> type) {
    return producersByType.get(type);
  }

  /**
   * Retrieves a mutable set of the currently registered handlers for {@code type}.  If no handlers are currently
   * registered for {@code type}, this method may either return {@code null} or an empty set.
   *
   * @param type type of handlers to retrieve.
   * @return currently registered handlers, or {@code null}.
   */
  Set<EventHandler> getHandlersForEventType(Class<?> type) {
    return handlersByType.get(type);
  }

  /**
   * Flattens a class's type hierarchy into a set of Class objects.  The set will include all superclasses
   * (transitively), and all interfaces implemented by these superclasses.
   *
   * @param concreteClass class whose type hierarchy will be retrieved.
   * @return {@code concreteClass}'s complete type hierarchy, flattened and uniqued.
   */
  Set<Class<?>> flattenHierarchy(Class<?> concreteClass) {
    Set<Class<?>> classes = flattenHierarchyCache.get(concreteClass);
    if (classes == null) {
      classes = getClassesFor(concreteClass);
      flattenHierarchyCache.put(concreteClass, classes);
    }

    return classes;
  }

  private Set<Class<?>> getClassesFor(Class<?> concreteClass) {
    List<Class<?>> parents = new LinkedList<Class<?>>();
    Set<Class<?>> classes = new HashSet<Class<?>>();

    parents.add(concreteClass);

    while (!parents.isEmpty()) {
      Class<?> clazz = parents.remove(0);
      classes.add(clazz);

      Class<?> parent = clazz.getSuperclass();
      if (parent != null) {
        parents.add(parent);
      }
    }
    return classes;
  }

  /**
   * Throw a {@link RuntimeException} with given message and cause lifted from an {@link
   * InvocationTargetException}. If the specified {@link InvocationTargetException} does not have a
   * cause, neither will the {@link RuntimeException}.
   */
  private static void throwRuntimeException(String msg, InvocationTargetException e) {
    Throwable cause = e.getCause();
    if (cause != null) {
      throw new RuntimeException(msg, cause);
    } else {
      throw new RuntimeException(msg);
    }
  }

  private final Map<Class<?>, Set<Class<?>>> flattenHierarchyCache =
      new HashMap<Class<?>, Set<Class<?>>>();

  /** Simple struct representing an event and its handler. */
  static class EventWithHandler {
    final Object event;
    final EventHandler handler;

    public EventWithHandler(Object event, EventHandler handler) {
      this.event = event;
      this.handler = handler;
    }
  }
}
