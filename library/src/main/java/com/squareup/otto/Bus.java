package com.squareup.otto;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.squareup.otto.EventHandler.EventProducer;
import com.squareup.otto.EventHandler.EventSubscriber;

/**
 * Dispatches events to listeners, and provides ways for listeners to register
 * themselves.
 * 
 * <p>
 * The Bus allows publish-subscribe-style communication between components
 * without requiring the components to explicitly register with one another (and
 * thus be aware of each other). It is designed exclusively to replace
 * traditional Android in-process event distribution using explicit registration
 * or listeners. It is <em>not</em> a general-purpose publish-subscribe system,
 * nor is it intended for interprocess communication.
 * 
 * <h2>Receiving Events</h2>
 * To receive events, an object should:
 * <ol>
 * <li>Expose a public method, known as the <i>event handler</i>, which accepts
 * a single argument of the type of event desired;</li>
 * <li>Mark it with a {@link com.squareup.otto.Subscribe} annotation;</li>
 * <li>Pass itself to an Bus instance's {@link #register(Object)} method.</li>
 * </ol>
 * 
 * <h2>Posting Events</h2>
 * To post an event, simply provide the event object to the
 * {@link #post(Object)} method. The Bus instance will determine the type of
 * event and route it to all registered listeners.
 * 
 * <p>
 * Events are routed based on their type &mdash; an event will be delivered to
 * any handler for any type to which the event is <em>assignable.</em> This
 * includes implemented interfaces, all superclasses, and all interfaces
 * implemented by superclasses.
 * 
 * <p>
 * When {@code post} is called, all registered handlers for an event are run in
 * sequence, so handlers should be reasonably quick. If an event may trigger an
 * extended process (such as a database load), spawn a thread or queue it for
 * later.
 * 
 * <h2>Handler Methods</h2> Event handler methods must accept only one argument:
 * the event.
 * 
 * <p>
 * Handlers should not, in general, throw. If they do, the Bus will wrap the
 * exception and re-throw it.
 * 
 * <p>
 * The Bus by default enforces that all interactions occur on the main thread.
 * You can provide an alternate enforcement by passing a {@link ThreadEnforcer}
 * to the constructor.
 * 
 * <h2>Producer Methods</h2>
 * Producer methods should accept no arguments and return their event type. When
 * a subscriber is registered for a type that a producer is also already
 * registered for, the subscriber will be called with the return value from the
 * producer.
 * 
 * <h2>Dead Events</h2>
 * If an event is posted, but no registered handlers can accept it, it is
 * considered "dead." To give the system a second chance to handle dead events,
 * they are wrapped in an instance of {@link com.squareup.otto.DeadEvent} and
 * reposted.
 * 
 * <p>
 * This class is safe for concurrent use.
 * 
 * @author Cliff Biffle
 * @author Jake Wharton
 * @author Sergej Shafarenka
 */
public class Bus {
	public static final String DEFAULT_IDENTIFIER = "default";
	
	/** All registered event handlers, indexed by event type. */
	private final Map<Class<?>, Collection<EventSubscriber>> subscribersByType = new HashMap<Class<?>, Collection<EventSubscriber>>();

	/** All registered event producers, index by event type. */
	private final Map<Class<?>, EventProducer> producersByType = new HashMap<Class<?>, EventProducer>();

	/** Thread enforcer for register, unregister, and posting events. */
	private final ThreadEnforcer enforcer;

	/** Used to find handler methods in register and unregister. */
	private final HandlerRegistry handlerRegistry;

	/** Events queue used for dispatching */
	private final ArrayDeque<EventWithHandler> eventsToDispatch = new ArrayDeque<EventWithHandler>();

	/** Identifier used to differentiate the event bus instance. */
	private final String identifier;

	/** Shows whether we dispatch events */
	private boolean dispatching;

	/**
	 * Creates a new Bus named "default" that enforces actions on the main
	 * thread.
	 */
	public Bus() {
		this(DEFAULT_IDENTIFIER);
	}

	/**
	 * Creates a new Bus with the given {@code identifier} that enforces actions
	 * on the main thread.
	 * 
	 * @param identifier
	 *            a brief name for this bus, for debugging purposes. Should be a
	 *            valid Java identifier.
	 */
	public Bus(String identifier) {
		this(ThreadEnforcer.MAIN, identifier);
	}

	/**
	 * Creates a new Bus named "default" with the given {@code enforcer} for
	 * actions.
	 * 
	 * @param enforcer
	 *            Thread enforcer for register, unregister, and post actions.
	 */
	public Bus(ThreadEnforcer enforcer) {
		this(enforcer, DEFAULT_IDENTIFIER);
	}

