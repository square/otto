/*
 * Copyright (C) 2013 Square, Inc.
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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class OttoBus implements Bus {

  private static final class Shared {
    final MainThread mainThread;
    /** Used to find handler methods in register and unregister. */
    final HandlerFinder handlerFinder;
    final DeadEventHandler deadEventHandler;
    // ArrayDeque is an array-backed queue that grows and shrinks, so it won't create a lot of
    // unnecessary objects for the GC to deal with.
    final Queue<Object> dispatchEventQueue;
    final Queue<OttoBus> dispatchBusQueue;
    final Queue<EventHandler> dispatchHandlerQueue;
    final HierarchyFlattener hierarchyFlattener;
    final OttoBus root;

    Shared(MainThread mainThread, HandlerFinder handlerFinder, DeadEventHandler deadEventHandler,
        OttoBus root) {
      this.mainThread = mainThread;
      this.handlerFinder = handlerFinder;
      this.deadEventHandler = deadEventHandler;
      this.root = root;
      this.hierarchyFlattener = new HierarchyFlattener();
      this.dispatchEventQueue = new ArrayDeque<Object>();
      this.dispatchBusQueue = new ArrayDeque<OttoBus>();
      this.dispatchHandlerQueue = new ArrayDeque<EventHandler>();
    }
  }

  private final Map<Class<?>, Set<EventHandler>> handlersByEventType =
      new HashMap<Class<?>, Set<EventHandler>>();
  // We use LinkedHashSet essentially so that our tests are deterministic.  Consistent iteration
  // over children is NOT part of the contract and should not be relied upon by clients.
  private final Set<OttoBus> children = new LinkedHashSet<OttoBus>();

  /** null if a root bus. */
  private final OttoBus parent;
  private final Shared shared;
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
    if (mainThread == null) mainThread = new AndroidMainThread();
    mainThread.enforce();
    this.parent = null;
    this.shared = new Shared(mainThread, handlerFinder, deadEventHandler, this);
  }

  /** Create a non-root bus. */
  private OttoBus(OttoBus parent, Shared shared) {
    shared.mainThread.enforce();
    this.parent = parent;
    this.shared = shared;
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
    shared.mainThread.enforce();
    if (destroyed) throw new IllegalStateException("Bus has been destroyed.");

    Map<Class<?>, Set<EventHandler>> handlers = shared.handlerFinder.findAllSubscribers(subscriber);

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
    shared.mainThread.enforce();
    if (!destroyed) {
      boolean dispatched = shared.root.doPost(event);
      if (dispatched) {
        dispatchQueuedEvents();
        return;
      }
    }
    shared.deadEventHandler.onDeadEvent(event);
  }

  /** @return true iff event was dispatched to some subscriber. */
  private boolean doPost(Object event) {
    boolean dispatched = false;
    if (destroyed) return false;
    Set<Class<?>> dispatchTypes = shared.hierarchyFlattener.flatten(event.getClass());

    for (Class eventType : dispatchTypes) {
      Set<EventHandler> eventHandlers = handlersByEventType.get(eventType);
      if (eventHandlers != null && !eventHandlers.isEmpty()) {
        dispatched = true;
        for (EventHandler handler : eventHandlers) {
          shared.dispatchBusQueue.add(this);
          shared.dispatchEventQueue.add(event);
          shared.dispatchHandlerQueue.add(handler);
        }
      }
    }
    for (OttoBus child : children) {
      dispatched |= child.doPost(event);
    }
    return dispatched;
  }

  private void dispatchQueuedEvents() {
    if (dispatching) return;
    try {
      dispatching = true;
      while ((!destroyed) && (!shared.dispatchEventQueue.isEmpty())) {
        OttoBus bus = shared.dispatchBusQueue.poll();
        EventHandler handler = shared.dispatchHandlerQueue.poll();
        Object event = shared.dispatchEventQueue.poll();
        if (bus.destroyed) continue;
        try {
          handler.handleEvent(event);
        } catch (InvocationTargetException e) {
          Class<?> eventType = event.getClass();
          throwRuntimeException("Could not dispatch event: " + eventType + " to handler " + handler,
              e);
        }
      }
    } finally {
      shared.dispatchBusQueue.clear();
      shared.dispatchHandlerQueue.clear();
      shared.dispatchEventQueue.clear();
      dispatching = false;
    }
  }

  @Override public void postOnMainThread(final Object event) {
    shared.mainThread.forbid();
    shared.mainThread.post(event);
  }

  @Override public void destroy() {
    shared.mainThread.enforce();

    if (destroyed) return;
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
    return new OttoBus(this, shared);
  }

  /**
   * Retrieves a mutable set of the currently registered handlers for {@code type}.  If no handlers
   * are currently registered for {@code type}, this method may either return {@code null} or an
   * empty set.
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

    void forbid();

    void post(Object event);
  }

  private final class AndroidMainThread implements MainThread {

    private final Handler handler = new BusHandler();

    private boolean isOnMainThread() {
      return Thread.currentThread() == Looper.getMainLooper().getThread();
    }

    @Override public void enforce() {
      if (!isOnMainThread()) {
        throw new AssertionError(
            "Illegal bus access from non-main thread " + Thread.currentThread());
      }
    }

    @Override public void forbid() {
      if (isOnMainThread()) throw new AssertionError("Illegal bus access from main thread");
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
