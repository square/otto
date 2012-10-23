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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wraps a 'producer' method on a specific object.
 *
 * <p> This class only verifies the suitability of the method and event type if something fails.  Callers are expected
 * to verify their uses of this class.
 *
 * @author Jake Wharton
 */
class EventProducer implements Comparable<EventProducer> {

  /** Object sporting the producer method. */
  private final Object target;
  /** Producer method. */
  private final Method method;
  /** Event priority. */
  private final int priority;
  /** Object hash code. */
  private final int hashCode;

  EventProducer(Object target, Method method, int priority) {
    if (target == null) {
      throw new NullPointerException("EventProducer target cannot be null.");
    }
    if (method == null) {
      throw new NullPointerException("EventProducer method cannot be null.");
    }

    this.target = target;
    this.method = method;
    this.priority = priority;
    method.setAccessible(true);

    // Compute hash code eagerly since we know it will be used frequently and we cannot estimate the runtime of the
    // target's hashCode call.
    final int prime = 31;
    hashCode = (prime + method.hashCode()) * prime + target.hashCode();
  }

  /**
   * Invokes the wrapped producer method.
   *
   * @throws java.lang.reflect.InvocationTargetException  if the wrapped method throws any {@link Throwable} that is not
   *     an {@link Error} ({@code Error}s are propagated as-is).
   */
  public Object produceEvent() throws InvocationTargetException {
    try {
      return method.invoke(target);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof Error) {
        throw (Error) e.getCause();
      }
      throw e;
    }
  }

  @Override public String toString() {
    return "[EventProducer " + method + "]";
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

    final EventProducer other = (EventProducer) obj;

    return method.equals(other.method) && target == other.target;
  }

  /**
   * Natural ordering by priority, ascending, then by hashCode.
   */
  @Override
  public int compareTo(EventProducer o) {
    if (priority == o.priority) {
      return hashCode > o.hashCode ? 1 : hashCode < o.hashCode ? -1 : 0;
    }
    return priority < o.priority ? -1 : priority > o.priority ? 1 : 0;
  }
}