	/**
	 * Creates a new Bus with the given {@code enforcer} for actions and the
	 * given {@code identifier}.
	 * 
	 * @param enforcer
	 *            Thread enforcer for register, unregister, and post actions.
	 * @param identifier
	 *            A brief name for this bus, for debugging purposes. Should be a
	 *            valid Java identifier.
	 */
	public Bus(ThreadEnforcer enforcer, String identifier) {
		this(enforcer, identifier, HandlerRegistry.ANNOTATED);
	}

	/**
	 * Test constructor which allows replacing the default {@code HandlerFinder}
	 * .
	 * 
	 * @param enforcer
	 *            Thread enforcer for register, unregister, and post actions.
	 * @param identifier
	 *            A brief name for this bus, for debugging purposes. Should be a
	 *            valid Java identifier.
	 * @param handlerRegistry
	 *            Used to discover event handlers and producers when
	 *            registering/unregistering an object.
	 */
	Bus(ThreadEnforcer enforcer, String identifier, HandlerRegistry handlerRegistry) {
		this.enforcer = enforcer;
		this.identifier = identifier;
		this.handlerRegistry = handlerRegistry;
	}

	@Override public String toString() {
		return "[Bus \"" + identifier + "\"]";
	}

	/**
	 * Registers all handler methods on {@code object} to receive events and
	 * producer methods to provide events.
	 * <p>
	 * If any subscribers are registering for types which already have a
	 * producer they will be called immediately with the result of calling that
	 * producer.
	 * <p>
	 * If any producers are registering for types which already have
	 * subscribers, each subscriber will be called with the value from the
	 * result of calling the producer.
	 * 
	 * @param object
	 *            object whose handler methods should be registered.
	 * @throws NullPointerException
	 *             if the object is null.
	 */
	public void register(Object object) {
		if (object == null) throw new NullPointerException("Object to register must not be null.");
		enforcer.enforce(this);

		// 1: add producers and dispatch events from these producers to subscribers
		Map<Class<?>, EventProducer> foundProducers = handlerRegistry.findEventProducers(object);
		for (Class<?> type : foundProducers.keySet()) {
			final EventProducer producer = foundProducers.get(type);
			EventProducer previousProducer = producersByType.put(type, producer);

			// checking if the previous producer existed
			if (previousProducer != null && producer.valid) {
				throw new IllegalArgumentException("Producer method for type " + type + " found on type "
						+ producer.target.getClass() + ", but already registered by type "
						+ previousProducer.target.getClass() + ".");
			}

			Collection<EventSubscriber> subscribers = subscribersByType.get(type);
			dispatchProducerEventToSubscribers(producer, subscribers, type);
		}

		// 2: add subscribers
		Map<Class<?>, Collection<EventSubscriber>> foundSubscribers = handlerRegistry.findEventSubscribers(object);
		for (Class<?> type : foundSubscribers.keySet()) {
			Collection<EventSubscriber> handlers = subscribersByType.get(type);
			if (handlers == null) {
				//handlers = new LinkedHashSet<EventSubscriber>();
				//handlers = new HashSet<EventSubscriber>();
				handlers = new ArrayList<EventSubscriber>();
				subscribersByType.put(type, handlers);
			}
			final Collection<EventSubscriber> foundHandlers = foundSubscribers.get(type);
			handlers.addAll(foundHandlers);
		}

		// 3: dispatch events to all newly registered subscribers
		for (Map.Entry<Class<?>, Collection<EventSubscriber>> entry : foundSubscribers.entrySet()) {
			Class<?> eventType = entry.getKey();
			EventProducer producer = producersByType.get(eventType);
			if (producer != null) {
				Collection<EventSubscriber> subscribers = entry.getValue();
				dispatchProducerEventToSubscribers(producer, subscribers, eventType);
			}
		}
	}

	private static void dispatchProducerEventToSubscribers(EventProducer producer, Collection<EventSubscriber> subscribers, Class<?> eventType) {

		// check input variables
		if (subscribers == null || subscribers.isEmpty()) return;

		// request and cache producer event. same event will be sent to all subscribers.
		Object event = null;
		try {
			event = producer.produceEvent();
		} catch (Exception e) {
			throw new RuntimeException("Producer " + producer + " threw an exception.", e);
		}
		if (event == null) return;
		
		for (EventSubscriber subscriber : subscribers) {
			if (!subscriber.valid) continue;
			if (!producer.valid) break;
			dispatch(event, subscriber);
		}
	}
	
