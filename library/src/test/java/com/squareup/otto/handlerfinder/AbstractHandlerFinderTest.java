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

package com.squareup.otto.handlerfinder;

import com.squareup.otto.Bus;
import com.squareup.otto.Otto;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

import static com.squareup.otto.DeadEventHandler.IGNORE_DEAD_EVENTS;

@Ignore // Tests are in extending classes.
public abstract class AbstractHandlerFinderTest<H> {
  static final Object EVENT = new Object();
  private H handler;

  abstract H createHandler();

  H getHandler() {
    return handler;
  }

  @Before
  public void setUp() throws Exception {
    handler = createHandler();
    Bus bus = Otto.createBus(IGNORE_DEAD_EVENTS);
    bus.register(handler);
    bus.post(EVENT);
  }

  @After
  public void tearDown() throws Exception {
    handler = null;
  }
}
