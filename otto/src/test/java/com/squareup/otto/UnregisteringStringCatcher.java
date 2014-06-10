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

import java.util.ArrayList;
import java.util.List;

/** An EventHandler mock that records a String and unregisters itself in the handler. */
public class UnregisteringStringCatcher {
  private final Bus bus;

  private List<String> events = new ArrayList<String>();

  public UnregisteringStringCatcher(Bus bus) {
    this.bus = bus;
  }

  @Subscribe public void unregisterOnString(String event) {
    bus.unregister(this);
    events.add(event);
  }

  @Subscribe public void zzzSleepinOnStrings(String event) {
    events.add(event);
  }

  @Subscribe public void haveAnInteger(Integer event) {}

  @Subscribe public void enjoyThisLong(Long event) {}

  @Subscribe public void perhapsATastyDouble(Double event) {}

  public List<String> getEvents() {
    return events;
  }
}
