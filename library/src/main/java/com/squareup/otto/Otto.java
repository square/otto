package com.squareup.otto;

/** Factories for creating a {@link com.squareup.otto.Bus}. */
public final class Otto {
  private Otto() {
  }

  /** Create a bus which silently drops dead events. */
  public static Bus createBus() {
    return new OttoBus(HandlerFinder.ANNOTATED, DeadEventHandler.IGNORE_DEAD_EVENTS);
  }

  /** Create a bus that delivers dead events to the supplied handler. */
  public static Bus createBus(DeadEventHandler deadEventHandler) {
    return new OttoBus(HandlerFinder.ANNOTATED, deadEventHandler);
  }
}
