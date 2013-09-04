package com.squareup.otto;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DeadEventTest {
  private static final String EVENT = "Hello";

  DeadEventCatcher catcher;
  Bus bus;

  @Before public void setUp() throws Exception {
    catcher = new DeadEventCatcher();
    bus = new OttoBus(catcher);
  }

  @Test public void eventDiesIfNoSubscribers() {
    // A String -- an event for which noone has registered.
    bus.post(EVENT);
    assertThat(catcher.getEvents()).containsExactly(EVENT);
  }

  @Test public void eventDoesntDieIfChildHasSubscribers() {
    bus.spawn().register(new StringCatcher());
    bus.post(EVENT);
    assertThat(catcher.getEvents()).isEmpty();
  }

  public static class DeadEventCatcher implements DeadEventHandler {
    private List<Object> events = new ArrayList<Object>();

    public List<Object> getEvents() {
      return events;
    }

    @Override public void onDeadEvent(Object event) {
      events.add(event);
    }
  }
}
