package com.squareup.otto;

/** A Bus is backed by a single thread.  It is disabled at creation. */
public interface Bus {

  /**
   * Synchronously send event to all subscribers registered on this Bus and its children.
   * Must be called from the thread on which the Bus was created.
   */
  void post(Object event);

  /**
   * Like post, but may be called from any thread.  The event will be dispatched on the Bus's
   * thread.  When posting from the Bus's thread, this is equivalent to {@link #post(Object)}.
   */
  void postOnBusThread(Object event);

  /** Must be called on the Bus's thread. */
  void register(Object subscriber);

  /**
   * Create a child Bus.  All subscribers to a child bus will receive events sent to
   * its ancestors.  Events posted to a child bus will not be sent to subscribers of
   * its ancestors.
   *
   * Must be called on the Bus's thread.  Child will be backed by the same thread.
   */
  Bus spawn();

  /** Permanently disable the bus and all descendents, dropping all references to subscribers. */
  void destroy();

  /** Turn on dispatch of events on this bus and all descendents. */
  void enable();

  /** Turn off dispatch of events on this bus and all descendents. */
  void disable();
}
