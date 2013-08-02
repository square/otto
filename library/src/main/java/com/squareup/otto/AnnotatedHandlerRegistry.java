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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.squareup.otto.EventHandler.EventProducer;
import com.squareup.otto.EventHandler.EventSubscriber;

/**
 * Helper methods for finding methods annotated with {@link Produce} and
 * {@link Subscribe}.
 *
 * @author Cliff Biffle
 * @author Louis Wasserman
 * @author Jake Wharton
 * @author Sergej Shafarenka
 */
final class AnnotatedHandlerRegistry {

    /** Cache event bus producer methods for each class. */
    private static final Map<Class<?>, Map<Class<?>, Method>> PRODUCERS_CACHE
        = new HashMap<Class<?>, Map<Class<?>, Method>>();

    /** Cache event bus subscriber methods for each class. */
    private static final Map<Class<?>, Map<Class<?>, List<Method>>> SUBSCRIBERS_CACHE
        = new HashMap<Class<?>, Map<Class<?>, List<Method>>>();

    /** Cache for class hierarchy of an event. */
    private static final Map<Class<?>, List<Class<?>>> EVENT_CLASS_HIERARCHY_CACHE
        = new HashMap<Class<?>, List<Class<?>>>();

    private AnnotatedHandlerRegistry() { }

    /**
     * Load all methods annotated with {@link Produce} or {@link Subscribe} into
     * their respective caches for the specified class.
     */
    private static void loadAnnotatedMethodsCache(Class<?> listenerClass) {
        Map<Class<?>, List<Method>> subscriberMethods = new HashMap<Class<?>, List<Method>>();
        Map<Class<?>, Method> producerMethods = new HashMap<Class<?>, Method>();

        for (Method method : listenerClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Subscribe.class)) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1) {
                    throw new IllegalArgumentException("Method " + method + " has @Subscribe annotation but requires "
                            + parameterTypes.length + " arguments.  Methods must require a single argument.");
                }

                Class<?> eventType = parameterTypes[0];
                if (eventType.isInterface()) {
                    throw new IllegalArgumentException("Method " + method + " has @Subscribe annotation on "
                            + eventType + " which is an interface.  Subscription must be on a concrete class type.");
                }

                if ((method.getModifiers() & Modifier.PUBLIC) == 0) {
                    throw new IllegalArgumentException("Method " + method + " has @Subscribe annotation on "
                            + eventType + " but is not 'public'.");
                }

                List<Method> methods = subscriberMethods.get(eventType);
                if (methods == null) {
                    methods = new ArrayList<Method>();
                    subscriberMethods.put(eventType, methods);
                }
                methods.add(method);
                method.setAccessible(true);
            } else if (method.isAnnotationPresent(Produce.class)) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 0) {
                    throw new IllegalArgumentException("Method " + method + "has @Produce annotation but requires "
                            + parameterTypes.length + " arguments.  Methods must require zero arguments.");
                }
                if (method.getReturnType() == Void.class) {
                    throw new IllegalArgumentException("Method " + method
                            + " has a return type of void.  Must declare a non-void type.");
                }

                Class<?> eventType = method.getReturnType();
                if (eventType.isInterface()) {
                    throw new IllegalArgumentException("Method " + method + " has @Produce annotation on " + eventType
                            + " which is an interface.  Producers must return a concrete class type.");
                }
                if (eventType.equals(Void.TYPE)) {
                    throw new IllegalArgumentException("Method " + method
                            + " has @Produce annotation but has no return type.");
                }

                if ((method.getModifiers() & Modifier.PUBLIC) == 0) {
                    throw new IllegalArgumentException("Method " + method + " has @Produce annotation on " + eventType
                            + " but is not 'public'.");
                }

                if (producerMethods.containsKey(eventType)) {
                    throw new IllegalArgumentException("Producer for type " + eventType
                            + " has already been registered.");
                }
                producerMethods.put(eventType, method);
                method.setAccessible(true);
            }
        }

        PRODUCERS_CACHE.put(listenerClass, producerMethods);
        SUBSCRIBERS_CACHE.put(listenerClass, subscriberMethods);
    }

    /**
     * This implementation finds all methods marked with a {@link Produce}
     * annotation.
     */
    public static Map<Class<?>, EventProducer> findEventProducers(Object listener) {
        final Class<?> listenerClass = listener.getClass();
        Map<Class<?>, EventProducer> handlersInMethod = new HashMap<Class<?>, EventProducer>();

        if (!PRODUCERS_CACHE.containsKey(listenerClass)) {
            loadAnnotatedMethodsCache(listenerClass);
        }
        Map<Class<?>, Method> methods = PRODUCERS_CACHE.get(listenerClass);
        if (!methods.isEmpty()) {
            for (Map.Entry<Class<?>, Method> e : methods.entrySet()) {
                EventProducer producer = new EventProducer(listener, e.getValue());
                handlersInMethod.put(e.getKey(), producer);
            }
        }

        return handlersInMethod;
    }

    /**
     * This implementation finds all methods marked with a {@link Subscribe}
     * annotation.
     */
    public static Map<Class<?>, Collection<EventSubscriber>> findEventSubscribers(Object listener) {
        Class<?> listenerClass = listener.getClass();
        Map<Class<?>, Collection<EventSubscriber>> handlersInMethod
            = new HashMap<Class<?>, Collection<EventSubscriber>>();

        if (!SUBSCRIBERS_CACHE.containsKey(listenerClass)) {
            loadAnnotatedMethodsCache(listenerClass);
        }
        Map<Class<?>, List<Method>> methods = SUBSCRIBERS_CACHE.get(listenerClass);
        if (!methods.isEmpty()) {
            for (Map.Entry<Class<?>, List<Method>> e : methods.entrySet()) {
                ArrayList<EventSubscriber> handlers = new ArrayList<EventSubscriber>();
                for (Method m : e.getValue()) {
                    handlers.add(new EventSubscriber(listener, m));
                }
                handlersInMethod.put(e.getKey(), handlers);
            }
        }

        return handlersInMethod;
    }

    /**
     * Flattens a class's type hierarchy into a set of Class objects. The set
     * will include all superclasses (transitively), and all interfaces
     * implemented by these superclasses.
     *
     * @param concreteClass
     *            class whose type hierarchy will be retrieved.
     * @return {@code concreteClass}'s complete type hierarchy, flattened and
     *         uniqued.
     */
    protected static List<Class<?>> readEventClassHierarchy(Object event) {
        Class<?> concreteClass = event.getClass();
        List<Class<?>> classes = EVENT_CLASS_HIERARCHY_CACHE.get(concreteClass);
        if (classes == null) {
            classes = new ArrayList<Class<?>>();
            Class<?> clazz = concreteClass;
            do {
                classes.add(clazz);
                clazz = clazz.getSuperclass();
            } while (clazz != null);
            EVENT_CLASS_HIERARCHY_CACHE.put(concreteClass, classes);
        }
        return classes;
    }

}
