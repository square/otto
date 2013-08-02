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

package com.squareup.otto.outside;

import com.squareup.otto.Bus;
import com.squareup.otto.Shuttle;
import com.squareup.otto.StringCatcher;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Test cases for {@code Bus} that must not be in the same package.
 *
 * @author Louis Wasserman
 */
public class OutsideEventBusTest {
  private static final String EVENT = "Hello";

  Bus bus;

  @Before public void setUp() {
    bus = new Shuttle();
  }

  /*
   * If you do this test from common.eventbus.BusTest, it doesn't actually test the behavior.
   * That is, even if exactly the same method works from inside the common.eventbus package tests,
   * it can fail here.
   */
  @Test public void subscriberReceivesPostedEvent() {
    StringCatcher catcher = new StringCatcher();
    bus.register(catcher);
    bus.post(EVENT);
    assertThat(catcher.getEvents()).as("Subscriber should receive posted event.")
        .containsExactly(EVENT);
  }

  @Test public void subscriberOnlyReceivesEventsForType() {
    StringCatcher catcher = new StringCatcher();
    bus.register(catcher);
    bus.post(new Object());
    assertThat(catcher.getEvents()).as("Subscriber should not receive event of wrong type.")
        .isEmpty();
  }

  @Test public void onlySubscriberOfCorrectTypeReceivesEvent() {
    StringCatcher catcher = new StringCatcher();
    IntegerCatcher intCatcher = new IntegerCatcher();
    bus.register(catcher);
    bus.post(EVENT);
    assertThat(intCatcher.getEvents()).as("Subscriber should not receive event of wrong type.")
        .isEmpty();
    assertThat(catcher.getEvents()).as("Subscriber of matching type should receive posted event.")
        .containsExactly(EVENT);
  }
}
