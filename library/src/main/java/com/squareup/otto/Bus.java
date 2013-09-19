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
   * Like post, but may be called from any thread. The event will be dispatched on the main thread.
   * When posting from the main thread, this is equivalent to {@link #post(Object)}.
   */
  void postOnMainThread(Object event);

  /** Must be called on the Bus's thread. */
  void register(Object subscriber);

  /**
   * Create a child Bus.  All subscribers to a child bus will receive events sent to its ancestors.
   * Events posted to a child bus will not be sent to subscribers of its ancestors.
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
