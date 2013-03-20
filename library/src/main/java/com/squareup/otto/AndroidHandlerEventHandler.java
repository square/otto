package com.squareup.otto;

import java.lang.reflect.Method;

import android.os.Handler;
import android.os.Looper;

public class AndroidHandlerEventHandler extends EventHandler {

  private final Handler handler;

  AndroidHandlerEventHandler(Object target, Method method, Looper looper) {
    this(target, method, new Handler(looper));
  }

  AndroidHandlerEventHandler(Object target, Method method, Handler handler) {
    super(target, method);
    this.handler = handler;
  }

  @Override
  protected boolean onTargetThread() {
    return handler.getLooper() == Looper.myLooper();
  }

  @Override
  protected void enqueue(Runnable runnable) {
    handler.post(runnable);
  }
}
