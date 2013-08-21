package com.squareup.otto.handlerfinder;

import com.squareup.otto.Subscribe;
import java.util.ArrayList;
import java.util.List;
import org.fest.assertions.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/*
 * We break the tests up based on whether they are annotated or abstract in the superclass.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BasicHandlerFinderTest
    extends AbstractHandlerFinderTest<BasicHandlerFinderTest.Handler> {
  static class Handler {
    final List<Object> nonSubscriberEvents = new ArrayList<Object>();
    final List<Object> subscriberEvents = new ArrayList<Object>();

    public void notASubscriber(Object o) {
      nonSubscriberEvents.add(o);
    }

    @Subscribe
    public void subscriber(Object o) {
      subscriberEvents.add(o);
    }
  }

  @Test public void nonSubscriber() {
    Assertions.assertThat(getHandler().nonSubscriberEvents).isEmpty();
  }

  @Test public void subscriber() {
    Assertions.assertThat(getHandler().subscriberEvents).containsExactly(
        EVENT);
  }

  @Override Handler createHandler() {
    return new Handler();
  }
}
