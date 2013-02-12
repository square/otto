/*
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

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Test case for {@link Bus}.
 *
 * @author Cliff Biffle
 */
public class BusTest {
  private static final String EVENT = "Hello";
  private static final String BUS_IDENTIFIER = "test-bus";

  private Bus bus;

  @Before public void setUp() throws Exception {
    bus = new Bus(ThreadEnforcer.NONE, BUS_IDENTIFIER);
  }

  @Test public void basicCatcherDistribution() {
    StringCatcher catcher = new StringCatcher();
    bus.register(catcher);

    Set<EventHandler> wrappers = bus.getHandlersForEventType(String.class);
    assertNotNull("Should have at least one method registered.", wrappers);
    assertEquals("One method should be registered.", 1, wrappers.size());

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
      @SuppressWarnings("unused")
      @Subscribe public void eat(Object food) {
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
    assertEquals("Only one String should be delivered.",
        1, stringEvents.size());
    assertEquals("Correct string should be delivered.",
        EVENT, stringEvents.get(0));

    // Check the Catcher<Object>...
    assertEquals("Three Objects should be delivered.",
        3, objectEvents.size());
    assertEquals("String fixture must be first object delivered.",
        EVENT, objectEvents.get(0));
    assertEquals("Object fixture must be second object delivered.",
        OBJ_EVENT, objectEvents.get(1));
    assertEquals("Comparable fixture must be thirdobject delivered.",
        COMP_EVENT, objectEvents.get(2));
  }

  @Test public void deadEventForwarding() {
    GhostCatcher catcher = new GhostCatcher();
    bus.register(catcher);

    // A String -- an event for which noone has registered.
    bus.post(EVENT);

    List<DeadEvent> events = catcher.getEvents();
    assertEquals("One dead event should be delivered.", 1, events.size());
    assertEquals("The dead event should wrap the original event.",
        EVENT, events.get(0).event);
  }

  @Test public void deadEventPosting() {
    GhostCatcher catcher = new GhostCatcher();
    bus.register(catcher);

    bus.post(new DeadEvent(this, EVENT));

    List<DeadEvent> events = catcher.getEvents();
    assertEquals("The explicit DeadEvent should be delivered.",
        1, events.size());
    assertEquals("The dead event must not be re-wrapped.",
        EVENT, events.get(0).event);
  }

  @Test public void publisherCalledForExistingSubscribers() {
    StringCatcher catcher = new StringCatcher();
    StringPublisher publisher = new StringPublisher();

    bus.register(catcher);
    bus.register(publisher);

    assertEquals(Arrays.asList(StringPublisher.VALUE), catcher.getEvents());
  }

  @Test public void publishingNullIsNoOp() {
    LazyStringPublisher publisher = new LazyStringPublisher();
    StringCatcher catcher = new StringCatcher();

    bus.register(catcher);
    bus.register(publisher);

    assertTrue(catcher.getEvents().isEmpty());

    bus.unregister(publisher);
    publisher.value = "Foo";
    bus.register(publisher);

    assertEquals(Arrays.asList("Foo"), catcher.getEvents());
  }

  @Test public void subscribingOrPublishingOnlyAllowedOnPublicMethods() {
    try {
      bus.register(new Object() {
        @Subscribe protected void method(Object o) {}
      });
      fail();
    } catch (IllegalArgumentException expected) {
      // Expected.
    }
    try {
      bus.register(new Object() {
        @Subscribe void method(Object o) {}
      });
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      bus.register(new Object() {
        @Subscribe private void method(Object o) {}
      });
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      bus.register(new Object() {
        @Publish protected Object method() { return null; }
      });
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      bus.register(new Object() {
        @Publish Object method() { return null; }
      });
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      bus.register(new Object() {
        @Publish private Object method() { return null; }
      });
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void voidPublisherThrowsException() throws Exception {
    class VoidPublisher {
      @Publish public void things() {}
    }
    bus.register(new VoidPublisher());
  }

  @Test public void publisherUnregisterAllowsReregistering() {
    StringPublisher publisher1 = new StringPublisher();
    StringPublisher publisher2 = new StringPublisher();

    bus.register(publisher1);
    bus.unregister(publisher1);
    bus.register(publisher2);
  }

  @Test public void flattenHierarchy() {
    HierarchyFixture fixture = new HierarchyFixture();
    Set<Class<?>> hierarchy = bus.flattenHierarchy(fixture.getClass());

    assertEquals(3, hierarchy.size());
    assertContains(Object.class, hierarchy);
    assertContains(HierarchyFixtureParent.class, hierarchy);
    assertContains(HierarchyFixture.class, hierarchy);
  }

  @Test public void missingSubscribe() {
    bus.register(new Object());
  }

  @Test public void unregister() {
    StringCatcher catcher1 = new StringCatcher();
    StringCatcher catcher2 = new StringCatcher();
    try {
      bus.unregister(catcher1);
      fail("Attempting to unregister an unregistered object succeeded");
    } catch (IllegalArgumentException expected) {
      // OK.
    }

    bus.register(catcher1);
    bus.post(EVENT);
    bus.register(catcher2);
    bus.post(EVENT);

    List<String> expectedEvents = new ArrayList<String>();
    expectedEvents.add(EVENT);
    expectedEvents.add(EVENT);

    assertEquals("Two correct events should be delivered.",
                 expectedEvents, catcher1.getEvents());

    assertEquals("One correct event should be delivered.",
        Arrays.asList(EVENT), catcher2.getEvents());

    bus.unregister(catcher1);
    bus.post(EVENT);

    assertEquals("Shouldn't catch any more events when unregistered.",
                 expectedEvents, catcher1.getEvents());
    assertEquals("Two correct events should be delivered.",
                 expectedEvents, catcher2.getEvents());

    try {
      bus.unregister(catcher1);
      fail("Attempting to unregister an unregistered object succeeded");
    } catch (IllegalArgumentException expected) {
      // OK.
    }

    bus.unregister(catcher2);
    bus.post(EVENT);
    assertEquals("Shouldn't catch any more events when unregistered.",
                 expectedEvents, catcher1.getEvents());
    assertEquals("Shouldn't catch any more events when unregistered.",
                 expectedEvents, catcher2.getEvents());
  }

  @Test public void producingNullIsInvalid() {
    try {
      bus.register(new NullPublisher());
    } catch (IllegalStateException expected) {
      // Expected.
    }
  }

  @Test public void testExceptionThrowingPublisher() throws Exception {
    bus.register(new ExceptionThrowingPublisher());
    try {
      bus.register(new DummySubscriber());
      fail("Should have failed due to exception-throwing publisher.");
    } catch (RuntimeException e) {
      // Expected.
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

  private class ExceptionThrowingPublisher {
    @Publish public String publishThingsExceptionally() {
      throw new IllegalStateException("Bogus!");
    }
  }

  private class DummySubscriber {
    @Subscribe public void subscribeToString(String value) {
    }
  }

  private class ExceptionThrowingHandler {
    @Subscribe public void subscribeToString(String value) {
      throw new IllegalStateException("Dude where's my car?");
    }
  }

  private <T> void assertContains(T element, Collection<T> collection) {
    assertTrue("Collection must contain " + element,
        collection.contains(element));
  }

  /**
   * A collector for DeadEvents.
   *
   * @author cbiffle
   *
   */
  public static class GhostCatcher {
    private List<DeadEvent> events = new ArrayList<DeadEvent>();

    @Subscribe
    public void ohNoesIHaveDied(DeadEvent event) {
      events.add(event);
    }

    public List<DeadEvent> getEvents() {
      return events;
    }
  }

  public static class NullPublisher {
    @Publish public Object publishNull() {
      return null;
    }

    @Subscribe public void method(Object event) {
      fail();
    }
  }

  public interface HierarchyFixtureInterface {
    // Exists only for hierarchy mapping; no members.
  }

  public interface HierarchyFixtureSubinterface
      extends HierarchyFixtureInterface {
    // Exists only for hierarchy mapping; no members.
  }

  public static class HierarchyFixtureParent
      implements HierarchyFixtureSubinterface {
    // Exists only for hierarchy mapping; no members.
  }

  public static class HierarchyFixture extends HierarchyFixtureParent {
    // Exists only for hierarchy mapping; no members.
  }

}
