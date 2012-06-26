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

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class EventHandlerTest {

  private static final Object FIXTURE_ARGUMENT = new Object();

  private boolean methodCalled;
  private Object methodArgument;

  @Before public void setUp() throws Exception {
    methodCalled = false;
    methodArgument = null;
  }

  /**
   * Checks that a no-frills, no-issues method call is properly executed.
   *
   * @throws Exception  if the aforementioned proper execution is not to be had.
   */
  @Test public void basicMethodCall() throws Exception {
    Method method = getRecordingMethod();

    EventHandler handler = new EventHandler(this, method);

    handler.handleEvent(FIXTURE_ARGUMENT);

    assertTrue("Handler must call provided method.", methodCalled);
    assertSame("Handler argument must be *exactly* the provided object.",
        methodArgument, FIXTURE_ARGUMENT);
  }

  /** Checks that EventHandler's constructor disallows null methods. */
  @Test public void rejectionOfNullMethods() {
    try {
      new EventHandler(this, null);
      fail("EventHandler must immediately reject null methods.");
    } catch (NullPointerException expected) {
      // Hooray!
    }
  }

  /** Checks that EventHandler's constructor disallows null targets. */
  @Test public void rejectionOfNullTargets() throws NoSuchMethodException {
    Method method = getRecordingMethod();
    try {
      new EventHandler(null, method);
      fail("EventHandler must immediately reject null targets.");
    } catch (NullPointerException expected) {
      // Huzzah!
    }
  }

  @Test public void exceptionWrapping() throws NoSuchMethodException {
    Method method = getExceptionThrowingMethod();
    EventHandler handler = new EventHandler(this, method);

    try {
      handler.handleEvent(new Object());
      fail("Handlers whose methods throw must throw InvocationTargetException");
    } catch (InvocationTargetException e) {
      assertTrue("Expected exception must be wrapped.",
          e.getCause() instanceof IntentionalException);
    }
  }

  @Test public void errorPassthrough() throws InvocationTargetException, NoSuchMethodException {
    Method method = getErrorThrowingMethod();
    EventHandler handler = new EventHandler(this, method);

    try {
      handler.handleEvent(new Object());
      fail("Handlers whose methods throw Errors must rethrow them");
    } catch (JudgmentError expected) {
      // Expected.
    }
  }

  private Method getRecordingMethod() throws NoSuchMethodException {
    return getClass().getMethod("recordingMethod", Object.class);
  }

  private Method getExceptionThrowingMethod() throws NoSuchMethodException {
    return getClass().getMethod("exceptionThrowingMethod", Object.class);
  }

  private Method getErrorThrowingMethod() throws NoSuchMethodException {
    return getClass().getMethod("errorThrowingMethod", Object.class);
  }

  /**
   * Records the provided object in {@link #methodArgument} and sets
   * {@link #methodCalled}.
   *
   * @param arg  argument to record.
   */
  public void recordingMethod(Object arg) {
    if (methodCalled) {
      throw new IllegalStateException("Method called more than once.");
    }
    methodCalled = true;
    methodArgument = arg;
  }

  public void exceptionThrowingMethod(Object arg) throws Exception {
    throw new IntentionalException();
  }

  /** Local exception subclass to check variety of exception thrown. */
  static class IntentionalException extends Exception {
    private static final long serialVersionUID = -2500191180248181379L;
  }

  public void errorThrowingMethod(Object arg) {
    throw new JudgmentError();
  }

  /** Local Error subclass to check variety of error thrown. */
  static class JudgmentError extends Error {
    private static final long serialVersionUID = 634248373797713373L;
  }
}
