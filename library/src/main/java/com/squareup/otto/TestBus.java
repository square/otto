package com.squareup.otto;

/**
 * A Bus implementation which always assumes it has been called on the "main thread" and does not
 * provide enforcement or coercion.  Use only in tests, and with care.
 */
public final class TestBus implements Bus {

  private final OttoBus delegate;

  public TestBus(DeadEventHandler deadEventHandler)  {
    OttoBus.MainThread thread = new OttoBus.MainThread() {
      @Override public void enforce() {
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
