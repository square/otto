/*
 * Copyright (C) 2013 Square, Inc.
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

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class) @Config(manifest = Config.NONE)
public class DeadEventTest {
  private static final String EVENT = "Hello";

  DeadEventCatcher catcher;
  Bus bus;

  @Before public void setUp() throws Exception {
    catcher = new DeadEventCatcher();
    bus = new OttoBus(catcher);
  }

  @Test public void eventDiesIfNoSubscribers() {
    // A String -- an event for which no-one has registered.
    bus.post(EVENT);
    assertThat(catcher.getEvents()).containsExactly(EVENT);
  }

  @Test public void eventDoesntDieIfChildHasSubscribers() {
    bus.spawn().register(new StringCatcher());
    bus.post(EVENT);
    assertThat(catcher.getEvents()).isEmpty();
  }

  public static class DeadEventCatcher implements DeadEventHandler {
    private List<Object> events = new ArrayList<Object>();

    public List<Object> getEvents() {
      return events;
    }

    @Override public void onDeadEvent(Object event) {
      events.add(event);
    }
  }
}
