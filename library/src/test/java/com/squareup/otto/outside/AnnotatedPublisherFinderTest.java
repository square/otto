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

package com.squareup.otto.outside;

import com.squareup.otto.Bus;
import com.squareup.otto.Publish;
import com.squareup.otto.Subscribe;
import com.squareup.otto.ThreadEnforcer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Test that Bus finds the correct publishers.
 *
 * This test must be outside the c.g.c.eventbus package to test correctly.
 *
 * @author Jake Wharton
 */
@SuppressWarnings("UnusedDeclaration")
public class AnnotatedPublisherFinderTest {

  static class Subscriber {
    final List<Object> events = new ArrayList<Object>();

    @Subscribe public void subscribe(Object o) {
      events.add(o);
    }
  }

  static class SimplePublisher {
    static final Object VALUE = new Object();

    int publishCalled = 0;

    @Publish public Object publishIt() {
      publishCalled += 1;
      return VALUE;
    }
  }

  @Test public void simplePublisher() {
    Bus bus = new Bus(ThreadEnforcer.NONE);
    Subscriber subscriber = new Subscriber();
    SimplePublisher publisher = new SimplePublisher();

    bus.register(publisher);
    assertThat(publisher.publishCalled).isEqualTo(0);
    bus.register(subscriber);
    assertThat(publisher.publishCalled).isEqualTo(1);
    assertEquals(Arrays.asList(SimplePublisher.VALUE), subscriber.events);
  }

  @Test public void multipleSubscriptionsCallsProviderEachTime() {
    Bus bus = new Bus(ThreadEnforcer.NONE);
    SimplePublisher producer = new SimplePublisher();

    bus.register(producer);
    bus.register(new Subscriber());
    assertThat(producer.publishCalled).isEqualTo(1);
    bus.register(new Subscriber());
    assertThat(producer.publishCalled).isEqualTo(2);
  }
}
