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

package com.squareup.eventbus;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Helper methods for finding methods annotated with {@link Produce} and {@link Subscribe}.
 *
 * @author Cliff Biffle
 * @author Louis Wasserman
 * @author Jake Wharton
 */
final class AnnotatedHandlerFinder {

  /** Cache event bus producer methods for each class. */
  private static final Map<Class<?>, Map<Class<?>, Method>> PRODUCERS_CACHE =
      new HashMap<Class<?>, Map<Class<?>, Method>>();

  /** Cache event bus subscriber methods for each class. */
  private static final Map<Class<?>, Map<Class<?>, Set<Method>>> SUBSCRIBERS_CACHE =
      new HashMap<Class<?>, Map<Class<?>, Set<Method>>>();

  /**
   * Returns all interfaces implemented by the class and all superclasses: all Classes that this is
   * assignable to.
   */
  private static Set<Class<?>> getAllSuperclasses(Class<?> clazz) {
    Queue<Class<?>> queue = new LinkedList<Class<?>>();
    Set<Class<?>> supers = new HashSet<Class<?>>();
    queue.add(clazz);
    while (!queue.isEmpty()) {
      Class<?> c = queue.poll();
      if (supers.add(c)) {
        queue.addAll(Arrays.asList(c.getInterfaces()));
        if (c.getSuperclass() != null) {
          queue.add(c.getSuperclass());
        }
      }
    }
    return supers;
  }

  /**
   * Load all methods annotated with {@link Produce} or {@link Subscribe} into their respective caches for the
   * specified class.
   */
  private static void loadAnnotatedMethods(Class<?> listenerClass) {
    Map<Class<?>, Set<Method>> subscriberMethods = new HashMap<Class<?>, Set<Method>>();
    Map<Class<?>, Method> producerMethods = new HashMap<Class<?>, Method>();

    Set<Class<?>> supers = getAllSuperclasses(listenerClass);

    for (Method method : listenerClass.getMethods()) {
      /*
       * Iterate over each distinct method of {@code clazz}, checking if it is annotated with
       * @Subscribe or @Produce by any of the superclasses or superinterfaces that declare it.
       */
      for (Class<?> c : supers) {
        try {
          Method m = c.getDeclaredMethod(method.getName(), method.getParameterTypes());
          if (m.isAnnotationPresent(Produce.class)) {
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
            if (producerMethods.containsKey(eventType)) {
              throw new IllegalArgumentException("Producer for type " + eventType + " has already been registered.");
            }
            producerMethods.put(eventType, method);
            break;
          } else if (m.isAnnotationPresent(Subscribe.class)) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
              throw new IllegalArgumentException("Method " + method + " has @Subscribe annotation but requires "
                  + parameterTypes.length + " arguments.  Methods must require a single argument.");
            }

            Class<?> eventType = parameterTypes[0];
            Set<Method> methods = subscriberMethods.get(eventType);
            if (methods == null) {
              methods = new HashSet<Method>();
              subscriberMethods.put(eventType, methods);
            }
            methods.add(m);
            break;
          }
        } catch (NoSuchMethodException ignored) {
          // Move on.
        }
      }
    }

    PRODUCERS_CACHE.put(listenerClass, producerMethods);
    SUBSCRIBERS_CACHE.put(listenerClass, subscriberMethods);
  }

  /** This implementation finds all methods marked with a {@link Produce} annotation. */
  static Map<Class<?>, EventProducer> findAllProducers(Object listener) {
    final Class<?> listenerClass = listener.getClass();
    Map<Class<?>, EventProducer> handlersInMethod = new HashMap<Class<?>, EventProducer>();

    if (!PRODUCERS_CACHE.containsKey(listenerClass)) {
      loadAnnotatedMethods(listenerClass);
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

  /** This implementation finds all methods marked with a {@link Subscribe} annotation. */
  static Map<Class<?>, Set<EventHandler>> findAllSubscribers(Object listener) {
    Class<?> listenerClass = listener.getClass();
    Map<Class<?>, Set<EventHandler>> handlersInMethod = new HashMap<Class<?>, Set<EventHandler>>();

    if (!SUBSCRIBERS_CACHE.containsKey(listenerClass)) {
      loadAnnotatedMethods(listenerClass);
    }
    Map<Class<?>, Set<Method>> methods = SUBSCRIBERS_CACHE.get(listenerClass);
    if (!methods.isEmpty()) {
      for (Map.Entry<Class<?>, Set<Method>> e : methods.entrySet()) {
        Set<EventHandler> handlers = new HashSet<EventHandler>();
        for (Method m : e.getValue()) {
          handlers.add(new EventHandler(listener, m));
        }
        handlersInMethod.put(e.getKey(), handlers);
      }
    }

    return handlersInMethod;
  }

  private AnnotatedHandlerFinder() {
    // No instances.
  }

}
