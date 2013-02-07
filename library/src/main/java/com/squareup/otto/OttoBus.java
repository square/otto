/*
 * Copyright (C) 2012 Square, Inc.
 * Copyright (C) 2007 The Guava Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squareup.otto;

/**
 * Interface for event bus implementations, which dispatch events to listeners and provide ways for listeners to
 * register themselves.
 *
 * <p>A class implementing this interface (henceforth, a "bus") allows publish-subscribe-style communication between
 * components without requiring the components to explicitly register with one another (and thus be aware of each
 * other).  This is designed exclusively to replace traditional Android in-process event distribution using explicit
 * registration or listeners. It is <em>not</em> a general-purpose publish-subscribe system, nor is it intended for
 * interprocess communication.
 *
 * <h2>Receiving Events</h2>
 * To receive events, an object should:
 * <ol>
 * <li>Expose a public method, known as the <i>event handler</i>, which accepts a single argument of the type of event
 * desired;</li>
 * <li>Mark it with a {@link com.squareup.otto.Subscribe} annotation;</li>
 * <li>Pass itself to the bus' {@link #register(Object)} method.
 * </li>
 * </ol>
 *
 * <h2>Posting Events</h2>
 * To post an event, simply provide the event object to the bus' {@link #post(Object)} method.  The bus will
 * determine the type of event and route it to all registered listeners.
 *
 * <p>Events are routed based on their type &mdash; an event will be delivered to any handler for any type to which the
 * event is <em>assignable.</em>  This includes implemented interfaces, all superclasses, and all interfaces implemented
 * by superclasses.
 *
 * <p>When {@code post} is called, all registered handlers for an event are run in sequence, so handlers should be
 * reasonably quick.  If an event may trigger an extended process (such as a database load), spawn a thread or queue it
 * for later.
 *
 * <h2>Handler Methods</h2>
 * Event handler methods must accept only one argument: the event.
 *
 * <p>Handlers should not, in general, throw.  If they do, the bus will wrap the exception and
 * re-throw it.
 *
 * <h2>Producer Methods</h2>
 * Producer methods should accept no arguments and return their event type. When a subscriber is registered for a type
 * that a producer is also already registered for, the subscriber will be called with the return value from the
 * producer, provided it is not null.
 *
 * <h2>Dead Events</h2>
 * If an event is posted, but no registered handlers can accept it, it is considered "dead."  To give the system a
 * second chance to handle dead events, they are wrapped in an instance of {@link com.squareup.otto.DeadEvent} and
 * reposted.
 *
 * @author Cliff Biffle
 * @author Jake Wharton
 */
public interface OttoBus {
  /**
   * Registers all handler methods on {@code object} to receive events and producer methods to provide events.
   * <p>
   * If any subscribers are registering for types which already have a producer they will be called immediately
   * with the result of calling that producer, unless the result is null.
   * <p>
   * If any producers are registering for types which already have subscribers, each subscriber will be called with
   * the value from the result of calling the producer, unless the result is null.
   *
   * @param object object whose handler methods should be registered.
   */
  void register(Object object);

  /**
   * Unregisters all producer and handler methods on a registered {@code object}.
   *
   * @param object object whose producer and handler methods should be unregistered.
   * @throws IllegalArgumentException if the object was not previously registered.
   */
  void unregister(Object object);

  /**
   * Posts an event to all registered handlers.  This method will return successfully after the event has been posted to
   * all handlers, and regardless of any exceptions thrown by handlers.
   *
   * <p>If no handlers have been subscribed for {@code event}'s class, and {@code event} is not already a
   * {@link DeadEvent}, it will be wrapped in a DeadEvent and reposted.
   *
   * @param event event to post.
   */
  void post(Object event);
}
