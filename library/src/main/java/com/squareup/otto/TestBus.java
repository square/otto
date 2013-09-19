/*
 * Copyright (C) 2013 Square, Inc.
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
 * A Bus implementation which always assumes it has been called on the "main thread" and does not
 * provide enforcement or coercion.  Use only in tests, and with care.
 */
public final class TestBus implements Bus {

  private final OttoBus delegate;

  public TestBus(DeadEventHandler deadEventHandler) {
    OttoBus.MainThread thread = new OttoBus.MainThread() {
      @Override public void enforce() {
      }

      @Override public void forbid() {
      }

      @Override public void post(Object event) {
        delegate.post(event);
      }
    };
    delegate = new OttoBus(thread, HandlerFinder.ANNOTATED, deadEventHandler);
  }

  @Override public void post(Object event) {
    delegate.post(event);
  }

  @Override public void postOnMainThread(Object event) {
    delegate.postOnMainThread(event);
  }

  @Override public void register(Object subscriber) {
    delegate.register(subscriber);
  }

  @Override public Bus spawn() {
    return delegate.spawn();
  }

  @Override public void destroy() {
    delegate.destroy();
  }
}
