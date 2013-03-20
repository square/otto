package com.squareup.otto;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

public class ExecutorEventHandler extends EventHandler {

  private final Executor executor;

  ExecutorEventHandler(Object target, Method method, Executor executor) {
    super(target, method);
    this.executor = executor;
  }

  @Override
  protected boolean onTargetThread() {
    // always enqueue
    return false;
  }

  @Override
  protected void enqueue(Runnable runnable) {
    executor.execute(runnable);
  }

}
