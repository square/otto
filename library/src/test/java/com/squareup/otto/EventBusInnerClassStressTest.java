// Copyright 2012 Square, Inc.
package com.squareup.otto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertTrue;

/**
 * Stress test of {@link Bus} against inner classes. The anon inner class tests
 * were broken when we switched to weak references.
 *
 * @author Ray Ryan (ray@squareup.com)
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class EventBusInnerClassStressTest {
  public static final int REPS = 1000000;
  boolean called;

  class Sub {
    @Subscribe
    public void in(Object o) {
      called = true;
    }
  }

  Bus eb = Shuttle.createRootBus();

  Sub sub = new Sub();

  @Test public void eventBusOkayWithNonStaticInnerClass() {
    eb.register(sub);
    int i = 0;
    while (i < REPS) {
      called = false;
      i++;
      eb.post(nextEvent(i));
      assertTrue("Failed at " + i, called);
    }
  }

  @Test public void eventBusFailWithAnonInnerClass() {
    eb.register(new Object() {
      @Subscribe
      public void in(String o) {
        called = true;
      }
    });
    int i = 0;
    while (i < REPS) {
      called = false;
      i++;
      eb.post(nextEvent(i));
      assertTrue("Failed at " + i, called);
    }
  }

  @Test public void eventBusNpeWithAnonInnerClassWaitingForObject() {
    eb.register(new Object() {
      @Subscribe
      public void in(Object o) {
        called = true;
      }
    });
    int i = 0;
    while (i < REPS) {
      called = false;
      i++;
      eb.post(nextEvent(i));
      assertTrue("Failed at " + i, called);
    }
  }

  private static String nextEvent(int i) {
    return "" + i;
  }
}
