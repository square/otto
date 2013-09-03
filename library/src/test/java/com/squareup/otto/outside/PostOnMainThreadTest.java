package com.squareup.otto.outside;

import android.os.Looper;
import com.squareup.otto.Bus;
import com.squareup.otto.OttoBus;
import com.squareup.otto.StringCatcher;
import com.squareup.otto.Subscribe;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static com.squareup.otto.DeadEventHandler.IGNORE_DEAD_EVENTS;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class PostOnMainThreadTest {

  private static final String EVENT = "Hello";

  Bus bus;

  @Before public void setUp() {
    bus = new OttoBus(IGNORE_DEAD_EVENTS);
  }

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
