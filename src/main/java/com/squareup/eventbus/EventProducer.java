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

package com.squareup.eventbus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wraps a 'producer' method on a specific object.
 *
 * <p>This class only verifies the suitability of the method and event type if
 * something fails.  Callers are expected to verify their uses of this class.
 *
 * @author Jake Wharton
 */
class EventProducer {

  /** Object sporting the producer method. */
  private final Object target;
  /** Producer method. */
  private final Method method;

  EventProducer(Object target, Method method) {
    if (target == null) {
      throw new NullPointerException("EventProducer target cannot be null.");
    }
    if (method == null) {
      throw new NullPointerException("EventProducer method cannot be null.");
    }

    this.target = target;
    this.method = method;
    method.setAccessible(true);
  }

  /**
   * Invokes the wrapped producer method.
   *
   * @throws java.lang.reflect.InvocationTargetException  if the wrapped method throws any
   *     {@link Throwable} that is not an {@link Error} ({@code Error}s are
   *     propagated as-is).
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

}
