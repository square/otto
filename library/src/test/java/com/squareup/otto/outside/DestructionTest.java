package com.squareup.otto.outside;

import com.squareup.otto.Bus;
import com.squareup.otto.OttoBus;
import com.squareup.otto.StringCatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.squareup.otto.DeadEventHandler.IGNORE_DEAD_EVENTS;

@RunWith(RobolectricTestRunner.class)
public class DestructionTest {
  private static final String EVENT = "Hello";
  private Bus root;
  private Bus child;
  private StringCatcher catcher;

  @Before public void setUp() {
    root = new OttoBus(IGNORE_DEAD_EVENTS);
    child = root.spawn();
    catcher = new StringCatcher();
  }

  @Test public void destroyedBusDoesNotDispatchEvents() {
    root.register(catcher);
    root.destroy();
    root.post(EVENT);
    catcher.assertThatEvents("Destroyed bus should not dispatch post.").isEmpty();
  }

  @Test public void destroyedBusCannotBeDestroyed() {
    root.register(catcher);
    root.destroy();
    try {
      root.destroy();
      Assert.fail("Should not be possible to destroy a destroyed bus.");
    } catch (IllegalStateException e) {
    }
  }

  @Test public void destroyedChildDoesNotReceiveEventPostedToParent() {
    child.register(catcher);
    child.destroy();
    root.post(EVENT);
    catcher.assertThatEvents("Destroyed bus should not receive event posted to parent.").isEmpty();
  }

  @Test public void destroyParentAlsoDestroysChild() {
    child.register(catcher);
    root.destroy();
    child.post(EVENT);
    catcher.assertThatEvents("Destroying parent should destroy child.").isEmpty();
  }
}
