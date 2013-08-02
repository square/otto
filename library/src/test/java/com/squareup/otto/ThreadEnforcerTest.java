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

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;

public class ThreadEnforcerTest {

  private static class RecordingThreadEnforcer implements ThreadEnforcer {
    boolean called = false;

    @Override public void enforce(Bus bus) {
      called = true;
    }
  }

  @Test public void enforerCalledForRegister() {
    RecordingThreadEnforcer enforcer = new RecordingThreadEnforcer();
    Bus bus = new Bus(enforcer);

    assertFalse(enforcer.called);
    bus.register(this);
    assertTrue(enforcer.called);
  }

  @Test public void enforcerCalledForPost() {
    RecordingThreadEnforcer enforcer = new RecordingThreadEnforcer();
    Bus bus = new Bus(enforcer);

    assertFalse(enforcer.called);
    bus.post(this);
    assertTrue(enforcer.called);
  }

  @Test public void enforcerCalledForUnregister() {
    RecordingThreadEnforcer enforcer = new RecordingThreadEnforcer();
    Bus bus = new Bus(enforcer);

    assertFalse(enforcer.called);
    bus.unregister(this);
    assertTrue(enforcer.called);
  }

	@Test public void testSingleThreadEnforcer() throws InterruptedException {

		final Bus bus = new Bus();
		final ThreadEnforcer e = ThreadEnforcer.SINGLE;
		final Exception[] expected = new Exception[1];

		e.enforce(bus);

		Thread thread = new Thread(new Runnable() {
			@Override public void run() {
				try {
					e.enforce(bus);
				} catch (Exception e) {
					expected[0] = e;
				}
			}
		});
		thread.start();
		thread.join();
		
		assertNotNull(expected[0]);
	}
  
}
