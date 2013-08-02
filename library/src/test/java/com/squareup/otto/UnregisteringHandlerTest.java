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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

/** Test case for subscribers which unregister while handling an event. */
public class UnregisteringHandlerTest {

	private static final String EVENT = "Hello";
	private static final String BUS_IDENTIFIER = "test-bus";

	private Bus bus;

	@Before
	public void setUp() throws Exception {
		bus = new Bus(ThreadEnforcer.ANY, BUS_IDENTIFIER);
	}

	@Test
	public void unregisterInHandler() {
		UnregisteringStringCatcher catcher = new UnregisteringStringCatcher(bus);
		bus.register(catcher);
		bus.post(EVENT);

		assertEquals("One correct event should be delivered.", Arrays.asList(EVENT), catcher.getEvents());

		bus.post(EVENT);
		bus.post(EVENT);
		assertEquals("Shouldn't catch any more events when unregistered.", Arrays.asList(EVENT), catcher.getEvents());
	}

	@Test
	public void unregisterInHandlerWhenEventProduced() throws Exception {
		UnregisteringStringCatcher catcher = new UnregisteringStringCatcher(bus);

		bus.register(new StringProducer());
		bus.register(catcher);
		assertEquals(Arrays.asList(StringProducer.VALUE), catcher.getEvents());

		bus.post(EVENT);
		bus.post(EVENT);
		assertEquals("Shouldn't catch any more events when unregistered.", Arrays.asList(StringProducer.VALUE),
				catcher.getEvents());
	}

	@Test
	public void unregisterProducerInHandler() throws Exception {
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
		bus.register(new EventRecorder() {
			@Subscribe public void firstUnsubscribeTheProducer(String event) {
				unregisterOrFail(event);
			}

			@Subscribe public void shouldNeverBeCalled(String event) {
				unregisterOrFail(event);
			}

			private void unregisterOrFail(String event) {
				if (isFirstEvent()) {
					bus.unregister(producer);
					recordEvent(event);
				} else {
					fail("Shouldn't receive events from an unregistered producer.");
				}
			}
		});
	}

}
