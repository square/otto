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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wraps a single-argument 'handler' method on a specific object.
 * 
 * <p>
 * This class only verifies the suitability of the method and event type if
 * something fails. Callers are expected t verify their uses of this class.
 * 
 * <p>
 * Two EventHandlers are equivalent when they refer to the same method on the
 * same object (not class). This property is used to ensure that no handler
 * method is registered more than once.
 * 
 * @author Cliff Biffle
 * @author Jake Wharton
 * @author Sergej Shafarenka
 */
abstract class EventHandler {

	/** Event subscriber handler */
	static class EventSubscriber extends EventHandler {
		public EventSubscriber(Object target, Method method) {
			super(target, method);
		}

		public void handleEvent(Object event) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
			if (!valid) throw new IllegalStateException(toString() + " has been invalidated and can no longer handle events.");
			method.invoke(target, event);
		}

	}

	/** Event producer handler */
	static class EventProducer extends EventHandler {
		public EventProducer(Object target, Method method) {
			super(target, method);
		}

		public Object produceEvent() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
			if (!valid) throw new IllegalStateException(toString() + " has been invalidated and can no longer produce events.");
			return method.invoke(target);
		}
	}
	
	/** Object sporting the handler method. */
	protected final Object target;
	/** Handler method. */
	protected final Method method;
	/** Object hash code. */
	protected final int hashCode;
	/** Shows whether handler is still valid for event delivery */
	public boolean valid = true;

	EventHandler(Object target, Method method) {
		if (target == null) throw new NullPointerException("EventHandler target cannot be null.");
		if (method == null) throw new NullPointerException("EventHandler method cannot be null.");

		this.target = target;
		this.method = method;

		// Compute hash code eagerly since we know it will be used frequently
		// and we cannot estimate the runtime of the
		// target's hashCode call.
		hashCode = (31 + method.hashCode()) * 31 + target.hashCode();
	}

	@Override public int hashCode() {
		return hashCode;
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final EventHandler other = (EventHandler) obj;
		return method.equals(other.method) && target == other.target;
	}
	
	@Override public String toString() {
		return new StringBuffer('[')
			.append(getClass().getSimpleName())
			.append(' ')
			.append(method)
			.append(']').toString();
	}

}
