package com.squareup.otto;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Test cases highlighting concurrency when registering, unregistering and triggering on events.
 *
 * @author John Ericksen
 */
public class EventConcurrencyTest {

  private static final String EVENT = "event";
  private static final String BUS_IDENTIFIER = "test-bus";

  private Bus bus;
  private ExecutorService executor;

  public class StringProducer {
    @Produce public String produceEvent() {
      return EVENT;
    }
  }

  public class RegisterableBase {
    private boolean registered = false;
    private boolean unregistered = false;

    public synchronized void register() {
      registered = true;
      bus.register(this);
    }

    public synchronized void unregister() {
      if(registered) {
        bus.unregister(this);
        unregistered = true;
      }
    }

    public boolean isUnregistered() {
      return unregistered;
    }
  }

  public class EventWatcher extends RegisterableBase {
    private boolean calledAfterUnregister = false;

    @Subscribe public void event(String event) {
      try {
        // Adding some time to allow unregisters to happen often.
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      calledAfterUnregister = isUnregistered();
    }

    public boolean isCalledAfterUnregister() {
      return calledAfterUnregister;
    }
  }

  public class DeadlockTrigger extends RegisterableBase {
    private int count = 0;

    @Subscribe public void deadlock(String event) {
      if(count < 2){
        bus.post(event);
      }
      count++;
    }
  }

  public class EventRegistration implements Runnable {

    private RegisterableBase registerable;

    public EventRegistration(RegisterableBase registerable) {
      this.registerable = registerable;
    }

    @Override public void run() {
      registerable.register();
    }
  }

  public class EventUnregistration implements Runnable {

    private RegisterableBase registerable;

    public EventUnregistration(RegisterableBase registerable) {
      this.registerable = registerable;
    }

    @Override public void run() {
      registerable.unregister();
    }
  }

  public class EventTrigger implements Runnable {
    @Override public void run() {
      bus.post(EVENT);
    }
  }

  @Before public void setup() {
    bus = new Bus(ThreadEnforcer.ANY, BUS_IDENTIFIER);
    executor = Executors.newFixedThreadPool(4);
  }

  /**
   * Tests for the case where multiple threads register and unregister and verifies that events are not triggered
   * after a handler is unregistered.
   *
   * @throws InterruptedException
   */
  @Test public void calledAfterUnregisterTest() throws InterruptedException {

    bus.register(new StringProducer());

    List<EventWatcher> registrations = new ArrayList<EventWatcher>();

    for(int i = 0; i < 100; i++) {
      EventWatcher eventWatcher = new EventWatcher();
      registrations.add(eventWatcher);
      executor.execute(new EventRegistration(eventWatcher));
      executor.execute(new EventUnregistration(eventWatcher));
      executor.execute(new EventTrigger());
    }

    executor.shutdown();
    while (!executor.awaitTermination(10, TimeUnit.MILLISECONDS)) {}

    for (EventWatcher registration : registrations) {
      assertFalse(registration.isCalledAfterUnregister());
    }
  }

  /**
   * Exercises possible deadlock conditions where an event triggers itself, both during registration and during
   * event posting.
   *
   * @throws InterruptedException
   */
  @Test public void deadlockTest() throws InterruptedException {
    bus.register(new com.squareup.otto.StringProducer());

    for(int i = 0; i < 100; i++) {
      final DeadlockTrigger deadlockTrigger = new DeadlockTrigger();
      executor.execute(new EventRegistration(deadlockTrigger));
      executor.execute(new EventUnregistration(deadlockTrigger));
      executor.execute(new EventTrigger());
    }

    // Wait 100ms for threads to finish.  If not finished, assume deadlock.
    executor.shutdown();
    assertTrue(executor.awaitTermination(100, TimeUnit.MILLISECONDS));
  }
}
