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

import com.squareup.otto.internal.Finder;
import com.squareup.otto.internal.Producer;
import com.squareup.otto.internal.Subscriber;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.squareup.otto.internal.AnnotationProcessor.FINDER_SUFFIX;

/**
 * The reference implementation for the Bus interface.  Dispatches events to listeners, and
 * provides ways for listeners to register themselves.
 * <p>
 * BasicBus, by default, enforces that all interactions occur on the main thread.  You can
 * provide an alternate enforcement by passing a {@link ThreadEnforcer} to the constructor.
 * <p>
 * This class is safe for concurrent use.
 *
 * @author Cliff Biffle
 * @author Jake Wharton
 */
public class BasicBus implements Bus {
  private static final String DEFAULT_IDENTIFIER = "default";

  /** Expands the visibility of install and uninstall methods to registered Finders. */
  public class Installer {
    Installer() {
    }

    public <T> void installSubscriber(Class<T> type, Subscriber<T> subscriber) {
      BasicBus.this.installSubscriber(type, subscriber);
    }

    public <T> void installProducer(Class<T> type, Producer<T> producer) {
      BasicBus.this.installProducer(type, producer);
    }

    public <T> void uninstallSubscriber(Class<T> type, Subscriber<T> subscriber) {
      BasicBus.this.uninstallSubscriber(type, subscriber);
    }

    public <T> void uninstallProducer(Class<T> type) {
      BasicBus.this.uninstallProducer(type);
    }
  }

  private final Installer busInstaller = new Installer();

  /** All registered event handlers, indexed by event type. */
  private final ConcurrentMap<Class<?>, Map<Subscriber, Subscriber>> subscribersByType =
      new ConcurrentHashMap<Class<?>, Map<Subscriber, Subscriber>>();

  /** All registered event producers, index by event type. */
  private final ConcurrentMap<Class<?>, Producer> producersByType =
      new ConcurrentHashMap<Class<?>, Producer>();

  /** A cache of flattened class hierarchies. */
  private final Map<Class<?>, Set<Class<?>>> flattenedHierarchyCache =
      new LinkedHashMap<Class<?>, Set<Class<?>>>();

  /** All dynamically loaded event finders, indexed by register type. */
  private final Map<Class<?>, Finder<?>> findersByType =
      new ConcurrentHashMap<Class<?>, Finder<?>>();

  /** Identifier used to differentiate the event bus instance. */
  private final String identifier;

  /** Thread enforcer for register, unregister, and posting events. */
  private final ThreadEnforcer enforcer;

  /**
   * Event finder which uses reflection. Used a fallback when a code generated implementation is
   * not available.
   */
  private final ReflectionFinder fallbackFinder = new ReflectionFinder();

  /** Queues of events for the current thread to dispatch. */
  private final ThreadLocal<Queue<EventWithSubscriber>> eventsToDispatch =
      new ThreadLocal<Queue<EventWithSubscriber>>() {
        @Override protected Queue<EventWithSubscriber> initialValue() {
          return new LinkedList<EventWithSubscriber>();
        }
      };

  /** {@code true} if the current thread is currently dispatching an event. */
  private final ThreadLocal<Boolean> isDispatching = new ThreadLocal<Boolean>() {
    @Override protected Boolean initialValue() {
      return false;
    }
  };

  /** Creates a new BasicBus named "default" that enforces actions on the main thread. */
  public BasicBus() {
    this(DEFAULT_IDENTIFIER);
  }

  /**
   * Creates a new BasicBus with the given {@code identifier} that enforces actions on the main
   * thread.
   *
   * @param identifier a brief name for this bus, for debugging purposes.  Should be a valid Java
   * identifier.
   */
  public BasicBus(String identifier) {
    this(ThreadEnforcer.MAIN, identifier);
  }

