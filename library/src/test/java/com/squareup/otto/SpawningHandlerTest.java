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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.squareup.otto.BusSpawningPostingCatcher.IntegerCatcher;
import static com.squareup.otto.DeadEventHandler.IGNORE_DEAD_EVENTS;
import static org.fest.assertions.api.Assertions.assertThat;

/** Test case for subscribers which spawn a new child bus while handling an event. */
@RunWith(RobolectricTestRunner.class) @Config(manifest = Config.NONE)
public class SpawningHandlerTest {

  private static final String EVENT = "Hello";
  private Bus bus;

  @Before
  public void setUp() throws Exception {
    bus = new OttoBus(IGNORE_DEAD_EVENTS);
  }

  @Test public void spawnBusInHandler() throws Exception {
    IntegerCatcher integerCatcher = new IntegerCatcher();
    BusSpawningPostingCatcher catcher = new BusSpawningPostingCatcher(bus, integerCatcher);
    bus.spawn().register(catcher);
    bus.spawn();
    bus.post(EVENT);

    assertThat(catcher.getEvents()).as("One correct event should be delivered.")
        .containsExactly(EVENT);

    assertThat(integerCatcher.events).as("One correct event should be delivered.")
        .containsExactly(BusSpawningPostingCatcher.INTEGER_EVENT);
  }
}
