/* Copyright (C) 2012 Square, Inc.
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

/**
 * An {@link EventHandler} that invokes a method with a
 * given @{link {@link java.util.concurrent.Executor}.
 * <p>A method invocation is executed with
 * {@link java.util.concurrent.Executor#execute(Runnable)}.</p>
 *
 * @author James Hugman
 */
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

public class EventHandlerWithExecutor extends EventHandler {

  private final Executor executor;

  EventHandlerWithExecutor(Object target, Method method, Executor executor) {
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
