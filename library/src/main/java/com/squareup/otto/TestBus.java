package com.squareup.otto;

/**
 * A Bus implementation which always assumes it has been called on the "main thread" and does not
 * provide enforcement or coercion.  Use only in tests, and with care.
 */
public final class TestBus implements Bus {

  private final OttoBus delegate;

  private static final OttoBus.MainThread TEST_THREAD = new OttoBus.MainThread() {
    private OttoBus bus;

    @Override public void setBus(OttoBus bus) {
      this.bus = bus;
    }

    @Override public void enforce() {
    }

    @Override public void post(Object event) {
      bus.post(event);
    }
  };

  public TestBus(DeadEventHandler deadEventHandler)  {
    delegate = new OttoBus(TEST_THREAD, HandlerFinder.ANNOTATED, deadEventHandler);
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
