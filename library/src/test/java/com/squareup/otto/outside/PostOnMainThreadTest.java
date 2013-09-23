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

import android.os.Looper;
import com.squareup.otto.Bus;
import com.squareup.otto.Otto;
import com.squareup.otto.StringCatcher;
import com.squareup.otto.Subscribe;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class PostOnMainThreadTest {

  private static final String EVENT = "Hello";
  Bus bus = Otto.createBus();

  @Test public void postFromMainThreadIsSynchronous() {
    StringCatcher catcher = new StringCatcher();
    bus.register(catcher);
    bus.post(EVENT);
    catcher.assertThatEvents("Subscriber should receive posted event.").containsExactly(EVENT);
  }

  @Test public void postFromBackgroundThreadIsReceivedOnMainThread() throws Exception {
    class Subscriber {
      String result;

      @Subscribe public void callbackOnMainThread(String result) {
        this.result = result;
        Thread mainThread = Looper.getMainLooper().getThread();
        assertThat(Thread.currentThread()).isSameAs(mainThread);
      }
    }

    Subscriber subscriber = new Subscriber();
    bus.register(subscriber);

    Thread backgroundThread = new Thread() {
      @Override public void run() {
        bus.postOnMainThread(EVENT);
      }
    };
    backgroundThread.start();
    backgroundThread.join();

    Robolectric.runUiThreadTasks();

    assertThat(subscriber.result).isSameAs(EVENT);
  }
}
