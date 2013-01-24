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

import static com.squareup.otto.internal.AnnotationProcessor.FINDER_SUFFIX;
import static com.squareup.otto.internal.AnnotationProcessor.PACKAGE_PREFIX;

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
 * <li>Expose a public method, known as the <i>event subscriber</i>, which accepts a single argument of the type of
 * event desired;</li>
 * <li>Mark it with a {@link com.squareup.otto.Subscribe} annotation;</li>
 * <li>Pass itself to an Bus instance's {@link #register(Object)} method.
 * </li>
 * </ol>
 *
 * <h2>Posting Events</h2>
 * To post an event, simply provide the event object to the {@link #post(Object)} method.  The Bus instance will
 * determine the type of event and route it to all registered listeners.
 *
 * <p>Events are routed based on their type &mdash; an event will be delivered to any subscriber for any type to which
 * the event is <em>assignable.</em>  This includes implemented interfaces, all superclasses, and all interfaces
 * implemented by superclasses.
 *
 * <p>When {@code post} is called, all registered handlers for an event are run in sequence, so handlers should be
 * reasonably quick.  If an event may trigger an extended process (such as a database load), spawn a thread or queue it
 * for later.
 *
 * <h2>Subscriber Methods</h2>
 * Event subscriber methods must accept only one argument: the event.
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
  private final Map<Class<?>, Set<Subscriber>> handlersByType = new ConcurrentHashMap<Class<?>, Set<Subscriber>>();

  /** All registered event producers, index by event type. */
  private final Map<Class<?>, Producer> producersByType = new ConcurrentHashMap<Class<?>, Producer>();

  /** All dynamically loaded event finders, indexed by register type. */
  private final Map<Class<?>, Finder<?>> findersByType = new ConcurrentHashMap<Class<?>, Finder<?>>();

  /** Identifier used to differentiate the event bus instance. */
  private final String identifier;

  /** Thread enforcer for register, unregister, and posting events. */
  private final ThreadEnforcer enforcer;

  /** Event finder which uses reflection. Used a fallback when a code generated implementation is not available. */
  private final ReflectionFinder fallbackFinder = new ReflectionFinder();

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
    this.enforcer = enforcer;
    this.identifier = identifier;
  }

  @Override public String toString() {
    return "[Bus \"" + identifier + "\"]";
  }

  Finder obtainFinder(Class<?> type) {
    Finder finder = findersByType.get(type);
    if (finder == null) {
      Class<?> typeFinder = null;
      Class<?> typeToLoad = type;
      while (typeFinder == null && typeToLoad != Object.class) {
        try {
          typeFinder = Class.forName(PACKAGE_PREFIX + typeToLoad.getSimpleName() + FINDER_SUFFIX);
        } catch (ClassNotFoundException ignored) {
          typeToLoad = typeToLoad.getSuperclass();
        }
      }
      if (typeFinder != null) {
        try {
          finder = (Finder) typeFinder.newInstance();
          findersByType.put(type, finder);
          return finder;
        } catch (InstantiationException e) {
          throw new RuntimeException("Unable to instantiate generated finder instance.", e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException("Unable to instantiate generated finder instance.", e);
        }
      }
    }
    return fallbackFinder;
  }

  /**
   * Registers all subscriber methods on {@code object} to receive events and producer methods to provide events.
   * <p>
   * If any subscribers are registering for types which already have a producer they will be called immediately
   * with the result of calling that producer.
   * <p>
   * If any producers are registering for types which already have subscribers, each subscriber will be called with
   * the value from the result of calling the producer.
   *
   * @param object object whose subscriber methods should be registered.
   */
  public void register(Object object) {
    enforcer.enforce(this);
    obtainFinder(object.getClass()).install(object, this);
  }

  private void dispatchProducerResultToHandler(Subscriber subscriber, Producer producer) {
    Object event;
    try {
      event = producer.produce();
    } catch (InvocationTargetException e) {
      throw new RuntimeException("Producer " + producer + " threw an exception.", e);
    }
    if (event == null) {
      return;
    }
    try {
      subscriber.handle(event);
    } catch (InvocationTargetException e) {
      String type = event.getClass().toString();
      throw new RuntimeException(
          "Could not dispatch event " + type + " from " + producer + " to subscriber " + subscriber, e);
    }
  }

  /**
   * Unregisters all producer and subscriber methods on a registered {@code object}.
   *
   * @param object object whose producer and subscriber methods should be unregistered.
   * @throws IllegalArgumentException if the object was not previously registered.
   */
  public void unregister(Object object) {
    enforcer.enforce(this);
    obtainFinder(object.getClass()).uninstall(object, this);
  }

  <T> void installSubscriber(Class<T> type, Subscriber<T> subscriber) {
    Set<Subscriber> subscribers = handlersByType.get(type);
    if (subscribers == null) {
      subscribers = new HashSet<Subscriber>();
      handlersByType.put(type, subscribers);
    }
    subscribers.add(subscriber);

    Producer producer = producersByType.get(type);
    if (producer != null) {
      dispatchProducerResultToHandler(subscriber, producer);
    }
  }

  <T> void installProducer(Class<T> type, Producer<T> producer) {
    if (producersByType.containsKey(type)) {
      throw new IllegalArgumentException("Producer method for type " + type + " already registered.");
    }
    producersByType.put(type, producer);

    // Trigger producer for each subscriber already registered to its type.
    Set<Subscriber> subscribers = handlersByType.get(type);
    if (subscribers != null && !subscribers.isEmpty()) {
      for (Subscriber subscriber : subscribers) {
        dispatchProducerResultToHandler(subscriber, producer);
      }
    }
  }

  <T> void uninstallSubscriber(Class<T> type, Subscriber<T> subscriber) {
    Set<Subscriber> subscribers = handlersByType.get(type);
    if (subscribers == null || !subscribers.remove(subscriber)) {
      throw new IllegalArgumentException("Missing subscriber for an annotated method. Is "
              + subscriber + " registered?");
    }
  }

  <T> void uninstallProducer(Class<T> type) {
    if (producersByType.remove(type) == null) {
      throw new IllegalArgumentException("Missing producer for an annotated method. Is " + type + " registered?");
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
   */
  public void post(Object event) {
    enforcer.enforce(this);

    Set<Class<?>> dispatchTypes = flattenHierarchy(event.getClass());

    boolean dispatched = false;
    for (Class<?> eventType : dispatchTypes) {
      Collection<Subscriber> wrappers = getHandlersForEventType(eventType);

      if (wrappers != null && !wrappers.isEmpty()) {
        dispatched = true;
        for (Subscriber wrapper : wrappers) {
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
  protected void enqueueEvent(Object event, Subscriber subscriber) {
    eventsToDispatch.get().offer(new EventWithHandler(event, subscriber));
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

        dispatch(eventWithHandler.event, eventWithHandler.subscriber);
      }
    } finally {
      isDispatching.set(false);
    }
  }

  /**
   * Dispatches {@code event} to the subscriber in {@code wrapper}.  This method is an appropriate override point for
   * subclasses that wish to make event delivery asynchronous.
   *
   * @param event event to dispatch.
   * @param wrapper wrapper that will call the subscriber.
   */
  protected void dispatch(Object event, Subscriber wrapper) {
    try {
      wrapper.handle(event);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(
          "Could not dispatch event: " + event.getClass() + " to subscriber " + wrapper, e);
    }
  }

  /**
   * Retrieves a mutable set of the currently registered producers for {@code type}.  If no producers are currently
   * registered for {@code type}, this method will return {@code null}.
   *
   * @param type type of producers to retrieve.
   * @return currently registered producer, or {@code null}.
   */
  Producer getProducerForEventType(Class<?> type) {
    return producersByType.get(type);
  }

  /**
   * Retrieves a mutable set of the currently registered handlers for {@code type}.  If no handlers are currently
   * registered for {@code type}, this method may either return {@code null} or an empty set.
   *
   * @param type type of handlers to retrieve.
   * @return currently registered handlers, or {@code null}.
   */
  Set<Subscriber> getHandlersForEventType(Class<?> type) {
    return handlersByType.get(type);
  }

  /**
   * Flattens a class's type hierarchy into a set of Class objects.  The set will include all superclasses
   * (transitively), and all interfaces implemented by these superclasses.
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

  /** Simple struct representing an event and its subscriber. */
  static class EventWithHandler {
    final Object event;
    final Subscriber subscriber;

    public EventWithHandler(Object event, Subscriber subscriber) {
      this.event = event;
      this.subscriber = subscriber;
    }
  }
}
