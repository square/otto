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

package com.squareup.eventbus;

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
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.squareup.eventbus.AnnotatedHandlerFinder.findAllSubscribers;
import static com.squareup.eventbus.AnnotatedHandlerFinder.findAllProducers;

/**
 * Dispatches events to listeners, and provides ways for listeners to register
 * themselves.
 *
 * <p>The EventBus allows publish-subscribe-style communication between
 * components without requiring the components to explicitly register with one
 * another (and thus be aware of each other).  It is designed exclusively to
 * replace traditional Java in-process event distribution using explicit
 * registration. It is <em>not</em> a general-purpose publish-subscribe system,
 * nor is it intended for interprocess communication.
 *
 * <h2>Receiving Events</h2>
 * To receive events, an object should:<ol>
 * <li>Expose a public method, known as the <i>event handler</i>, which accepts
 * a single argument of the type of event desired;</li>
 * <li>Mark it with a {@link com.squareup.eventbus.Subscribe} annotation;</li>
 * <li>Pass itself to an EventBus instance's {@link #register(Object)} method.
 * </li>
 * </ol>
 *
 * <h2>Posting Events</h2>
 * To post an event, simply provide the event object to the
 * {@link #post(Object)} method.  The EventBus instance will determine the type
 * of event and route it to all registered listeners.
 *
 * <p>Events are routed based on their type &mdash; an event will be delivered
 * to any handler for any type to which the event is <em>assignable.</em>  This
 * includes implemented interfaces, all superclasses, and all interfaces
 * implemented by superclasses.
 *
 * <p>When {@code post} is called, all registered handlers for an event are run
 * in sequence, so handlers should be reasonably quick.  If an event may trigger
 * an extended process (such as a database load), spawn a thread or queue it for
 * later.  (For a convenient way to do this, use an {@link com.squareup.eventbus.AsyncEventBus}.)
 *
 * <h2>Handler Methods</h2>
 * Event handler methods must accept only one argument: the event.
 *
 * <p>Handlers should not, in general, throw.  If they do, the EventBus will
 * catch and log the exception.  This is rarely the right solution for error
 * handling and should not be relied upon; it is intended solely to help find
 * problems during development.
 *
 * <p>The EventBus guarantees that it will not call a handler method from
 * multiple threads simultaneously, unless the method explicitly allows it by
 * bearing the {@link com.squareup.eventbus.AllowConcurrentEvents} annotation.  If this
 * annotation is
 * not present, handler methods need not worry about being reentrant, unless
 * also called from outside the EventBus.
 *
 * <h2>Dead Events</h2>
 * If an event is posted, but no registered handlers can accept it, it is
 * considered "dead."  To give the system a second chance to handle dead events,
 * they are wrapped in an instance of {@link com.squareup.eventbus.DeadEvent} and reposted.
 *
 * <p>If a handler for a supertype of all events (such as Object) is registered,
 * no event will ever be considered dead, and no DeadEvents will be generated.
 * Accordingly, while DeadEvent extends {@link Object}, a handler registered to
 * receive any Object will never receive a DeadEvent.
 *
 * <p>This class is safe for concurrent use.
 *
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/EventBusExplained">
 * {@code EventBus}</a>.
 *
 * @author Cliff Biffle
 * @since 10.0
 */
public class EventBus {

  /** All registered event handlers, indexed by event type. */
  private final Map<Class<?>, Set<EventHandler>> handlersByType = new ConcurrentHashMap<Class<?>, Set<EventHandler>>();

  /** All registered event producers, index by event type. */
  private final Map<Class<?>, EventProducer> producersByType = new ConcurrentHashMap<Class<?>, EventProducer>();

  /**
   * Logger for event dispatch failures.  Named by the fully-qualified name of
   * this class, followed by the identifier provided at construction.
   */
  private final Logger logger;

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

  /** Creates a new EventBus named "default". */
  public EventBus() {
    this("default");
  }

