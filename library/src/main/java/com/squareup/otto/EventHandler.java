/*
 * Copyright (C) 2012 Square, Inc.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wraps a single-argument 'handler' method on a specific object.
 *
 * <p>This class only verifies the suitability of the method and event type if something fails.  Callers are expected t
 * verify their uses of this class.
 * <p/>
 * <p>Two EventHandlers are equivalent when they refer to the same method on the same object (not class).   This
 * property is used to ensure that no handler method is registered more than once.
 *
 * @author Cliff Biffle
 */
class EventHandler {

  /** Handler Callback. */
  private final Callback callback;
  /** Object hash code. */
  private final int hashCode;
  /** Should this handler receive events? */
  private boolean valid = true;

  EventHandler(Object target, Method method) {
    if (target == null) {
      throw new NullPointerException("EventHandler target cannot be null.");
    }
    if (method == null) {
      throw new NullPointerException("EventHandler method cannot be null.");
    }

    // Compute hash code eagerly since we know it will be used frequently and we cannot estimate the runtime of the
    // target's hashCode call.
    final int prime = 31;
    this.callback = new MethodInvokingCallback(target, method);
    this.hashCode = (prime + callback.hashCode()) * prime + target.hashCode();
  }

  EventHandler(final Callback callback) {
    if (callback == null) {
      throw new NullPointerException("EventHandler callback cannot be null.");
    }

    // Compute hash code eagerly since we know it will be used frequently and we cannot estimate the runtime of the
    // target's hashCode call.
    final int prime = 31;
    this.callback = callback;
    this.hashCode = (prime + callback.hashCode()) * prime;
  }

  public boolean isValid() {
    return valid;
  }

  /**
   * If invalidated, will subsequently refuse to handle events.
   *
   * Should be called when the wrapped object is unregistered from the Bus.
   */
  public void invalidate() {
    valid = false;
  }

  /**
   * Invokes the wrapped handler callback to handle {@code event}.
   *
   * @param event event to handle
   * @throws java.lang.reflect.InvocationTargetException if the wrapped callback throws any
   *                                                     {@link Throwable} that is not
   *                                                     an {@link Error} ({@code Error}s
   *                                                     are propagated as-is).
   */
  @SuppressWarnings("unchecked")
  public void handleEvent(Object event) throws InvocationTargetException {
    if (!valid) {
      throw new IllegalStateException(toString() + " has been invalidated and can no longer handle events.");
    }
    callback.call(event);
  }

  @Override public String toString() {
    return "[EventHandler " + callback.toString() + "]";
  }

  @Override public int hashCode() {
    return hashCode;
  }

  @Override public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    final EventHandler other = (EventHandler) obj;

    return callback.equals(other.callback);
  }


  public boolean hasCallback(Callback callback) {
    return this.callback.equals(callback);
  }
}
