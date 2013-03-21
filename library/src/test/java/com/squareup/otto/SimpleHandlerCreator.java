package com.squareup.otto;

import java.lang.reflect.Method;

import com.squareup.otto.Subscribe.ExecuteOn;

public class SimpleHandlerCreator extends EventHandlerCreator {
  @Override
  public EventHandler createHandler(ExecuteOn thread, Object target,
      Method method) {
    return new EventHandler(target, method);
  }
}
