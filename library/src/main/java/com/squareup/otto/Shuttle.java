package com.squareup.otto;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Shuttle implements Bus {

  // Phase 1

  private final Map<Class<?>, Set<EventHandler>> handlersByEventType =
      new HashMap<Class<?>, Set<EventHandler>>();

  /**
   * Posting is disabled by default.
   */
  private boolean postingEnabled;

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

    if (!postingEnabled) {
      return;
    }

    Class<?> eventType = event.getClass();
    Set<EventHandler> eventHandlers = handlersByEventType.get(eventType);
    if (eventHandlers == null) return;
    for (EventHandler handler : eventHandlers) {
      try {
        handler.handleEvent(event);
      } catch (InvocationTargetException e) {
        throwRuntimeException("Could not dispatch event: " + eventType + " to handler " + handler,
            e);
      }
    }
  }

  @Override public void enable() {
    postingEnabled = true;
  }

  // Phase 3  - beer

  @Override public void disable() {
    postingEnabled = false;
  }

  // Phase 4 - whiskey

  @Override public void postOnBusThread(Object event) {
    throw new UnsupportedOperationException("NOT IMPLEMENTED");
  }

  // Phase 5

  @Override public void destroy() {
    throw new UnsupportedOperationException("NOT IMPLEMENTED");
  }

  @Override public Bus spawn() {
    throw new UnsupportedOperationException("NOT IMPLEMENTED");
  }
}
