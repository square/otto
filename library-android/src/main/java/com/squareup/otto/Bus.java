/*
 * Copyright (C) 2012 Square, Inc.
 * Copyright (C) 2007 The Guava Authors
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
 * Dispatches events to listeners, and provides ways for listeners to register themselves.
 *
 * <p>The Bus allows publish-subscribe-style communication between components without requiring the components to
 * explicitly register with one another (and thus be aware of each other).  It is designed exclusively to replace
 * traditional Android in-process event distribution using explicit registration or listeners. It is <em>not</em> a
 * general-purpose publish-subscribe system, nor is it intended for interprocess communication.
 *
 * <h2>Receiving Events</h2>
 * To receive events, an object should:
 * <ol>
 * <li>Expose a public method, known as the <i>event handler</i>, which accepts a single argument of the type of event
 * desired;</li>
 * <li>Mark it with a {@link Subscribe} annotation;</li>
 * <li>Pass itself to an Bus instance's {@link #register(Object)} method.
 * </li>
 * </ol>
 *
 * <h2>Posting Events</h2>
 * To post an event, simply provide the event object to the {@link #post(Object)} method.  The Bus instance will
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
 * <p>Handlers should not, in general, throw.  If they do, the Bus will wrap the exception and
 * re-throw it.
 *
 * <p>The Bus by default enforces that all interactions occur on the main thread.  You can provide an alternate
 * enforcement by passing a {@link ThreadEnforcer} to the constructor.
 *
 * <h2>Producer Methods</h2>
 * Producer methods should accept no arguments and return their event type. When a subscriber is registered for a type
 * that a producer is also already registered for, the subscriber will be called with the return value from the
 * producer.
 *
 * <h2>Dead Events</h2>
 * If an event is posted, but no registered handlers can accept it, it is considered "dead."  To give the system a
 * second chance to handle dead events, they are wrapped in an instance of {@link DeadEvent} and
 * reposted.
 *
 * <p>This class is safe for concurrent use.
 *
 * @author Cliff Biffle
 * @author Jake Wharton
 * @author Francisco Javier Fernandez
 */
public class Bus extends com.squareup.otto.vanilla.Bus {

    /**
     * Creates a new Bus with the given {@code identifier} that enforces Main thread for its actions.
     *
     * @param identifier a brief name for this bus, for debugging purposes.  Should be a valid Java identifier.
     */
    public Bus(String identifier) {
        super(ThreadEnforcer.MAIN, identifier);
    }
}
