/*
 * Copyright (C) 2007 The Guava Authors
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

/**
 * Wraps an event that was posted, but which had no subscribers and thus could not be delivered.
 *
 * <p>Subscribing a DeadEvent handler is useful for debugging or logging, as it can detect misconfigurations in a
 * system's event distribution.
 *
 * @author Cliff Biffle
 */
public class DeadEvent {

  public final Object source;
  public final Object event;

  /**
   * Creates a new DeadEvent.
   *
   * @param source object broadcasting the DeadEvent (generally the {@link Bus}).
   * @param event the event that could not be delivered.
   */
  public DeadEvent(Object source, Object event) {
    this.source = source;
    this.event = event;
  }

}
