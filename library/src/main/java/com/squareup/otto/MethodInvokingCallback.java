package com.squareup.otto;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * <p>Extracted from EventHandler's original implementation</p>
 * <p>Wraps a Method in a Callback interface in order to be called whenever the EventHandler is posted an Event</p>
 * <p>This class only verifies the suitability of the method and event type if something fails. Callers are expected to
 * verify their uses of this class.</p>
 * <p>Two MethodInvokingCallback are equivalent when they refer to the same method on the same object (not class).
 * This property is used to ensure that no handler method is registered more than once.</p>
 *
 * @author Guillermo Gutierrez
 */
public class MethodInvokingCallback implements Callback {
  private final Object target;
  private final Method method;
  private final int hashCode;

  public MethodInvokingCallback(Object target, Method method) {
    method.setAccessible(true);
    this.target = target;
    this.method = method;
    // Compute hash code eagerly since we know it will be used frequently and we cannot estimate the runtime of the
    // target's hashCode call.
    final int prime = 31;
    hashCode = (prime + method.hashCode()) * prime + target.hashCode();
  }

  /**
   * <p>This method will be called whenever an Event is posted to the EventHandler that has an implementation of this
   * interface.</p>
   *
   * @param event event to handle
   * @throws java.lang.reflect.InvocationTargetException if the wrapped method throws any {@link Throwable} that is not
   *                                                     an {@link Error} ({@code Error}s are propagated as-is).
   */
  @Override
  public void call(Object event) throws InvocationTargetException {
    try {
      method.invoke(target, event);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof Error) {
        throw (Error) e.getCause();
      }
      throw e;
    }
  }

  public Method getMethod() {
    return method;
  }

  @Override
  public String toString() {
    return method.toString();
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    final MethodInvokingCallback other = (MethodInvokingCallback) obj;

    return method.equals(other.getMethod()) && target == other.target;
  }
}
