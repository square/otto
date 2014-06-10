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
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;
import com.squareup.otto.ThreadEnforcer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Test that Bus finds the correct producers.
 *
 * This test must be outside the c.g.c.eventbus package to test correctly.
 *
 * @author Jake Wharton
 */
@SuppressWarnings("UnusedDeclaration")
public class AnnotatedProducerFinderTest {

  static class Subscriber {
    final List<Object> events = new ArrayList<Object>();

    @Subscribe public void subscribe(Object o) {
      events.add(o);
    }
  }

  static class SimpleProducer {
    static final Object VALUE = new Object();

    int produceCalled = 0;

    @Produce public Object produceIt() {
      produceCalled += 1;
      return VALUE;
    }
  }

  @Test public void simpleProducer() {
    Bus bus = new Bus(ThreadEnforcer.ANY);
    Subscriber subscriber = new Subscriber();
    SimpleProducer producer = new SimpleProducer();

    bus.register(producer);
    assertThat(producer.produceCalled).isEqualTo(0);
    bus.register(subscriber);
    assertThat(producer.produceCalled).isEqualTo(1);
    assertEquals(Arrays.asList(SimpleProducer.VALUE), subscriber.events);
  }

  @Test public void multipleSubscriptionsCallsProviderEachTime() {
    Bus bus = new Bus(ThreadEnforcer.ANY);
    SimpleProducer producer = new SimpleProducer();

    bus.register(producer);
    bus.register(new Subscriber());
    assertThat(producer.produceCalled).isEqualTo(1);
    bus.register(new Subscriber());
    assertThat(producer.produceCalled).isEqualTo(2);
  }
}
