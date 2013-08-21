package com.squareup.otto;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class Shuttle implements Bus {

  public static Shuttle createRootBus() {
    return new Shuttle(HandlerFinder.ANNOTATED);
  }

  /** Create a root bus for testing. */
  static Shuttle createTestBus(HandlerFinder handlerFinder) {
    return new Shuttle(handlerFinder);
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

  /** Used to find handler methods in register and unregister. */
  private final HandlerFinder handlerFinder;

  private final Map<Class<?>, Set<EventHandler>> handlersByEventType =
      new HashMap<Class<?>, Set<EventHandler>>();
  private final Set<Shuttle> children = new HashSet<Shuttle>();

  // ArrayDeque is an array-backed queue that grows and shrinks, so it won't create a lot of
  // unnecessary objects for the GC to deal with.
  private final Queue<Object> dispatchEventQueue = new ArrayDeque<Object>();
  private final Queue<EventHandler> dispatchHandlerQueue = new ArrayDeque<EventHandler>();

  private final HierarchyFlattener hierarchyFlattener;

  private boolean dispatching;

  /**
   * Each bus gets its own Handler.
   */
  private final Handler handler = new BusHandler();

  /** null if a root bus. */
  private final Shuttle parent;
  /** this if a root bus. */
  private final Shuttle root;
  private boolean destroyed;

  /** Create a root bus. */
  private Shuttle(HandlerFinder handlerFinder) {
    enforceMainThread();
    this.parent = null;
    this.root = this;
    this.handlerFinder = handlerFinder;
    this.hierarchyFlattener = new HierarchyFlattener();
  }

  /** Create a non-root bus. */
  private Shuttle(Shuttle parent, Shuttle root, HierarchyFlattener hierarchyFlattener) {
    enforceMainThread();
    this.parent = parent;
    this.root = root == null ? this : root;
    this.handlerFinder = this.root.handlerFinder;
    this.hierarchyFlattener = hierarchyFlattener;
    if (parent != null) parent.children.add(this);
  }

  @Override public void register(Object subscriber) {
    enforceMainThread();
    Map<Class<?>, Set<EventHandler>> handlers =
        handlerFinder.findAllSubscribers(subscriber);

    for (Map.Entry<Class<?>, Set<EventHandler>> entry : handlers.entrySet()) {
      Class<?> eventType = entry.getKey();
      Set<EventHandler> registeredHandlers;
      if (!handlersByEventType.containsKey(eventType)) {
        handlersByEventType.put(eventType, entry.getValue());
      } else {
        registeredHandlers = handlersByEventType.get(eventType);
        registeredHandlers.addAll(entry.getValue());
      }
    }
  }

  @Override public void post(Object event) {
    enforceMainThread();
    root.doPost(event);
  }

  private void doPost(Object event) {
    if (destroyed) return;
    Set<Class<?>> dispatchTypes = hierarchyFlattener.flatten(event.getClass());
    boolean dispatched = false;

    for (Class eventType : dispatchTypes) {
      Set<EventHandler> eventHandlers = handlersByEventType.get(eventType);
      if (eventHandlers != null && !eventHandlers.isEmpty()) {
        dispatched = true;
        for (EventHandler handler : eventHandlers) {
          dispatchEventQueue.add(event);
          dispatchHandlerQueue.add(handler);
        }
      }
    }
    if (!dispatched && !(event instanceof DeadEvent)) {
      post(new DeadEvent(this, event));
    }
    dispatchQueuedEvents();
    for (Shuttle child : children) {
      child.doPost(event);
    }
  }

  private void dispatchQueuedEvents() {
    if (dispatching) return;
    try {
      dispatching = true;
      while ((!destroyed) && (!dispatchEventQueue.isEmpty())) {
        EventHandler handler = dispatchHandlerQueue.poll();
        Object event = dispatchEventQueue.poll();
        try {
          handler.handleEvent(event);
        } catch (InvocationTargetException e) {
          Class<?> eventType = event.getClass();
          throwRuntimeException("Could not dispatch event: " + eventType + " to handler " + handler,
              e);
        }
      }
    } finally {
      dispatchHandlerQueue.clear();
      dispatchEventQueue.clear();
      dispatching = false;
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
    return new Shuttle(this, root, hierarchyFlattener);
  }

  private void enforceMainThread() {
    if (!isOnMainThread()) {
      throw new AssertionError("Event bus accessed from non-main thread " + Thread.currentThread());
    }
  }

  private boolean isOnMainThread() {
    return Thread.currentThread() == Looper.getMainLooper().getThread();
  }

  /**
   * Retrieves a mutable set of the currently registered handlers for {@code type}.  If no handlers are currently
   * registered for {@code type}, this method may either return {@code null} or an empty set.
   *
   * @param type type of handlers to retrieve.
   * @return currently registered handlers, or {@code null}.
   */
  Set<EventHandler> getHandlersForEventType(Class<?> type) {
    return handlersByEventType.get(type);
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
}
