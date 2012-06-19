/*
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
 * A {@link com.squareup.eventbus.HandlerFindingStrategy} for collecting all event handler
 * methods that are marked with
 * the {@link com.squareup.eventbus.Subscribe} annotation.
 *
 * @author Cliff Biffle
 * @author Louis Wasserman
 */
class AnnotatedHandlerFinder implements HandlerFindingStrategy {

  /**
   * Returns all interfaces implemented by the class and all superclasses: all Classes that this is
   * assignable to.
   */
  static Set<Class<?>> getAllSuperclasses(Class<?> clazz) {
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
   * {@inheritDoc}
   *
   * This implementation finds all methods marked with a {@link com.squareup.eventbus.Subscribe}
   * annotation.
   */
  @Override public Map<Class<?>, Set<EventHandler>> findAllHandlers(Object listener) {
    Map<Class<?>, Set<EventHandler>> handlersInMethod = new HashMap<Class<?>, Set<EventHandler>>();

    Map<Class<?>, Set<Method>> methods = getSubscriberMethods(listener.getClass());
    if (methods.size() > 0) {
      for (Map.Entry<Class<?>, Set<Method>> e : methods.entrySet()) {
        Set<EventHandler> handlers = new HashSet<EventHandler>();
        for (Method m : e.getValue()) {
          handlers.add(new SynchronizedEventHandler(listener, m));
        }
        handlersInMethod.put(e.getKey(), handlers);
      }
    }

    return handlersInMethod;
  }

  /** Cache event bus subscriber methods for each class. */
  private static Map<Class<?>, Map<Class<?>, Set<Method>>> subscriberMethodsCache =
      new HashMap<Class<?>, Map<Class<?>, Set<Method>>>();

  static Map<Class<?>, Set<Method>> getSubscriberMethods(Class<?> listenerClass) {
    Map<Class<?>, Set<Method>> subscriberMethods = subscriberMethodsCache.get(listenerClass);
    if (subscriberMethods == null) {
      // Allocate and cache for rapid retrieval next time.
      subscriberMethods = new HashMap<Class<?>, Set<Method>>();
      subscriberMethodsCache.put(listenerClass, subscriberMethods);

      Set<Class<?>> supers = getAllSuperclasses(listenerClass);

      for (Method method : listenerClass.getMethods()) {
        /*
         * Iterate over each distinct method of {@code clazz}, checking if it is annotated with
         * @Subscribe by any of the superclasses or superinterfaces that declare it.
         */
        for (Class<?> c : supers) {
          try {
            Method m = c.getMethod(method.getName(), method.getParameterTypes());
            if (m.isAnnotationPresent(Subscribe.class)) {
              Class<?>[] parameterTypes = method.getParameterTypes();
              if (parameterTypes.length != 1) {
                throw new IllegalArgumentException("Method "
                    + method
                    + " has @Subscribe annotation, but requires "
                    + parameterTypes.length
                    + " arguments.  Event handler methods must require a single argument.");
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
    }
    return subscriberMethods;
  }
}
