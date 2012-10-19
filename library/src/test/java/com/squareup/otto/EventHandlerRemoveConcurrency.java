package com.squareup.otto;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertFalse;

/**
 * Test cases highlighting concurrency when registering, unregistering and triggering on events.
 *
 * @author John Ericksen
 */
public class EventHandlerRemoveConcurrency {

  private static final String EVENT = "event";
  private static final String BUS_IDENTIFIER = "test-bus";

  private Bus bus;

  public class Provider{

    @Produce
    public String produceEvent(){
      return EVENT;
    }
  }

  public class EventWatcher{
    private boolean registered = false;
    private boolean unregistered = false;
    private boolean calledAfterUnregister = false;

    @Subscribe
    public void event(String event){
      try {
        //adding some time to allow unregisters to happen often
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      calledAfterUnregister = unregistered;
    }

    public synchronized void register(){
        registered = true;
        bus.register(this);
    }

    public synchronized void unregisterMe(){
      if(registered){
        bus.unregister(this);
        unregistered = true;
      }
    }

    public boolean isCalledAfterUnregister() {
      return calledAfterUnregister;
    }
  }

  public class EventRegistration implements Runnable{

    private EventWatcher watcher;

    public EventRegistration(EventWatcher watcher){
      this.watcher = watcher;
    }

    @Override
    public void run() {
      watcher.register();
    }
  }

  public class EventUnregistration implements Runnable{

    private EventWatcher watcher;

    public EventUnregistration(EventWatcher watcher){
      this.watcher = watcher;
    }

    @Override
    public void run() {
      watcher.unregisterMe();
    }
  }

  public class EventTrigger implements Runnable {
    @Override
    public void run() {
      bus.post(EVENT);
    }
  }

  @Before
  public void setup(){
    bus = new Bus(ThreadEnforcer.ANY, BUS_IDENTIFIER);
  }

  @Test
  public void hammerLifecycleTest() throws InterruptedException {

    bus.register(new Provider());

    ExecutorService executorService = Executors.newFixedThreadPool(4);

    List<EventWatcher> registrations = new ArrayList<EventWatcher>();

    for(int i = 0; i < 100; i++){
      EventWatcher eventWatcher = new EventWatcher();
      registrations.add(eventWatcher);
      executorService.execute(new EventRegistration(eventWatcher));
      executorService.execute(new EventUnregistration(eventWatcher));
      executorService.execute(new EventTrigger());
    }

    executorService.shutdown();
    while (!executorService.awaitTermination(10, TimeUnit.MILLISECONDS)) {}

    for (EventWatcher registration : registrations) {
      assertFalse(registration.isCalledAfterUnregister());
    }
  }
}
