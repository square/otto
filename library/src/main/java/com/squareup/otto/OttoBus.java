package com.squareup.otto;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class OttoBus implements Bus {

  /** Used to find handler methods in register and unregister. */
  private final HandlerFinder handlerFinder;
  private final DeadEventHandler deadEventHandler;
  private final Map<Class<?>, Set<EventHandler>> handlersByEventType =
      new HashMap<Class<?>, Set<EventHandler>>();
  private final Set<OttoBus> children = new HashSet<OttoBus>();
  // ArrayDeque is an array-backed queue that grows and shrinks, so it won't create a lot of
  // unnecessary objects for the GC to deal with.
  private final Queue<Object> dispatchEventQueue = new ArrayDeque<Object>();
  private final Queue<EventHandler> dispatchHandlerQueue = new ArrayDeque<EventHandler>();
  private final HierarchyFlattener hierarchyFlattener;
  /** null if a root bus. */
  private final OttoBus parent;
  /** this if a root bus. */
  private final OttoBus root;
  private MainThread mainThread;
  private boolean dispatching;
  private boolean destroyed;
  /** Create a root bus that ignores dead events. */
  public OttoBus() {
    this(HandlerFinder.ANNOTATED, DeadEventHandler.IGNORE_DEAD_EVENTS);
  }

  /** Create a root bus. */
  public OttoBus(DeadEventHandler deadEventHandler) {
    this(HandlerFinder.ANNOTATED, deadEventHandler);
  }

  OttoBus(HandlerFinder handlerFinder, DeadEventHandler deadEventHandler) {
    this(null, handlerFinder, deadEventHandler);
  }

  /** Create a root bus. */
  OttoBus(MainThread mainThread, HandlerFinder handlerFinder, DeadEventHandler deadEventHandler) {
    this.mainThread = mainThread == null ? new AndroidMainThread() : mainThread;
    this.mainThread.enforce();
    this.parent = null;
    this.root = this;
    this.handlerFinder = handlerFinder;
    this.deadEventHandler = deadEventHandler;
    this.hierarchyFlattener = new HierarchyFlattener();
  }

  /** Create a non-root bus. */
  private OttoBus(OttoBus parent, OttoBus root) {
    root.mainThread.enforce();
    this.parent = parent;
    this.root = root;
    this.mainThread = root.mainThread;
    this.handlerFinder = root.handlerFinder;
    this.deadEventHandler = root.deadEventHandler;
    this.hierarchyFlattener = root.hierarchyFlattener;
    parent.children.add(this);
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
    mainThread.enforce();

    Map<Class<?>, Set<EventHandler>> handlers =
        handlerFinder.findAllSubscribers(subscriber);

    for (Map.Entry<Class<?>, Set<EventHandler>> entry : handlers.entrySet()) {
      Class<?> eventType = entry.getKey();

      Set<EventHandler> eventHandlers = handlersByEventType.get(eventType);
      if (eventHandlers == null) {
        handlersByEventType.put(eventType, entry.getValue());
      } else {
        eventHandlers.addAll(entry.getValue());
      }
    }
  }

  @Override public void post(Object event) {
    mainThread.enforce();

    boolean dispatched = root.doPost(event);
    if (!dispatched) deadEventHandler.onDeadEvent(event);
  }

  /**
   * @return true iff event was dispatched to some subscriber.
   */
  private boolean doPost(Object event) {
    boolean dispatched = false;
    if (destroyed) return false;
    Set<Class<?>> dispatchTypes = hierarchyFlattener.flatten(event.getClass());

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
    dispatchQueuedEvents();
    for (OttoBus child : children) {
      dispatched |= child.doPost(event);
    }
    return dispatched;
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
    mainThread.post(event);
  }

  @Override public void destroy() {
    mainThread.enforce();

    if (destroyed) throw new IllegalStateException("Bus has already been destroyed.");
    if (parent != null) parent.children.remove(this);
    destroyRecursively();
  }

  private void destroyRecursively() {
    for (OttoBus child : children) {
      child.destroyRecursively();
    }
    children.clear();
    destroyed = true;
  }

  @Override public Bus spawn() {
    // Main thread enforcement is handled by the constructor.
    return new OttoBus(this, root);
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

  /**
   * Encapsulates the main thread implementation.  This exists only so that the Handler can be
   * avoided by using {@link TestBus}.
   */
  interface MainThread {
    void enforce();

    void post(Object event);
  }

  private final class AndroidMainThread implements MainThread {

    private final Handler handler = new BusHandler();

    private boolean isOnMainThread() {
      return Thread.currentThread() == Looper.getMainLooper().getThread();
    }

    @Override public void enforce() {
      if (!isOnMainThread()) {
        throw new AssertionError("Event bus accessed from non-main thread "
            + Thread.currentThread());
      }
    }

    @Override public void post(Object event) {
      if (isOnMainThread()) {
        OttoBus.this.post(event);
      } else {
        Message message = handler.obtainMessage();
        message.obj = event;
        handler.sendMessage(message);
      }
    }

    private final class BusHandler extends Handler {
      @Override public void handleMessage(Message message) {
        Object event = message.obj;
        OttoBus.this.post(event);
      }
    }
  }
}
