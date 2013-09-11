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

/** An EventHandler mock that records a String and spawns a bus. */
public class BusSpawningPostingCatcher {
  static final Integer INTEGER_EVENT = 9000;

  private final Bus bus;
  private final IntegerCatcher catcher;

  private List<String> events = new ArrayList<String>();

  public BusSpawningPostingCatcher(Bus bus, IntegerCatcher catcher) {
    this.bus = bus;
    this.catcher = catcher;
  }

  @Subscribe public void spawnOnString(String event) {
    bus.spawn().register(catcher);
    events.add(event);
    bus.post(INTEGER_EVENT);
  }

  public List<String> getEvents() {
    return events;
  }

  static final class IntegerCatcher {
    List<Integer> events = new ArrayList<Integer>();
    @Subscribe public void onInteger(Integer event) {
      events.add(event);
      System.out.println(event);
    }
  }
}
