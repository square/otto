package com.squareup.otto.outside;

import com.squareup.otto.Bus;
import com.squareup.otto.Shuttle;
import com.squareup.otto.StringCatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class HierarchyTest {

  private static final String EVENT = "Hello";
  private Bus root;
  private Bus child;
  private StringCatcher catcher;

  @Before public void setUp() {
    root = Shuttle.createRootBus();
    child = root.spawn();
    catcher = new StringCatcher();
  }

  @Test public void postsGoDown() {
    child.register(catcher);
    root.post(EVENT);
    catcher.assertThatEvents("Child should receive event posted to parent.").containsExactly(EVENT);
  }

  @Test public void postsDoNotGoUp() {
    root.register(catcher);
    child.post(EVENT);
    catcher.assertThatEvents("Parent should not receive event posted to child.").isEmpty();
  }
}
