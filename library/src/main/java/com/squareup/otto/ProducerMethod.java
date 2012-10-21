package com.squareup.otto;

import java.lang.reflect.Method;

public class ProducerMethod {
  public final int priority;
  public final Method method;

  public ProducerMethod(Method method, int priority) {
    this.method = method;
    this.priority = priority;
  }

  @Override
  public int hashCode() {
    return method.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return method.equals(obj);	
  }
}
