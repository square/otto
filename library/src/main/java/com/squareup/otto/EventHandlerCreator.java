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

  public Executor asyncExecutor;
  public Executor backgroundExecutor;
  public Handler  uiHandler;

  public EventHandlerCreator(Executor async, Executor background, Handler handler) {
    uiHandler = handler;
    backgroundExecutor = background;
    asyncExecutor = async;
  }

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
      handler = new ExecutorEventHandler(target, method, asyncExecutor);
      break;
    case BACKGROUND:
      handler = new ExecutorEventHandler(target, method, backgroundExecutor);
      break;
    case MAIN:
      if (uiHandler == null) {
        // we can't construct this at class initialization of EventHandler
        // so we should construct it lazily.
        uiHandler = new Handler(Looper.getMainLooper());
      }
      handler = new AndroidHandlerEventHandler(target, method, uiHandler);
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
}
