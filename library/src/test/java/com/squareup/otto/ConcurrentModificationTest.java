package com.squareup.otto;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

/**
 * Here we test registration / unregistration of handlers
 * (producers or subscribers) during event dispatching.
 * @author Sergej Shafarenka
 */
public class ConcurrentModificationTest {

	private Bus bus;

	@Before public void setUp() {
		bus = new Bus(ThreadEnforcer.SINGLE);
	}
	
	@Test public void testUnregistringSubscriberDuringEventDispatchingForOneSubscriber() {
		
		StringCatcher subscriber = new StringCatcher() {
			@Subscribe public void onEvent1(String event) {
				unregisterAndRecord(event);
			}
			@Subscribe public void onEvent2(String event) {
				unregisterAndRecord(event);
			}
			private void unregisterAndRecord(String event) {
				if (isFirstEvent()) {
					bus.unregister(this);
				}
				recordEvent(event);
			}
		};
		bus.register(subscriber);
		
		bus.post("event");
		
		subscriber.assertEventsCount(1);
		subscriber.assertLastEvent("event");
		
	}
	
	@Test public void testUnregistringSubscriberDuringEventDispatchingForMultipleSubscribers() {
		
		StringCatcher subscriber0 = new StringCatcher() {
			@Subscribe public void onEvent1(String event) {
				bus.unregister(this);
				recordEvent(event);
			}
			@Subscribe public void onEvent2(Integer event) {
				recordEvent("" + event);
			}
		};
		bus.register(subscriber0);
		
		StringCatcher subscriber1 = new StringCatcher() {
			@Subscribe public void onEvent1(String event) {
				bus.unregister(this);
				recordEvent(event);
			}
			@Subscribe public void onEvent2(Integer event) {
				recordEvent("" + event);
			}
		};
		bus.register(subscriber1);
		
		StringCatcher unregisteringSubscriber1 = new StringCatcher() {
			@Subscribe public void onEvent1(String event) {
				unregisterAndRecord(event);
			}
			@Subscribe public void onEvent2(String event) {
				unregisterAndRecord(event);
			}
			private void unregisterAndRecord(String event) {
				if (isFirstEvent()) {
					bus.unregister(this);
				}
				recordEvent(event);
			}
		};
		bus.register(unregisteringSubscriber1);
		
		StringCatcher unregisteringSubscriber2 = new StringCatcher() {
			@Subscribe public void onEvent1(String event) {
				if (getEvents().size() == 0) bus.unregister(this);
				recordEvent(event);
			}
			@Subscribe public void onEvent2(String event) {
				if (getEvents().size() == 0) bus.unregister(this);
				recordEvent(event);
			}
		};
		bus.register(unregisteringSubscriber2);
		
		StringCatcher subscriber2 = new StringCatcher() {
			@Subscribe public void onEvent1(String event) {
				recordEvent(event);
			}
			@Subscribe public void onEvent2(String event) {
				recordEvent(event);
			}
		};
		bus.register(subscriber2);
		
		StringCatcher subscriber3 = new StringCatcher() {
			@Subscribe public void onEvent1(String event) {
				recordEvent(event);
			}
			@Subscribe public void onEvent2(String event) {
				recordEvent(event);
			}
		};
		bus.register(subscriber3);
		
		
		bus.post("event");
		
		unregisteringSubscriber1.assertEventsCount(1);
		unregisteringSubscriber1.assertLastEvent("event");
		
		unregisteringSubscriber2.assertEventsCount(1);
		unregisteringSubscriber2.assertLastEvent("event");

		subscriber0.assertEventsCount(1);
		subscriber0.assertLastEvent("event");
		
		subscriber1.assertEventsCount(1);
		subscriber1.assertLastEvent("event");
		
		subscriber2.assertEventsCount(2);
		subscriber2.assertLastEvent("event");
		
		subscriber3.assertEventsCount(2);
		subscriber3.assertLastEvent("event");
		
	}
	
	@Test public void testCrossUnregistering() {
		
		final StringCatcher[] subscribers = new StringCatcher[3];
		
		subscribers[0] = new StringCatcher() {
			@Subscribe public void onEvent1(String event) {
				recordEvent(event);
			}
			@Subscribe public void onEvent2(String event) {
				recordEvent(event);
			}
		};
		
		subscribers[1] = new StringCatcher() {
			@Subscribe public void onEvent1(String event) {
				if (isFirstEvent()) {
					bus.unregister(subscribers[0]);
					bus.unregister(subscribers[2]);
				}
				recordEvent(event);
			}
			@Subscribe public void onEvent2(String event) {
				if (isFirstEvent()) {
					bus.unregister(subscribers[0]);
					bus.unregister(subscribers[2]);
				}
				recordEvent(event);
			}
		};
		
		subscribers[2] = new StringCatcher() {
			@Subscribe public void onEvent1(String event) {
				recordEvent(event);
			}
			@Subscribe public void onEvent2(String event) {
				recordEvent(event);
			}
			@Subscribe public void onEvent3(String event) {
				recordEvent(event);
			}
		};

		bus.register(subscribers[0]);
		bus.register(subscribers[1]);
		bus.register(subscribers[2]);
		
		bus.post("event");
		
		try {
			bus.unregister(subscribers[0]);
			Assert.fail("this subscriber must be unregistered already");
		} catch (Exception e) {
			// ok
		}
		
		bus.unregister(subscribers[1]);
		
		try {
			bus.unregister(subscribers[2]);
			Assert.fail("this subscriber must be unregistered already");
		} catch (Exception e) {
			// ok
		}
		
		subscribers[1].assertEventsCount(2);
	}
}
