/*
 * Copyright (C) 2013 Square, Inc.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Test case for {@link Bus}.
 *
 * @author Cliff Biffle
 */
@RunWith(RobolectricTestRunner.class) @Config(manifest = Config.NONE)
public class BusTest {
  private static final String EVENT = "Hello";

  private Bus bus = Otto.createBus();

  @Test public void basicCatcherDistribution() {
    StringCatcher catcher = new StringCatcher();
    bus.register(catcher);

    bus.post(EVENT);

    List<String> events = catcher.getEvents();
    assertEquals("Only one event should be delivered.", 1, events.size());
    assertEquals("Correct string should be delivered.", EVENT, events.get(0));
  }

  /**
   * Tests that events are distributed to any subscribers to their type or any
   * supertype, including interfaces and superclasses.
   *
   * Also checks delivery ordering in such cases.
   */
  @Test public void polymorphicDistribution() {
    // Three catchers for related types String, Object, and Comparable<?>.
    // String isa Object
    // String isa Comparable<?>
    // Comparable<?> isa Object
    StringCatcher stringCatcher = new StringCatcher();

    final List<Object> objectEvents = new ArrayList<Object>();
    Object objCatcher = new Object() {
      @SuppressWarnings("unused") @Subscribe public void eat(Object food) {
        objectEvents.add(food);
      }
    };

    bus.register(stringCatcher);
    bus.register(objCatcher);

    // Two additional event types: Object and Comparable<?> (played by Integer)
    final Object OBJ_EVENT = new Object();
    final Object COMP_EVENT = new Integer(6);

    bus.post(EVENT);
    bus.post(OBJ_EVENT);
    bus.post(COMP_EVENT);

    // Check the StringCatcher...
    List<String> stringEvents = stringCatcher.getEvents();
    assertEquals("Only one String should be delivered.", 1, stringEvents.size());
    assertEquals("Correct string should be delivered.", EVENT, stringEvents.get(0));

    // Check the Catcher<Object>...
    assertEquals("Three Objects should be delivered.", 3, objectEvents.size());
    assertEquals("String fixture must be first object delivered.", EVENT, objectEvents.get(0));
    assertEquals("Object fixture must be second object delivered.", OBJ_EVENT, objectEvents.get(1));
    assertEquals("Comparable fixture must be thirdobject delivered.", COMP_EVENT,
        objectEvents.get(2));
  }

  @Test public void testNullInteractions() {
    try {
      bus.register(null);
      fail("Should have thrown an NPE on register.");
    } catch (NullPointerException e) {
    }
    try {
      bus.post(null);
      fail("Should have thrown an NPE on post.");
    } catch (NullPointerException e) {
    }
  }

  @Test public void subscribingOnlyAllowedOnPublicMethods() {
    try {
      bus.register(new Object() {
        @Subscribe protected void method(Object o) {
        }
      });
      fail();
    } catch (IllegalArgumentException expected) {
      // Expected.
    }
    try {
      bus.register(new Object() {
        @Subscribe void method(Object o) {
        }
      });
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      bus.register(new Object() {
        @Subscribe private void method(Object o) {
        }
      });
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test public void missingSubscribe() {
    bus.register(new Object());
  }

  @Test public void subscribingToInterfaceFails() {
    try {
      Otto.createBus().register(new InterfaceSubscriber());
      fail("Annotation finder allowed subscription to illegal interface type.");
    } catch (IllegalArgumentException expected) {
      // Do nothing.
    }
  }

  @Test public void testExceptionThrowingHandler() throws Exception {
    bus.register(new ExceptionThrowingHandler());
    try {
      bus.post("I love tacos");
      fail("Should have failed due to exception-throwing handler.");
    } catch (RuntimeException e) {
      // Expected
    }
  }

  private class ExceptionThrowingHandler {
    @Subscribe public void subscribeToString(String value) {
      throw new IllegalStateException("Dude where's my car?");
    }
  }

  static class InterfaceSubscriber {
    @Subscribe public void whatever(Serializable thingy) {
      // Do nothing.
    }
  }
}
