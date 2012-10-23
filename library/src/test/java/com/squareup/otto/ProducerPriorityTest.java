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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Ensures that events from Producers are dispatched in order of descending priority.
 * 
 * @author Mark Renouf
 */
public class ProducerPriorityTest {

  private Bus bus;
  private SubscriberClass subscriber;
  private PriorityProducerClass1 producer1;
  private PriorityProducerClass2 producer2;
  
  @Before public void setUp() throws Exception {
	bus = new Bus(ThreadEnforcer.ANY);
	subscriber = new SubscriberClass();
	producer1 = new PriorityProducerClass1();
	producer2 = new PriorityProducerClass2();
  }

  /**
   * Checks that events dispatched in priority order when their Producing class 
   * is registered.
   */
  @Test public void eventPriorityOnProducerRegistration() throws Exception {
    bus.register(subscriber);

    List<Number> expected = new ArrayList<Number>();
    expected.add(Float.valueOf(2));
    expected.add(Long.valueOf(3));
    expected.add(Integer.valueOf(1));

    bus.register(producer1);
    assertEquals("Producers should be invoked in order of their priority.", 
    	expected, subscriber.received);
    bus.unregister(producer1);
    
    subscriber.received.clear();
    expected.clear();
    
    bus.register(producer2);
    expected.add(Integer.valueOf(1));
    expected.add(Float.valueOf(2));
    expected.add(Long.valueOf(3));
    assertEquals("Producers should be invoked in order of their priority.", 
    	expected, subscriber.received);
  }

  /**
   * Checks that events dispatched in priority order when a Subscribing class 
   * is registered.
   */
  @Test public void eventPriorityOnSubscriberRegistration() throws Exception {
    Bus bus = new Bus(ThreadEnforcer.ANY);
    bus.register(producer1);

    bus.register(subscriber);
    List<Number> expected = new ArrayList<Number>();
    expected.add(Float.valueOf(2));
    expected.add(Long.valueOf(3));
    expected.add(Integer.valueOf(1));
    assertEquals("Producers should be invoked in order of their priority.", 
    	expected, subscriber.received);
    expected.clear();
    bus.unregister(subscriber);

    bus.unregister(producer1);
    subscriber.clear();
    bus.register(producer2);
    
    bus.register(subscriber);
    expected.add(Integer.valueOf(1));
    expected.add(Float.valueOf(2));
    expected.add(Long.valueOf(3));
    assertEquals("Producers should be invoked in order of their priority.", 
    	expected, subscriber.received);
  }

  public static class PriorityProducerClass1 {
	@Produce(priority=1)
	public Float getFloat() {
	  return Float.valueOf(2);
	}
	  
    @Produce(priority=2)
    public Long getLong() {
      return Long.valueOf(3);
    }

    @Produce(priority=3)
    public Integer getInteger() {
    	return Integer.valueOf(1);
    }
  }
  
  public static class PriorityProducerClass2 {
    @Produce(priority=1)
    public Integer getInteger() {
      return Integer.valueOf(1);
    }

    @Produce(priority=2)
    public Float getFloat() {
      return Float.valueOf(2);
    }
    
    @Produce(priority=3)
    public Long getLong() {
      return Long.valueOf(3);
    }
  }
  
  public static class SubscriberClass {
    List<Number> received = new ArrayList<Number>();
	
    @Subscribe
    public void receiveLong(Long value) {
      received.add(value);
    }
    
    @Subscribe
    public void receiveFloat(Float value) {
      received.add(value);
    }

    @Subscribe
    public void receiveInteger(Integer value) {
      received.add(value);
    }
    
    public void clear() {
      received.clear();
    }
  }
}
