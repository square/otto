package com.squareup.otto.outside;

import com.squareup.otto.Bus;
import com.squareup.otto.Shuttle;
import com.squareup.otto.ShuttleDispatcher;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;

public class ThreadEnforcementTest {

  Bus bus;
  ExecutorService backgroundThread;

  @Before public void setUp() {
    bus = new Shuttle(ShuttleDispatcher.TEST);
    bus.enable();
    backgroundThread = Executors.newSingleThreadExecutor();
  }

  @Test(expected = AssertionError.class) public void registerEnforcesThread() throws Throwable {
    Future<?> task = backgroundThread.submit(new Runnable() {
      @Override public void run() {
        bus.register(new Object());
      }
    });
    try {
      task.get();
    } catch (Exception e) {
      throw e.getCause();
    }
  }

  @Test(expected = AssertionError.class) public void postEnforcesThread() throws Throwable {
    Future<?> task = backgroundThread.submit(new Runnable() {
      @Override public void run() {
        bus.post(new Object());
      }
    });
    try {
      task.get();
    } catch (Exception e) {
      throw e.getCause();
    }
  }

  @Test(expected = AssertionError.class) public void alsoEnforcesWhenDisabled() throws Throwable {
    bus.disable();
    Future<?> task = backgroundThread.submit(new Runnable() {
      @Override public void run() {
        bus.post(new Object());
      }
    });
    try {
      task.get();
    } catch (Exception e) {
      throw e.getCause();
    }
  }

  @Test(expected = AssertionError.class) public void enableEnforcesThread() throws Throwable {
    bus.disable();
    Future<?> task = backgroundThread.submit(new Runnable() {
      @Override public void run() {
        bus.enable();
      }
    });
    try {
      task.get();
    } catch (Exception e) {
      throw e.getCause();
    }
  }

  @Test(expected = AssertionError.class) public void disableEnforcesThread() throws Throwable {
    Future<?> task = backgroundThread.submit(new Runnable() {
      @Override public void run() {
        bus.disable();
      }
    });
    try {
      task.get();
    } catch (Exception e) {
      throw e.getCause();
    }
  }

  @Test(expected = AssertionError.class) public void destroyEnforcesThread() throws Throwable {
    Future<?> task = backgroundThread.submit(new Runnable() {
      @Override public void run() {
        bus.destroy();
      }
    });
    try {
      task.get();
    } catch (Exception e) {
      throw e.getCause();
    }
  }

  @Test(expected = AssertionError.class) public void spawnEnforcesThread() throws Throwable {
    Future<?> task = backgroundThread.submit(new Runnable() {
      @Override public void run() {
        bus.spawn();
      }
    });
    try {
      task.get();
    } catch (Exception e) {
      throw e.getCause();
    }
  }
}
