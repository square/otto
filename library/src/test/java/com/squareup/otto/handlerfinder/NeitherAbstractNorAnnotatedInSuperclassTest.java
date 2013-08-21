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
