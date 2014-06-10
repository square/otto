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

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class EventProducerTest {

  private static final Object FIXTURE_RETURN_VALUE = new Object();

  private boolean methodCalled;
  private Object methodReturnValue;

  @Before public void setUp() throws Exception {
    methodCalled = false;
    methodReturnValue = FIXTURE_RETURN_VALUE;
  }

  /**
   * Checks that a no-frills, no-issues method call is properly executed.
   *
   * @throws Exception  if the aforementioned proper execution is not to be had.
   */
  @Test public void basicMethodCall() throws Exception {
    Method method = getRecordingMethod();
    EventProducer producer = new EventProducer(this, method);
    Object methodResult = producer.produceEvent();

    assertTrue("Producer must call provided method.", methodCalled);
    assertSame("Producer result must be *exactly* the specified return value.", methodResult, FIXTURE_RETURN_VALUE);
  }

  /** Checks that EventProducer's constructor disallows null methods. */
  @Test public void rejectionOfNullMethods() {
    try {
      new EventProducer(this, null);
      fail("EventProducer must immediately reject null methods.");
    } catch (NullPointerException expected) {
      // Hooray!
    }
  }

  /** Checks that EventProducer's constructor disallows null targets. */
  @Test public void rejectionOfNullTargets() throws NoSuchMethodException {
    Method method = getRecordingMethod();
    try {
      new EventProducer(null, method);
      fail("EventProducer must immediately reject null targets.");
    } catch (NullPointerException expected) {
      // Huzzah!
    }
  }

  @Test public void testExceptionWrapping() throws NoSuchMethodException {
    Method method = getExceptionThrowingMethod();
    EventProducer producer = new EventProducer(this, method);

    try {
      producer.produceEvent();
      fail("Producers whose methods throw must throw InvocationTargetException");
    } catch (InvocationTargetException e) {
      assertTrue("Expected exception must be wrapped.",
          e.getCause() instanceof IntentionalException);
    }
  }

  @Test public void errorPassthrough() throws InvocationTargetException, NoSuchMethodException {
    Method method = getErrorThrowingMethod();
    EventProducer producer = new EventProducer(this, method);

    try {
      producer.produceEvent();
      fail("Producers whose methods throw Errors must rethrow them");
    } catch (JudgmentError expected) {
      // Expected.
    }
  }

  @Test public void returnValueNotCached() throws Exception {
    Method method = getRecordingMethod();
    EventProducer producer = new EventProducer(this, method);
    producer.produceEvent();
    methodReturnValue = new Object();
    methodCalled = false;
    Object secondReturnValue = producer.produceEvent();

    assertTrue("Producer must call provided method twice.", methodCalled);
    assertSame("Producer result must be *exactly* the specified return value on each invocation.",
        secondReturnValue, methodReturnValue);
  }

  private Method getRecordingMethod() throws NoSuchMethodException {
    return getClass().getMethod("recordingMethod");
  }

  private Method getExceptionThrowingMethod() throws NoSuchMethodException {
    return getClass().getMethod("exceptionThrowingMethod");
  }

  private Method getErrorThrowingMethod() throws NoSuchMethodException {
    return getClass().getMethod("errorThrowingMethod");
  }

  /**
   * Records the invocation in {@link #methodCalled} and returns the value in
   * {@link #FIXTURE_RETURN_VALUE}.
   */
  public Object recordingMethod() {
    if (methodCalled) {
      throw new IllegalStateException("Method called more than once.");
    }
    methodCalled = true;
    return methodReturnValue;
  }

  public Object exceptionThrowingMethod() throws Exception {
    throw new IntentionalException();
  }

  /** Local exception subclass to check variety of exception thrown. */
  static class IntentionalException extends Exception {
    private static final long serialVersionUID = -2500191180248181379L;
  }

  public Object errorThrowingMethod() {
    throw new JudgmentError();
  }

  /** Local Error subclass to check variety of error thrown. */
  static class JudgmentError extends Error {
    private static final long serialVersionUID = 634248373797713373L;
  }
}
