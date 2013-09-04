package com.squareup.otto.outside;

import com.squareup.otto.Bus;
import com.squareup.otto.Shuttle;
import com.squareup.otto.StringCatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.squareup.otto.DeadEventHandler.IGNORE_DEAD_EVENTS;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class OutsideEventBusTest {
  private static final String EVENT = "Hello";
  Bus bus;

  @Before public void setUp() {
    bus = Shuttle.createRootBus(IGNORE_DEAD_EVENTS);
  }

  /*
   * If you do this test from common.eventbus.BusTest, it doesn't actually test the behavior.
   * That is, even if exactly the same method works from inside the common.eventbus package tests,
   * it can fail here.
   */
  @Test public void subscriberReceivesPostedEvent() {
    StringCatcher catcher = new StringCatcher();
    bus.register(catcher);
    bus.post(EVENT);
    catcher.assertThatEvents("Subscriber should receive posted event.")
        .containsExactly(EVENT);
  }

  @Test public void subscriberOnlyReceivesEventsForType() {
    StringCatcher catcher = new StringCatcher();
    bus.register(catcher);
    bus.post(new Object());
    catcher.assertThatEvents("Subscriber should not receive event of wrong type.")
        .isEmpty();
  }

  @Test public void onlySubscriberOfCorrectTypeReceivesEvent() {
    StringCatcher catcher = new StringCatcher();
    IntegerCatcher intCatcher = new IntegerCatcher();
    bus.register(catcher);
    bus.post(EVENT);
    assertThat(intCatcher.getEvents()).as("Subscriber should not receive event of wrong type.")
        .isEmpty();
    catcher.assertThatEvents("Subscriber of matching type should receive posted event.")
        .containsExactly(EVENT);
  }
}