  /**
   * Creates a new BasicBus named "default" with the given {@code enforcer} for actions.
   *
   * @param enforcer Thread enforcer for register, unregister, and post actions.
   */
  public BasicBus(ThreadEnforcer enforcer) {
    this(enforcer, DEFAULT_IDENTIFIER);
  }

  /**
   * Creates a new BasicBus with the given {@code enforcer} for actions and the given {@code
   * identifier}.
   *
   * @param enforcer Thread enforcer for register, unregister, and post actions.
   * @param identifier A brief name for this bus, for debugging purposes.  Should be a valid Java
   * identifier.
   */
  public BasicBus(ThreadEnforcer enforcer, String identifier) {
    if (enforcer == null) {
      throw new NullPointerException("Enforcer may not be null.");
    }
    if (identifier == null) {
      throw new NullPointerException("Identifier may not be null.");
    }
    this.enforcer = enforcer;
    this.identifier = identifier;
  }

  @Override public String toString() {
    return "[BasicBus \"" + identifier + "\"]";
  }

  Finder obtainFinder(Class<?> type) {
    Finder finder = findersByType.get(type);
    if (finder == null) {
      Class<?> typeFinder = null;
      Class<?> typeToLoad = type;
      while (typeFinder == null && typeToLoad != Object.class) {
        try {
          typeFinder = Class.forName(typeToLoad.getName() + FINDER_SUFFIX);
        } catch (ClassNotFoundException ignored) {
          typeToLoad = typeToLoad.getSuperclass();
        }
      }
      if (typeFinder != null) {
        try {
          finder = (Finder) typeFinder.newInstance();
          findersByType.put(type, finder);
        } catch (InstantiationException e) {
          throw new RuntimeException("Unable to instantiate generated finder instance.", e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException("Unable to instantiate generated finder instance.", e);
        }
      } else {
        finder = fallbackFinder;
      }
    }
    return finder;
  }

  @Override public void register(Object object) {
    enforcer.enforce(this);
    obtainFinder(object.getClass()).install(object, busInstaller);
  }

  private void dispatchProducerResultToSubscriber(Subscriber subscriber, Producer producer) {
    Object event = null;
    try {
      event = producer.produce();
    } catch (InvocationTargetException e) {
      throwRuntimeException("Producer " + producer + " threw an exception.", e);
    }
    if (event == null) {
      return;
    }
    dispatch(event, subscriber);
  }

  @Override public void unregister(Object object) {
    enforcer.enforce(this);
    obtainFinder(object.getClass()).uninstall(object, busInstaller);
  }

  <T> void installSubscriber(Class<T> type, Subscriber<T> subscriber) {
    Map<Subscriber, Subscriber> subscribers = subscribersByType.get(type);
    if (subscribers == null) {
      Map<Subscriber, Subscriber> newSubscribers = new ConcurrentHashMap<Subscriber, Subscriber>();
      subscribers = subscribersByType.putIfAbsent(type, newSubscribers);
      if (subscribers == null) {
        subscribers = newSubscribers;
      }
    }
    subscribers.put(subscriber, subscriber);

    Producer producer = producersByType.get(type);
    if (producer != null) {
      dispatchProducerResultToSubscriber(subscriber, producer);
    }
  }

  <T> void installProducer(Class<T> type, Producer<T> producer) {
    if (producersByType.putIfAbsent(type, producer) != null) {
      throw new IllegalArgumentException(
          "Producer method for type " + type + " already registered.");
    }
    // Trigger producer for each subscriber already registered to its type.
    Map<Subscriber, Subscriber> subscribers = subscribersByType.get(type);
    if (subscribers != null && !subscribers.isEmpty()) {
      for (Subscriber subscriber : subscribers.keySet()) {
        dispatchProducerResultToSubscriber(subscriber, producer);
      }
    }
  }

  <T> void uninstallSubscriber(Class<T> type, Subscriber<T> subscriber) {
    Map<Subscriber, Subscriber> subscribers = subscribersByType.get(type);
    if (subscribers != null) {
      Subscriber original = subscribers.remove(subscriber);
      if (original != null) {
        original.invalidate();
      }
      return;
    }
    throw new IllegalArgumentException(
        "Missing subscriber for an annotated method. Is " + type + " registered?");
  }

  <T> void uninstallProducer(Class<T> type) {
    Producer producer = producersByType.remove(type);
    if (producer == null) {
      throw new IllegalArgumentException(
          "Missing producer for an annotated method. Is " + type + " registered?");
    }
    producer.invalidate();
  }

  @Override public void post(Object event) {
    enforcer.enforce(this);

    Set<Class<?>> dispatchTypes = flattenHierarchy(event.getClass());

    boolean dispatched = false;
    for (Class<?> eventType : dispatchTypes) {
      Set<Subscriber> wrappers = getSubscribersForEventType(eventType);

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
   * Queue the {@code event} for dispatch during {@link #dispatchQueuedEvents()}. Events are queued
   * in-order of occurrence so they can be dispatched in the same order.
   */
  protected void enqueueEvent(Object event, Subscriber subscriber) {
    eventsToDispatch.get().offer(new EventWithSubscriber(event, subscriber));
  }

  /**
   * Drain the queue of events to be dispatched. As the queue is being drained, new events may be
   * posted to the end of the queue.
   */
  protected void dispatchQueuedEvents() {
    // Don't dispatch if we're already dispatching, that would allow reentrancy and out-of-order
    // events. Instead, leave the events to be dispatched after in-progress dispatch is complete.
    if (isDispatching.get()) {
      return;
    }

    isDispatching.set(true);
    try {
      while (true) {
        EventWithSubscriber eventWithSubscriber = eventsToDispatch.get().poll();
        if (eventWithSubscriber == null) {
          break;
        }

        dispatch(eventWithSubscriber.event, eventWithSubscriber.subscriber);
      }
    } finally {
      isDispatching.set(false);
    }
  }

  /**
   * Dispatches {@code event} to the subscriber in {@code subscriber}.  This method is an
   * appropriate
   * override point for subclasses that wish to make event delivery asynchronous.
   *
   * @param event event to dispatch.
   * @param subscriber subscriber that will call the method.
   */
  protected void dispatch(Object event, Subscriber subscriber) {
    try {
      subscriber.handle(event);
    } catch (InvocationTargetException e) {
      throwRuntimeException(
          String.format("Could not dispatch event %s to subscriber %s.", event.getClass(),
              subscriber), e);
    }
  }

  /**
   * Retrieves a mutable set of the currently registered subscribers for {@code type}.  If no
   * subscribers are currently registered for {@code type}, this method may either return
   * {@code null} or an empty set.
   *
   * @param type type of handlers to retrieve.
   * @return currently registered handlers, or {@code null}.
   */
  Set<Subscriber> getSubscribersForEventType(Class<?> type) {
    Map<Subscriber, Subscriber> subscribers = subscribersByType.get(type);
    return subscribers != null ? subscribers.keySet() : null;
  }

  /**
   * Flattens a class's type hierarchy into a set of Class objects.  The set will include all
   * superclasses (transitively), and all interfaces implemented by these superclasses.
   *
   * @param concreteClass class whose type hierarchy will be retrieved.
   * @return {@code clazz}'s complete type hierarchy, flattened and uniqued.
   */
  Set<Class<?>> flattenHierarchy(Class<?> concreteClass) {
    Set<Class<?>> classes = flattenedHierarchyCache.get(concreteClass);
    if (classes == null) {
      classes = getClassesFor(concreteClass);
      flattenedHierarchyCache.put(concreteClass, classes);
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

  /** Simple struct representing an event and its subscriber. */
  static class EventWithSubscriber {
    final Object event;
    final Subscriber subscriber;

    public EventWithSubscriber(Object event, Subscriber subscriber) {
      this.event = event;
      this.subscriber = subscriber;
    }
  }
}
