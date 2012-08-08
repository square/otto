/*
 * Copyright (C) 2012 Square, Inc.
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

import java.util.*;

import static junit.framework.Assert.assertEquals;

/** Test case for subscribers which unregister while handling an event. */
public class UnregisteringHandlerTest {

  private static final String EVENT = "Hello";

  private Bus bus;

  @Before public void setUp() throws Exception {
    final Finder finder = new SortedHandlerFinder();
    bus = new Bus(ThreadEnforcer.NONE) {
      @Override Finder obtainFinder(Class<?> type) {
        return finder;
      }
    };
  }

  @Test public void unregisterInHandler() {
    UnregisteringStringCatcher catcher = new UnregisteringStringCatcher(bus);
    bus.register(catcher);
    bus.post(EVENT);

    List<String> expectedEvents = new ArrayList<String>();
    expectedEvents.add(EVENT);

    assertEquals("One correct event should be delivered.", Arrays.asList(EVENT), catcher.getEvents());

    bus.post(EVENT);
    bus.post(EVENT);
    assertEquals("Shouldn't catch any more events when unregistered.", expectedEvents, catcher.getEvents());
  }

  @Test public void unregisterInHandlerWhenEventProduced() throws Exception {
    UnregisteringStringCatcher catcher = new UnregisteringStringCatcher(bus);

    bus.register(new StringProducer());
    bus.register(catcher);
    assertEquals(Arrays.asList(StringProducer.VALUE), catcher.getEvents());

    bus.post(EVENT);
    bus.post(EVENT);
    assertEquals("Shouldn't catch any more events when unregistered.",
        Arrays.asList(StringProducer.VALUE), catcher.getEvents());
  }

  /** Delegates to {@code HandlerFinder.ANNOTATED}, then sorts results by {@code EventHandler#toString} */
  static class SortedHandlerFinder extends ReflectionFinder {

    static final Comparator<Subscriber> SUBSCRIBER_COMPARATOR = new Comparator<Subscriber>() {
      @Override
      public int compare(Subscriber eventHandler, Subscriber eventHandler1) {
        return eventHandler.toString().compareTo(eventHandler1.toString());
      }
    };

    @Override public void install(Object instance, Bus bus) {
      Map<Class<?>, Producer> foundProducers = findAllProducers(instance);
      for (Map.Entry<Class<?>, Producer> entry : foundProducers.entrySet()) {
        bus.installProducer(entry.getKey(), entry.getValue());
      }

      Map<Class<?>, Set<Subscriber>> foundHandlersMap = findAllSubscribers(instance);
      for (Map.Entry<Class<?>, Set<Subscriber>> entry : foundHandlersMap.entrySet()) {
        SortedSet<Subscriber> sorted = new TreeSet<Subscriber>(SUBSCRIBER_COMPARATOR);
        sorted.addAll(entry.getValue());
        Class<?> type = entry.getKey();
        for (Subscriber foundSubscriber : sorted) {
          bus.installSubscriber(type, foundSubscriber);
        }
      }
    }
  }
}
