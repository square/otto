package com.squareup.otto;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.Assert.assertEquals;

/**
 * Concurrency tests to validate @Subscribe and @Produce are registered consistently across multiple threads.
 *
 * These tests may return false positives as there is only a chance that they error within the allotted number of tries.
 *
 * @author John Ericksen
 */
public class EventRegistrationConcurrencyTest {

  private static final int TRIES = 200;
  private static final int CONCURRENT_RUNS = 20;

  /**
   * Tests the @Subscribe registration using concurrent threads.
   *
   * @throws InterruptedException
   */
  @Test
  public void testConcurrentSubscribe() throws InterruptedException {
    for (int i = 0; i < TRIES; i++) {
      ExecutorService executorService = Executors.newFixedThreadPool(2);

      Bus bus = new Bus(ThreadEnforcer.ANY);

      EventSubscribeRunnable subscribeRunnable = new EventSubscribeRunnable(bus);

      for (int j = 0; j < CONCURRENT_RUNS; j++) {
        executorService.execute(subscribeRunnable);
      }

      //wait for all threads to terminate
      executorService.shutdown();
      while (!executorService.awaitTermination(10, TimeUnit.MILLISECONDS)) {}

      int handlerCount = bus.getHandlersForEventType(String.class).size();
      assertEquals("The number of subscribers registered must match the number requested.", CONCURRENT_RUNS, handlerCount);
    }
  }

  /**
   * Class used to asynchronously register a subscriber with the given Bus
   */
  private class EventSubscribeRunnable implements Runnable {

    private Bus bus;

    private EventSubscribeRunnable(Bus bus) {
      this.bus = bus;
    }

    @Override
    public void run() {
      //registers a String event subscriber
      bus.register(new Object() {
        @Subscribe
        public void onString(String event) {
        }
      });
    }
  }

  /**
   * Tests the @Produce registration using concurrent threads.
   * @throws InterruptedException
   */
  @Test
  public void testConcurrentProduces() throws InterruptedException {
    for (int i = 0; i < TRIES; i++) {
      ExecutorService executorService = Executors.newFixedThreadPool(4);

      Bus bus = new Bus(ThreadEnforcer.ANY);

      EventProduceRunnable produceRunnable = new EventProduceRunnable(bus);

      for (int j = 0; j < CONCURRENT_RUNS; j++) {
        executorService.execute(produceRunnable);
      }

      //wait for all threads to terminate
      executorService.shutdown();
      while (!executorService.awaitTermination(10, TimeUnit.MILLISECONDS)) {}

      int exceptionCount = produceRunnable.getIllegalArgumentExceptionCount();
      assertEquals("Incorrect number of exceptions thrown while registering producers.", CONCURRENT_RUNS - 1, exceptionCount);
    }
  }

  /**
   * Class used to asynchronously register a producer with the given Bus
   */
  private class EventProduceRunnable implements Runnable {

    private Bus bus;
    private AtomicInteger illegalArgumentExceptionCount;

    private EventProduceRunnable(Bus bus) {
      this.bus = bus;
      this.illegalArgumentExceptionCount = new AtomicInteger(0);
    }

    @Override
    public void run() {
      try {
        //registers a String event producer
        bus.register(new Object() {
          @Produce
          public String getString() {
            return "hello";
          }
        });
      } catch (IllegalArgumentException e) {
        //It is expected that more than one producer should increment this value
        illegalArgumentExceptionCount.incrementAndGet();
      }
    }

    public int getIllegalArgumentExceptionCount() {
      return illegalArgumentExceptionCount.get();
    }
  }
}
