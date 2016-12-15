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

import java.lang.Thread;

/**
 * Enforces a thread confinement policy for methods on a particular event bus.
 *
 * @author Jake Wharton
 */
public interface ThreadEnforcer {

  /**
   * Enforce a valid thread for the given {@code bus}. Implementations may throw any runtime exception.
   *
   * @param bus Event bus instance on which an action is being performed.
   */
  void enforce(Bus bus);


  /** A {@link ThreadEnforcer} that does no verification. */
  ThreadEnforcer ANY = new ThreadEnforcer() {
    @Override public void enforce(Bus bus) {
      // Allow any thread.
    }
  };

  /** A {@link ThreadEnforcer} that confines {@link Bus} methods to the main thread. */
  ThreadEnforcer MAIN = new ThreadEnforcer() {

    long id = Thread.currentThread().getId();

    @Override public void enforce(Bus bus) {
      Thread thread = Thread.currentThread(); if (id != thread.getId()) {
        String name = "Looper (" + thread.getName() + ", tid " + thread.getId() + ") {" + Integer.toHexString(System.identityHashCode(thread)) + "}";
        throw new IllegalStateException("Event bus " + bus + " accessed from non-main thread " + name);
      }
    }
  };

}