  /**
   * Creates a new EventBus with the given {@code identifier}.
   *
   * @param identifier a brief name for this bus, for logging purposes.  Should
   * be a valid Java identifier.
   */
  public EventBus(String identifier) {
    logger = Logger.getLogger(EventBus.class.getName() + "." + identifier);
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
   */
  public void register(Object object) {
    Map<Class<?>, EventProducer> foundProducers = findAllProducers(object);
    for (Class<?> type : foundProducers.keySet()) {
      if (producersByType.containsKey(type)) {
        throw new IllegalArgumentException("Producer method for type " + type + " already registered.");
      }
      final EventProducer producer = foundProducers.get(type);
      producersByType.put(type, producer);

      Set<EventHandler> handlers = handlersByType.get(type);
      if (handlers != null && !handlers.isEmpty()) {
        for (EventHandler handler : handlers) {
          dispatchProducerResultToHandler(handler, producer);
        }
      }
    }

    Map<Class<?>, Set<EventHandler>> foundHandlersMap = findAllSubscribers(object);
    for (Class<?> type : foundHandlersMap.keySet()) {
      Set<EventHandler> handlers = handlersByType.get(type);
      if (handlers == null) {
        handlers = new CopyOnWriteArraySet<EventHandler>();
        handlersByType.put(type, handlers);
      }
      final Set<EventHandler> foundHandlers = foundHandlersMap.get(type);
      handlers.addAll(foundHandlers);

      EventProducer producer = producersByType.get(type);
      if (producer != null) {
        for (EventHandler foundHandler : foundHandlers) {
          dispatchProducerResultToHandler(foundHandler, producer);
        }
      }
    }
  }

  private void dispatchProducerResultToHandler(EventHandler handler, EventProducer producer) {
    try {
      handler.handleEvent(producer.produceEvent());
    } catch (InvocationTargetException e) {
      logger.log(Level.SEVERE, "Could not dispatch event from " + producer + " to handler " + handler, e);
    }
  }

  /**
   * Unregisters all producer and handler methods on a registered {@code object}.
   *
   * @param object object whose producer and handler methods should be unregistered.
   * @throws IllegalArgumentException if the object was not previously registered.
   */
  public void unregister(Object object) {
    Map<Class<?>, EventProducer> producersInListener = findAllProducers(object);
    for (Map.Entry<Class<?>, EventProducer> entry : producersInListener.entrySet()) {
      final Class<?> key = entry.getKey();
      EventProducer producer = getProducerForEventType(key);
      EventProducer value = entry.getValue();

      if (value == null || value != producer) {
        throw new IllegalArgumentException(
            "Missing event producer for an annotated method. Is " + object + " registered?");
      }
      producersInListener.remove(key);
    }

    Map<Class<?>, Set<EventHandler>> handlersInListener = findAllSubscribers(object);
    for (Map.Entry<Class<?>, Set<EventHandler>> entry : handlersInListener.entrySet()) {
      Set<EventHandler> currentHandlers = getHandlersForEventType(entry.getKey());
      Collection<EventHandler> eventMethodsInListener = entry.getValue();

      if (currentHandlers == null || !currentHandlers.containsAll(entry.getValue())) {
        throw new IllegalArgumentException(
            "Missing event handler for an annotated method. Is " + object + " registered?");
      }
      currentHandlers.removeAll(eventMethodsInListener);
    }
  }

  /**
   * Posts an event to all registered handlers.  This method will return
   * successfully after the event has been posted to all handlers, and
   * regardless of any exceptions thrown by handlers.
   *
   * <p>If no handlers have been subscribed for {@code event}'s class, and
   * {@code event} is not already a {@link com.squareup.eventbus.DeadEvent}, it will be
   * wrapped
   * in a
   * DeadEvent and reposted.
   *
   * @param event event to post.
   */
  public void post(Object event) {
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
   * Queue the {@code event} for dispatch during
   * {@link #dispatchQueuedEvents()}. Events are queued in-order of occurrence
   * so they can be dispatched in the same order.
   */
  protected void enqueueEvent(Object event, EventHandler handler) {
    eventsToDispatch.get().offer(new EventWithHandler(event, handler));
  }

  /**
   * Drain the queue of events to be dispatched. As the queue is being drained,
   * new events may be posted to the end of the queue.
   */
  protected void dispatchQueuedEvents() {
    // don't dispatch if we're already dispatching, that would allow reentrancy
    // and out-of-order events. Instead, leave the events to be dispatched
    // after the in-progress dispatch is complete.
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

        dispatch(eventWithHandler.event, eventWithHandler.handler);
      }
    } finally {
      isDispatching.set(false);
    }
  }

  /**
   * Dispatches {@code event} to the handler in {@code wrapper}.  This method
   * is an appropriate override point for subclasses that wish to make
   * event delivery asynchronous.
   *
   * @param event event to dispatch.
   * @param wrapper wrapper that will call the handler.
   */
  protected void dispatch(Object event, EventHandler wrapper) {
    try {
      wrapper.handleEvent(event);
    } catch (InvocationTargetException e) {
      logger.log(Level.SEVERE, "Could not dispatch event: " + event + " to handler " + wrapper, e);
    }
  }

  /**
   * Retrieves a mutable set of the currently registered producers for
   * {@code type}.  If no producers are currently registered for {@code type},
   * this method will return {@code null}.
   *
   * @param type type of producers to retrieve.
   * @return currently registered producer, or {@code null}.
   */
  EventProducer getProducerForEventType(Class<?> type) {
    return producersByType.get(type);
  }

  /**
   * Retrieves a mutable set of the currently registered handlers for
   * {@code type}.  If no handlers are currently registered for {@code type},
   * this method may either return {@code null} or an empty set.
   *
   * @param type type of handlers to retrieve.
   * @return currently registered handlers, or {@code null}.
   */
  Set<EventHandler> getHandlersForEventType(Class<?> type) {
    return handlersByType.get(type);
  }

  /**
   * Flattens a class's type hierarchy into a set of Class objects.  The set
   * will include all superclasses (transitively), and all interfaces
   * implemented by these superclasses.
   *
   * @param concreteClass class whose type hierarchy will be retrieved.
   * @return {@code clazz}'s complete type hierarchy, flattened and uniqued.
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

  private final Map<Class<?>, Set<Class<?>>> flattenHierarchyCache =
      new HashMap<Class<?>, Set<Class<?>>>();

  /** Simple struct representing an event and it's handler. */
  static class EventWithHandler {
    final Object event;
    final EventHandler handler;

    public EventWithHandler(Object event, EventHandler handler) {
      this.event = event;
      this.handler = handler;
    }
  }
}
