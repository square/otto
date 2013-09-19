/*
 * Copyright (C) 2013 Square, Inc.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class HierarchyFlattener {
  private final Map<Class<?>, Set<Class<?>>> cache = new HashMap<Class<?>, Set<Class<?>>>();

  /**
   * Flattens a class's type hierarchy into a set of Class objects.  The set will include all
   * superclasses (transitively), and all interfaces implemented by these superclasses.
   *
   * @param concreteClass class whose type hierarchy will be retrieved.
   * @return {@code concreteClass}'s complete type hierarchy, flattened and uniqued.
   */
  Set<Class<?>> flatten(Class<?> concreteClass) {
    Set<Class<?>> classes = cache.get(concreteClass);
    if (classes == null) {
      classes = getClassesFor(concreteClass);
      cache.put(concreteClass, classes);
    }

    return classes;
  }

  private Set<Class<?>> getClassesFor(Class<?> concreteClass) {
    List<Class<?>> parents = new LinkedList<Class<?>>();
    Set<Class<?>> classes = new HashSet<Class<?>>();

    parents.add(concreteClass);

    while (!parents.isEmpty()) {
      Class<?> clazz = parents.remove(0);
      classes.add(clazz);

      Class<?> parent = clazz.getSuperclass();
      if (parent != null) {
        parents.add(parent);
      }
    }
    return classes;
  }
}
