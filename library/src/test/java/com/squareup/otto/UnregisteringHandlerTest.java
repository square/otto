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

import com.squareup.otto.internal.Finder;
import com.squareup.otto.internal.Producer;
import com.squareup.otto.internal.Subscriber;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/** Test case for subscribers which unregister while handling an event. */
public class UnregisteringHandlerTest {

  private static final String EVENT = "Hello";

  private Bus bus;

  @Before public void setUp() throws Exception {
    final Finder finder = new SortedHandlerFinder();
    bus = new BasicBus(ThreadEnforcer.NONE) {
      @Override Finder obtainFinder(Class<?> type) {
        return finder;
      }
    };
  }

  @Ignore // TODO This test is less NUTS, but currently not working with the new system.
  @Test public void unregisterInHandler() {
    UnregisteringStringCatcher catcher = new UnregisteringStringCatcher(bus);
    bus.register(catcher);
    bus.post(EVENT);

    assertEquals("One correct event should be delivered.", Arrays.asList(EVENT),
        catcher.getEvents());

    bus.post(EVENT);
    bus.post(EVENT);
    assertEquals("Shouldn't catch any more events when unregistered.", Arrays.asList(EVENT),
        catcher.getEvents());
  }

  @Ignore // TODO This test is NUTS! Need to talk to Logan before working around it.
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

  @Test public void unregisterProducerInHandler() throws Exception {
    final Object producer = new Object() {
      private int calls = 0;

      @Produce public String produceString() {
        calls++;
        if (calls > 1) {
          fail("Should only have been called once, then unregistered and never called again.");
        }
        return "Please enjoy this hand-crafted String.";
      }
    };
    bus.register(producer);
    bus.register(new Object() {
      @Subscribe public void firstUnsubscribeTheProducer(String produced) {
        bus.unregister(producer);
      }

      @Subscribe public void shouldNeverBeCalled(String uhoh) {
        fail("Shouldn't receive events from an unregistered producer.");
      }
    });
  }

  /**
   * Delegates to {@code HandlerFinder.ANNOTATED}, then sorts results by {@code
   * ReflectionSubscriber#toString}
   */
  static class SortedHandlerFinder extends ReflectionFinder {

    static final Comparator<Subscriber> SUBSCRIBER_COMPARATOR = new Comparator<Subscriber>() {
      @Override public int compare(Subscriber subscriber1, Subscriber subscriber2) {
        return subscriber1.toString().compareTo(subscriber2.toString());
      }
    };

    @Override public void install(Object instance, BasicBus.Installer bus) {

      Map<Class<?>, Producer> foundProducers = findAllProducers(instance);
      for (Map.Entry<Class<?>, Producer> entry : foundProducers.entrySet()) {
        bus.installProducer(entry.getKey(), entry.getValue());
      }

      Map<Class<?>, Set<Subscriber>> foundSubscribers = findAllSubscribers(instance);
      for (Map.Entry<Class<?>, Set<Subscriber>> entry : foundSubscribers.entrySet()) {
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
