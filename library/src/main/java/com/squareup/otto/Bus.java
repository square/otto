package com.squareup.otto;

/** A Bus is backed by the main thread. It is disabled at creation. */
public interface Bus {

  /**
   * Synchronously send event to all subscribers registered to any bus on this bus's tree.
   * Must be called on the main thread.
   */
  void post(Object event);

  /**
   * Like post, but may be called from any thread. The event will be dispatched on the  main
   * thread. When posting from the main thread, this is equivalent to {@link #post(Object)}.
   */
  void postOnMainThread(Object event);

  /** Must be called on the Bus's thread. */
  void register(Object subscriber);

  /**
   * Create a child Bus.  All subscribers to a child bus will receive events sent to
   * its ancestors.  Events posted to a child bus will not be sent to subscribers of
   * its ancestors.
   *
   * Must be called on the main thread.
   */
  Bus spawn();

  /** Permanently disable the bus and all descendents, dropping all references to subscribers. */
  void destroy();
}