	/**
	 * Unregisters all producer and handler methods on a registered
	 * {@code object}.
	 * 
	 * @param object
	 *            object whose producer and handler methods should be
	 *            unregistered.
	 * @throws IllegalArgumentException
	 *             if the object was not previously registered.
	 * @throws NullPointerException
	 *             if the object is null.
	 */
	public void unregister(Object object) {
		if (object == null) throw new NullPointerException("Object to unregister must not be null.");
		enforcer.enforce(this);

		// 1: remove producer
		Map<Class<?>, EventProducer> producersInListener = handlerRegistry.findEventProducers(object);
		for (Map.Entry<Class<?>, EventProducer> entry : producersInListener.entrySet()) {
			final Class<?> key = entry.getKey();
			EventProducer producer = producersByType.get(key);
			EventProducer value = entry.getValue();

			if (value == null || !value.equals(producer)) {
				throw new IllegalArgumentException("Missing event producer for an annotated method. Is "
						+ object.getClass() + " registered?");
			}
			producersByType.remove(key).valid = false;
		}

		// 2: remove subscriber
		Map<Class<?>, Collection<EventSubscriber>> handlersInListener = handlerRegistry.findEventSubscribers(object);
		for (Map.Entry<Class<?>, Collection<EventSubscriber>> entry : handlersInListener.entrySet()) {
			Collection<EventSubscriber> currentHandlers = subscribersByType.get(entry.getKey());
			Collection<EventSubscriber> eventMethodsInListener = entry.getValue();
			if (currentHandlers == null || !currentHandlers.containsAll(eventMethodsInListener)) {
				throw new IllegalArgumentException("Missing event handler for an annotated method. Is "
						+ object.getClass() + " registered?");
			}

			for (EventSubscriber handler : currentHandlers) {
				if (eventMethodsInListener.contains(handler)) handler.valid = false;
			}
			currentHandlers.removeAll(eventMethodsInListener);
		}

	}

	/**
	 * Posts an event to all registered handlers. This method will return
	 * successfully after the event has been posted to all handlers, and
	 * regardless of any exceptions thrown by handlers.
	 * 
	 * <p>
	 * If no handlers have been subscribed for {@code event}'s class, and
	 * {@code event} is not already a {@link DeadEvent}, it will be wrapped in a
	 * DeadEvent and reposted.
	 * 
	 * @param event
	 *            event to post.
	 * @throws NullPointerException
	 *             if the event is null.
	 */
	public void post(Object event) {
		if (event == null) throw new NullPointerException("Event to post must not be null.");
		enforcer.enforce(this);

		Collection<Class<?>> dispatchTypes = handlerRegistry.readEventClassHierarchy(event);
		boolean dispatched = false;
		for (Class<?> eventType : dispatchTypes) {
			Collection<EventSubscriber> subscribers = subscribersByType.get(eventType);
			if (subscribers != null && !subscribers.isEmpty()) {
				dispatched = true;
				for (EventSubscriber subscriber : subscribers) {
					eventsToDispatch.offer(EventWithHandler.obtain(event, subscriber));
				}
			}
		}

		if (!dispatched && !(event instanceof DeadEvent)) {
			post(new DeadEvent(this, event));
		}

		// don't dispatch if we're already dispatching, that would allow reentrancy and out-of-order events. Instead, leave
		// the events to be dispatched after the in-progress dispatch is complete.
		if (dispatching) return;

		dispatching = true;
		try {
			EventWithHandler eventWithHandler;
			while ((eventWithHandler = eventsToDispatch.poll()) != null) {
				dispatch(eventWithHandler.event, eventWithHandler.handler);
				EventWithHandler.release(eventWithHandler);
			}
		} finally {
			dispatching = false;
		}
	}

	/**
	 * Dispatches {@code event} to the handler in {@code wrapper}. This method
	 * is an appropriate override point for subclasses that wish to make event
	 * delivery asynchronous.
	 * 
	 * @param event
	 *            event to dispatch.
	 * @param wrapper
	 *            wrapper that will call the handler.
	 */
	private static void dispatch(Object event, EventSubscriber wrapper) {
		if (wrapper.valid) {
			try {
				wrapper.handleEvent(event);
			} catch (Exception e) {
				throw new RuntimeException("Could not dispatch event: " + event.getClass() + " to handler " + wrapper, e);
			}
		}
	}

	// methods for testing - begin
	
	Collection<EventSubscriber> getEventSubscribers(Class<?> eventType) {
		return subscribersByType.get(eventType);
	}

	Collection<Class<?>> getEventClassHierarchy(Object event) {
		return handlerRegistry.readEventClassHierarchy(event);
	}

	// methods for testing - end
	
	/** Simple struct representing an event and its handler */
	static class EventWithHandler {

		public Object event;
		public EventSubscriber handler;

		public EventWithHandler(Object event, EventSubscriber handler) {
			this.event = event;
			this.handler = handler;
		}

		/* simple object pool */
		private static final int POOL_SIZE = 8;
		private static final EventWithHandler[] sPool = new EventWithHandler[POOL_SIZE];
		private static int sCount;

		public static EventWithHandler obtain(Object event, EventSubscriber handler) {
			if (sCount > 0) {
				EventWithHandler res = sPool[--sCount];
				res.handler = handler;
				res.event = event;
				return res;
			} else {
				return new EventWithHandler(event, handler);
			}
		}

		public static void release(EventWithHandler object) {
			if (sCount == POOL_SIZE) return;
			object.event = null;
			object.handler = null;
			sPool[sCount++] = object;
		}
	}
	
}
