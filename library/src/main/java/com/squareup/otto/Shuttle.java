package com.squareup.otto;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Shuttle implements Bus {

  public static Shuttle createRootBus() {
    return new Shuttle();
  }

  private final Map<Class<?>, Set<EventHandler>> handlersByEventType =
      new HashMap<Class<?>, Set<EventHandler>>();
  private final ShuttleDispatcher dispatcher;
  private final Set<Shuttle> children = new HashSet<Shuttle>();
  /** null if a root bus. */
  private final Shuttle parent;
  /** Posting is disabled by default. */
  private boolean postingEnabled;
  private boolean destroyed;

  private Shuttle() {
    this(ShuttleDispatcher.main());
  }

  /** Exposed for tests.  Don't touch. */
  public Shuttle(ShuttleDispatcher dispatcher) {
    this(null, dispatcher);
  }

  private Shuttle(Shuttle parent, ShuttleDispatcher dispatcher) {
    this.parent = parent;
    this.dispatcher = dispatcher;
    if (parent != null) parent.children.add(this);
  }

  /**
   * Throw a {@link RuntimeException} with given message and cause lifted from an {@link
   * java.lang.reflect.InvocationTargetException}. If the specified {@link
   * java.lang.reflect.InvocationTargetException} does not have a
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

  @Override public void register(Object subscriber) {
    dispatcher.enforce();
    Map<Class<?>, Set<EventHandler>> handlers =
        AnnotatedHandlerFinder.findAllSubscribers(subscriber);

    for (Map.Entry<Class<?>, Set<EventHandler>> entry : handlers.entrySet()) {
      Class<?> eventType = entry.getKey();
      Set<EventHandler> registeredHandlers;
      if (!handlersByEventType.containsKey(eventType)) {
        registeredHandlers = new HashSet<EventHandler>();
        handlersByEventType.put(eventType, registeredHandlers);
      } else {
        registeredHandlers = handlersByEventType.get(eventType);
      }
      registeredHandlers.addAll(entry.getValue());
    }
  }

  @Override public void post(Object event) {
    dispatcher.enforce();
    doPost(event);
  }

  private void doPost(Object event) {
    if (!postingEnabled) {
      return;
    }
    Class<?> eventType = event.getClass();
    Set<EventHandler> eventHandlers = handlersByEventType.get(eventType);
    if (eventHandlers != null) {
      for (EventHandler handler : eventHandlers) {
        try {
          handler.handleEvent(event);
        } catch (InvocationTargetException e) {
          throwRuntimeException("Could not dispatch event: " + eventType + " to handler " + handler,
              e);
        }
      }
    }
    for (Shuttle child : children) {
      child.doPost(event);
    }
  }

  @Override public void enable() {
    dispatcher.enforce();
    if (postingEnabled) throw new IllegalStateException("Bus is already enabled.");
    if (destroyed) throw new IllegalStateException("Bus has been destroyed.");
    postingEnabled = true;
    for (Bus child : children) {
      child.enable();
    }
  }

  @Override public void disable() {
    dispatcher.enforce();
    postingEnabled = false;
    for (Bus child : children) {
      child.disable();
    }
  }

  @Override public void postOnBusThread(Object event) {
    throw new UnsupportedOperationException("NOT IMPLEMENTED");
  }

  @Override public void destroy() {
    dispatcher.enforce();
    if (destroyed) throw new IllegalStateException("Bus has already been destroyed.");
    if (parent != null) parent.children.remove(this);
    destroyRecursively();
  }

  private void destroyRecursively() {
    for (Shuttle child : children) {
      child.destroyRecursively();
    }
    children.clear();
    postingEnabled = false;
    destroyed = true;
  }

  @Override public Bus spawn() {
    dispatcher.enforce();
    return new Shuttle(this, dispatcher);
  }
}
