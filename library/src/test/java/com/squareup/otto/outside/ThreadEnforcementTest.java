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

package com.squareup.otto.outside;

import com.squareup.otto.Bus;
import com.squareup.otto.OttoBus;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.squareup.otto.DeadEventHandler.IGNORE_DEAD_EVENTS;

@RunWith(RobolectricTestRunner.class)
public class ThreadEnforcementTest {

  Bus bus;
  ExecutorService backgroundThread;

  @Before public void setUp() {
    bus = new OttoBus(IGNORE_DEAD_EVENTS);
    backgroundThread = Executors.newSingleThreadExecutor();
  }

  @Test(expected = AssertionError.class) public void registerEnforcesThread() throws Throwable {
    enforcesThread(new Runnable() {
      @Override public void run() {
        bus.register(new Object());
      }
    });
  }

  @Test(expected = AssertionError.class) public void postEnforcesThread() throws Throwable {
    enforcesThread(new Runnable() {
      @Override public void run() {
        bus.post(new Object());
      }
    });
  }

  @Test(expected = AssertionError.class) public void destroyEnforcesThread() throws Throwable {
    enforcesThread(new Runnable() {
      @Override public void run() {
        bus.destroy();
      }
    });
  }

  @Test(expected = AssertionError.class) public void spawnEnforcesThread() throws Throwable {
    enforcesThread(new Runnable() {
      @Override public void run() {
        bus.spawn();
      }
    });
  }

  @Test(expected = AssertionError.class) public void busBuilderEnforcesThread() throws Throwable {
    enforcesThread(new Runnable() {
      @Override public void run() {
        new OttoBus(IGNORE_DEAD_EVENTS);
      }
    });
  }

  public void enforcesThread(Runnable runnable) throws Throwable {
    Future<?> task = backgroundThread.submit(runnable);
    try {
      task.get();
    } catch (Exception e) {
      throw e.getCause();
    }
  }
}
