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

package com.squareup.otto;

/** A Bus is backed by the main thread. It is disabled at creation. */
public interface Bus {

  /**
   * Synchronously send event to all subscribers registered to any bus on this bus's tree.
   * Must be called on the main thread.
   */
  void post(Object event);

  /**
   * Like post, but must be called from a thread other than the main thread. The event will be
   * dispatched on the main thread.
   */
  void postOnMainThread(Object event);

  /** Must be called on the Bus's thread. */
  void register(Object subscriber);

  /**
   * Create a child Bus. Children provide no scoping with respect to posting events:
   * any event posted to any undestroyed bus in the tree will be processed by all
   * undestroyed buses. Children are useful because they are the only way to unregister
   * subscribers.
   *
   * Must be called on the main thread.
   */
  Bus spawn();

  /**
   * Permanently disable the bus and all descendants, dropping all references to subscribers.
   * Redundant calls are allowed.
   */
  void destroy();
}
