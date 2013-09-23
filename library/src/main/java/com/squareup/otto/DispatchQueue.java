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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.Queue;

final class DispatchQueue {
  // ArrayDeque is an array-backed queue that grows and shrinks, so it won't create a lot of
  // unnecessary objects for the GC to deal with.  We similarly have three queues instead of one
  // so that we don't need an aggregated element instance for each enqueued dispatch request.
  private final Queue<Object> dispatchEventQueue = new ArrayDeque<Object>();
  private final Queue<OttoBus> dispatchBusQueue = new ArrayDeque<OttoBus>();
  private final Queue<EventHandler> dispatchHandlerQueue  = new ArrayDeque<EventHandler>();

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

  void enqueueForDispatch(OttoBus originBus, Object event, EventHandler handler) {
    dispatchBusQueue.add(originBus);
    dispatchEventQueue.add(event);
    dispatchHandlerQueue.add(handler);
  }

  void dispatchNext() {
    OttoBus bus = dispatchBusQueue.poll();
    EventHandler handler = dispatchHandlerQueue.poll();
    Object event = dispatchEventQueue.poll();
    if (bus.isDestroyed()) return;
    try {
      handler.handleEvent(event);
    } catch (InvocationTargetException e) {
      Class<?> eventType = event.getClass();
      throwRuntimeException("Could not dispatch event: " + eventType + " to handler " + handler,
          e);
    }
  }

  void clear() {
    dispatchBusQueue.clear();
    dispatchHandlerQueue.clear();
    dispatchEventQueue.clear();
  }

  boolean isEmpty() {
    return dispatchBusQueue.isEmpty();
  }
}
