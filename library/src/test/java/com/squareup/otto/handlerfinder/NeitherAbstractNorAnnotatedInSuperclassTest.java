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

package com.squareup.otto.handlerfinder;

import com.squareup.otto.Subscribe;
import java.util.ArrayList;
import java.util.List;
import org.fest.assertions.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class NeitherAbstractNorAnnotatedInSuperclassTest
    extends AbstractHandlerFinderTest<NeitherAbstractNorAnnotatedInSuperclassTest.SubClass> {

  @Test public void neitherOverriddenNorAnnotated() {
    Assertions.assertThat(getHandler().neitherOverriddenNorAnnotatedEvents).isEmpty();
  }

  @Test public void overriddenInSubclassNowhereAnnotated() {
    Assertions.assertThat(getHandler().overriddenInSubclassNowhereAnnotatedEvents).isEmpty();
  }

  @Test public void overriddenAndAnnotatedInSubclass() {
    Assertions.assertThat(getHandler().overriddenAndAnnotatedInSubclassEvents)
        .containsExactly(EVENT);
  }

  @Override SubClass createHandler() {
    return new SubClass();
  }

  static class SuperClass {
    final List<Object> neitherOverriddenNorAnnotatedEvents = new ArrayList<Object>();
    final List<Object> overriddenInSubclassNowhereAnnotatedEvents = new ArrayList<Object>();
    final List<Object> overriddenAndAnnotatedInSubclassEvents = new ArrayList<Object>();

    @SuppressWarnings("UnusedDeclaration") public void neitherOverriddenNorAnnotated(Object o) {
      neitherOverriddenNorAnnotatedEvents.add(o);
    }

    public void overriddenInSubclassNowhereAnnotated(Object o) {
      overriddenInSubclassNowhereAnnotatedEvents.add(o);
    }

    public void overriddenAndAnnotatedInSubclass(Object o) {
      overriddenAndAnnotatedInSubclassEvents.add(o);
    }
  }

  static class SubClass extends SuperClass {
    @Override
    public void overriddenInSubclassNowhereAnnotated(Object o) {
      super.overriddenInSubclassNowhereAnnotated(o);
    }

    @Subscribe @Override
    public void overriddenAndAnnotatedInSubclass(Object o) {
      super.overriddenAndAnnotatedInSubclass(o);
    }
  }
}
