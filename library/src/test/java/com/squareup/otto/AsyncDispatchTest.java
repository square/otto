package com.squareup.otto;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.squareup.otto.Subscribe.ExecuteOn;

public class AsyncDispatchTest {


  Bus bus;

  CountDownLatch latch;

  Thread mainThread;

  class AsyncStringCatcher {
    String latestMessage;

    boolean isOnMainThread;

    @Subscribe(thread=ExecuteOn.ASYNC) public void catchString(String myMessage) {
      isOnMainThread = (mainThread == Thread.currentThread());
      latestMessage = myMessage;
      latch.countDown();
    }
  }

  class BackgroundStringCatcher {
    String latestMessage;
    boolean isOnMainThread;

    @Subscribe(thread=ExecuteOn.BACKGROUND) public void catchString(String myMessage) {
      isOnMainThread = (mainThread == Thread.currentThread());
      if (latestMessage == null) {
        // second post will happen after this one is finished.
        bus.post(myMessage + ".final");
      }
      // this will be overwritten by the post in the same thread.
      latestMessage = myMessage;
      latch.countDown();
    }
  }

  class PosterDecidesStringCatcher {
    String latestMessage;
    boolean isOnMainThread;

    @Subscribe(thread=ExecuteOn.POSTER_DECIDES) public void catchString(String myMessage) {
      isOnMainThread = (mainThread == Thread.currentThread());
      latch.countDown();
      if (latch.getCount() == 1) {
        // second post will happen immediately.
        bus.post(myMessage + ".final");
      }
      latestMessage = myMessage;
      // this will be overwritten by the post in the same thread.
    }
  }

  @Before public void setUp() throws Exception {
    bus = new Bus(ThreadEnforcer.ANY);
  }

  @Test public void aynchronousDispatch() throws InterruptedException {
    String myMessage = "DONE";

    latch = new CountDownLatch(1);

    mainThread = Thread.currentThread();

    AsyncStringCatcher subscriber = new AsyncStringCatcher();
    bus.register(subscriber);

    Assert.assertNull(subscriber.latestMessage);
    bus.post(myMessage);

    latch.await(1000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(myMessage, subscriber.latestMessage);
    Assert.assertFalse(subscriber.isOnMainThread);
  }

  @Test public void backgroundDispatchNotReentrant() throws InterruptedException {
    String myMessage = "DONE";

    latch = new CountDownLatch(2);

    mainThread = Thread.currentThread();

    BackgroundStringCatcher subscriber = new BackgroundStringCatcher();
    bus.register(subscriber);

    Assert.assertNull(subscriber.latestMessage);
    bus.post(myMessage);

    latch.await(1000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(myMessage + ".final", subscriber.latestMessage);
    Assert.assertFalse(subscriber.isOnMainThread);
  }

  @Test public void posterDecides() throws InterruptedException {
    String myMessage = "DONE";

    latch = new CountDownLatch(2);

    mainThread = Thread.currentThread();

    PosterDecidesStringCatcher subscriber = new PosterDecidesStringCatcher();
    bus.register(subscriber);

    Assert.assertNull(subscriber.latestMessage);
    bus.post(myMessage);

    // even though this is on the same thread, re-entrancy isn't desirous.
    Assert.assertEquals(myMessage + ".final", subscriber.latestMessage);
    Assert.assertTrue(subscriber.isOnMainThread);

  }

}
