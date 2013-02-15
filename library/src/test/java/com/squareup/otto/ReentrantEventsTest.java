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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Validate that {@link Bus} behaves carefully when listeners publish
 * their own events.
 *
 * @author Jesse Wilson
 */
public class ReentrantEventsTest {

  static final String FIRST = "one";
  static final Double SECOND = 2.0d;

  final Bus bus = new Bus(ThreadEnforcer.ANY);

  @Test public void noReentrantEvents() {
    ReentrantEventsHater hater = new ReentrantEventsHater();
    bus.register(hater);

    bus.post(FIRST);

    assertEquals("ReentrantEventHater expected 2 events",
        Arrays.<Object>asList(FIRST, SECOND), hater.eventsReceived);
  }

  public class ReentrantEventsHater {
    boolean ready = true;
    List<Object> eventsReceived = new ArrayList<Object>();

    @Subscribe
    public void listenForStrings(String event) {
      eventsReceived.add(event);
      ready = false;
      try {
        bus.post(SECOND);
      } finally {
        ready = true;
      }
    }

    @Subscribe
    public void listenForDoubles(Double event) {
      assertTrue("I received an event when I wasn't ready!", ready);
      eventsReceived.add(event);
    }
  }

  @Test public void eventOrderingIsPredictable() {
    EventProcessor processor = new EventProcessor();
    bus.register(processor);

    EventRecorder recorder = new EventRecorder();
    bus.register(recorder);

    bus.post(FIRST);

    assertEquals("EventRecorder expected events in order",
        Arrays.<Object>asList(FIRST, SECOND), recorder.eventsReceived);
  }

  public class EventProcessor {
    @Subscribe public void listenForStrings(String event) {
      bus.post(SECOND);
    }
  }

  public class EventRecorder {
    List<Object> eventsReceived = new ArrayList<Object>();
    @Subscribe public void listenForEverything(Object event) {
      eventsReceived.add(event);
    }
  }
}
