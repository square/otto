package com.squareup.otto.outside;

import com.squareup.otto.Bus;
import com.squareup.otto.Shuttle;
import com.squareup.otto.StringCatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
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

  @Test public void postsGoUp() {
    root.register(catcher);
    child.post(EVENT);
    catcher.assertThatEvents("Parent should receive event posted to child.").containsExactly(EVENT);
  }

  @Test public void postsGoEverywhere() {

    // Create a tree of buses.
    Bus[] buses = new Bus[10];
    buses[0] = Shuttle.createRootBus();
    for (int i = 0; i < buses.length / 2; i++) {
      buses[i + 1] = buses[i].spawn();
      buses[buses.length / 2 + i] = buses[i].spawn();
    }

    // Register a subscriber on each bus in the tree.
    StringCatcher[] catchers = new StringCatcher[buses.length];
    for (int i = 0; i < buses.length; i++) {
      catchers[i] = new StringCatcher();
      buses[i].register(catchers[i]);
    }

    // Post to each bus in the tree.
    for (int b = 0; b < buses.length; b++) {
      buses[b].post(EVENT);
    }

    // Every subscriber should have gotten every post.
    for (StringCatcher catcher : catchers) {
      catcher.assertThatEvents("Catcher received all events").hasSize(buses.length);
    }
  }
}
