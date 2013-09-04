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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.squareup.otto.DeadEventHandler.IGNORE_DEAD_EVENTS;
import static org.fest.assertions.api.Assertions.assertThat;

/** Test case for subscribers which destroy their bus while handling an event. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DestroyingHandlerTest {

  private static final String EVENT = "Hello";
  private Bus bus;

  @Before
  public void setUp() throws Exception {
    bus = Shuttle.createTestBus(new SortedHandlerFinder(), IGNORE_DEAD_EVENTS);
  }

  @Test public void destroyBusInHandler() throws Exception {
    Bus childBus = bus.spawn();
    BusDestroyingStringCatcher catcher = new BusDestroyingStringCatcher(childBus);
    childBus.register(catcher);
    bus.post(EVENT);

    assertThat(catcher.getEvents()).as("One correct event should be delivered.")
        .containsExactly(EVENT);

    bus.post(EVENT);
    bus.post(EVENT);

    assertThat(catcher.getEvents()).as("Shouldn't catch any more events after bus is destroyed")
        .containsExactly(EVENT);
  }

  /**
   * Delegates to {@code HandlerFinder.ANNOTATED}, then sorts results by {@code
   * EventHandler#toString}
   */
  static class SortedHandlerFinder implements HandlerFinder {

    static Comparator<EventHandler> handlerComparator = new Comparator<EventHandler>() {
      @Override
      public int compare(EventHandler eventHandler, EventHandler eventHandler1) {
        return eventHandler.toString().compareTo(eventHandler1.toString());
      }
    };

    @Override
    public Map<Class<?>, Set<EventHandler>> findAllSubscribers(Object listener) {
      Map<Class<?>, Set<EventHandler>> found = HandlerFinder.ANNOTATED.findAllSubscribers(listener);
      Map<Class<?>, Set<EventHandler>> sorted = new HashMap<Class<?>, Set<EventHandler>>();
      for (Map.Entry<Class<?>, Set<EventHandler>> entry : found.entrySet()) {
        SortedSet<EventHandler> handlers = new TreeSet<EventHandler>(handlerComparator);
        handlers.addAll(entry.getValue());
        sorted.put(entry.getKey(), handlers);
      }
      return sorted;
    }
  }
}
