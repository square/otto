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

import java.util.Set;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class HierarchyFlattenerTest {

  private final HierarchyFlattener flattener = new HierarchyFlattener();

  @Test public void flattenHierarchy() {
    HierarchyFixture fixture = new HierarchyFixture();
    Set<Class<?>> hierarchy = flattener.flatten(fixture.getClass());

    assertThat(hierarchy).containsOnly( //
        Object.class, //
        HierarchyFixtureParent.class, //
        HierarchyFixture.class //
    );
  }

  public interface HierarchyFixtureInterface {
    // Exists only for hierarchy mapping; no members.
  }

  public interface HierarchyFixtureSubinterface extends HierarchyFixtureInterface {
    // Exists only for hierarchy mapping; no members.
  }

  public static class HierarchyFixtureParent implements HierarchyFixtureSubinterface {
    // Exists only for hierarchy mapping; no members.
  }

  public static class HierarchyFixture extends HierarchyFixtureParent {
    // Exists only for hierarchy mapping; no members.
  }
}
