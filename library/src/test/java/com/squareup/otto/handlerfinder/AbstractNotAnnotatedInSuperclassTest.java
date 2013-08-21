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
public class AbstractNotAnnotatedInSuperclassTest
    extends AbstractHandlerFinderTest<AbstractNotAnnotatedInSuperclassTest.SubClass> {

  @Test public void overriddenAndAnnotatedInSubclass() {
    Assertions.assertThat(getHandler().overriddenAndAnnotatedInSubclassEvents)
        .containsExactly(EVENT);
  }

  @Test public void overriddenInSubclassNowhereAnnotated() {
    Assertions.assertThat(getHandler().overriddenInSubclassNowhereAnnotatedEvents).isEmpty();
  }

  @Override SubClass createHandler() {
    return new SubClass();
  }

  abstract static class SuperClass {
    @SuppressWarnings("UnusedDeclaration")
    public abstract void overriddenInSubclassNowhereAnnotated(Object o);

    @SuppressWarnings("UnusedDeclaration")
    public abstract void overriddenAndAnnotatedInSubclass(Object o);
  }

  static class SubClass extends SuperClass {
    final List<Object> overriddenInSubclassNowhereAnnotatedEvents = new ArrayList<Object>();
    final List<Object> overriddenAndAnnotatedInSubclassEvents = new ArrayList<Object>();

    @Override
    public void overriddenInSubclassNowhereAnnotated(Object o) {
      overriddenInSubclassNowhereAnnotatedEvents.add(o);
    }

    @Subscribe @Override
    public void overriddenAndAnnotatedInSubclass(Object o) {
      overriddenAndAnnotatedInSubclassEvents.add(o);
    }
  }
}
