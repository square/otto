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

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.Handler;
import android.os.Looper;

import com.squareup.otto.Subscribe.ExecuteOn;

/**
 * Creates an EventHandlerCreator based on a
 * {@link Subscribe.ExecuteOn} value.
 * <p>This implementation takes a {@link android.os.Handler}
 * for the main thread, and two {@link java.util.concurrent.Executor}
 * instances for each of the background and async thread pools.</p>
 *
 * @author James Hugman
 */
public class EventHandlerCreator {

  private static class OttoThreadFactory implements ThreadFactory {

    private final String threadName;

    private final AtomicInteger threadNumber = new AtomicInteger();

    OttoThreadFactory(Subscribe.ExecuteOn threadName) {
      this.threadName = threadName.name();
    }

    @Override
    public Thread newThread(Runnable r) {
      return new Thread(r, threadName + "-" + threadNumber.getAndIncrement());
    }
  }

  private Executor asyncExecutor;
  private Executor backgroundExecutor;
  private Handler  uiHandler;

  /**
   * Use an executor for background thread pool.
   * @param async
   * @param background
   * @param handler
   */
  public EventHandlerCreator(Executor async, Executor background, Handler handler) {
    uiHandler = handler;
    backgroundExecutor = background;
    asyncExecutor = async;
  }

  /**
   * By default, use a single thread executor and unbounded cached thread pool for
   * ASYNC and BACKGROUND.
   */
  public EventHandlerCreator() {
    this(Executors.newCachedThreadPool(new OttoThreadFactory(ExecuteOn.ASYNC)),
         Executors.newSingleThreadExecutor(new OttoThreadFactory(ExecuteOn.BACKGROUND)),
         null
    );
  }

  public EventHandler createHandler(ExecuteOn thread, Object target,
      Method method) {
    EventHandler handler;
    switch (thread) {
    case ASYNC:
      handler = createAsycHandler(target, method);
      break;
    case BACKGROUND:
      handler = createBackgroundHandler(target, method);
      break;
    case MAIN:
      handler = createMainHandler(target, method);
      break;
    case POSTER_DECIDES:
      handler = new EventHandler(target, method);
      break;
    default:
      handler = new EventHandler(target, method);
      break;
    }
    return handler;
  }

  protected EventHandler createMainHandler(Object target, Method method) {
    EventHandler handler;
    if (uiHandler == null) {
      // we can't construct this at class initialization of EventHandler
      // so we should construct it lazily.
      uiHandler = new Handler(Looper.getMainLooper());
    }
    handler = new EventHandlerWithHandler(target, method, uiHandler);
    return handler;
  }

  protected EventHandler createBackgroundHandler(Object target, Method method) {
    EventHandler handler;
    handler = new EventHandlerWithExecutor(target, method, backgroundExecutor);
    return handler;
  }

  protected EventHandler createAsycHandler(Object target, Method method) {
    EventHandler handler;
    handler = new EventHandlerWithExecutor(target, method, asyncExecutor);
    return handler;
  }
}
