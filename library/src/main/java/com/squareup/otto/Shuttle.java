package com.squareup.otto;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

public final class Shuttle implements Bus {

  public static Shuttle createRootBus() {
    return new Shuttle();
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

  class BusHandler extends Handler {
    @Override public void handleMessage(Message message) {
      Object event = message.obj;
      Shuttle.this.post(event);
    }
  }

  private final Map<Class<?>, Set<EventHandler>> handlersByEventType =
      new HashMap<Class<?>, Set<EventHandler>>();
  private final Set<Shuttle> children = new HashSet<Shuttle>();

  /**
   * Each bus gets its own Handler.
   */
  private final Handler handler = new BusHandler();

  /** null if a root bus. */
  private final Shuttle parent;
  private boolean destroyed;

  private Shuttle() {
    this(null);
  }

  private Shuttle(Shuttle parent) {
    enforceMainThread();
    this.parent = parent;
    if (parent != null) parent.children.add(this);
  }

  @Override public void register(Object subscriber) {
    enforceMainThread();
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
    enforceMainThread();
    doPost(event);
  }

  private void doPost(Object event) {
    if (destroyed) return;
    Class<?> eventType = event.getClass();
    Set<EventHandler> eventHandlers = handlersByEventType.get(eventType);
    if (eventHandlers != null) {
      for (EventHandler handler : eventHandlers) {
        try {
          handler.handleEvent(event);
        } catch (InvocationTargetException e) {
          String msg = format("Could not dispatch event %s to handler %s", eventType, handler);
          throwRuntimeException(msg, e);
        }
      }
    }
    for (Shuttle child : children) {
      child.doPost(event);
    }
  }

  @Override public void postOnMainThread(final Object event) {
    if (isOnMainThread()) {
      post(event);
    } else {
      Message message = handler.obtainMessage();
      message.obj = event;
      handler.sendMessage(message);
    }
  }

  @Override public void destroy() {
    enforceMainThread();
    if (destroyed) throw new IllegalStateException("Bus has already been destroyed.");
    if (parent != null) parent.children.remove(this);
    destroyRecursively();
  }

  private void destroyRecursively() {
    for (Shuttle child : children) {
      child.destroyRecursively();
    }
    children.clear();
    destroyed = true;
  }

  @Override public Bus spawn() {
    // Main thread enforcement is handled by the constructor.
    return new Shuttle(this);
  }

  private void enforceMainThread() {
    if (!isOnMainThread()) {
      throw new AssertionError("Event bus accessed from non-main thread " + Thread.currentThread());
    }
  }

  private boolean isOnMainThread() {
    return Thread.currentThread() == Looper.getMainLooper().getThread();
  }
}
