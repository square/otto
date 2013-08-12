package com.squareup.otto.outside;

import com.squareup.otto.Bus;
import com.squareup.otto.Shuttle;
import com.squareup.otto.ShuttleDispatcher;
import com.squareup.otto.StringCatcher;
import org.junit.Before;
import org.junit.Test;

public class HierarchyTest {

  private static final String EVENT = "Hello";
  private Bus root;
  private Bus child;
  private StringCatcher catcher;

  @Before public void setUp() {
    root = new Shuttle(ShuttleDispatcher.TEST);
    root.enable();
    child = root.spawn();
    catcher = new StringCatcher();
  }

  @Test public void childIsDisabledWhenSpawned() {
    child.register(catcher);
    child.post(EVENT);
    catcher.assertThatEvents("Child should be disabled when spawned.").isEmpty();
  }

  @Test public void childCanBeEnabledAndDisabled() {
    child.register(catcher);
    child.enable();
    child.post(EVENT);
    catcher.assertThatEvents("Bus should be enabled.").isNotEmpty();
    catcher.reset();
    child.disable();
    child.post(EVENT);
    catcher.assertThatEvents("Child should be disabled.").isEmpty();
  }

  @Test public void postsGoDown() {
    child.register(catcher);
    child.enable();
    root.post(EVENT);
    catcher.assertThatEvents("Child should receive event posted to parent.").containsExactly(EVENT);
  }

  @Test public void postsDoNotGoUp() {
    root.register(catcher);
    child.enable();
    child.post(EVENT);
    catcher.assertThatEvents("Parent should not receive event posted to child.").isEmpty();
  }

  @Test public void disableAndEnableParentDoesSameToChild() {
    child.register(catcher);
    root.disable();
    child.post(EVENT);
    catcher.assertThatEvents("Child should be disabled.").isEmpty();
    root.enable();
    child.post(EVENT);
    catcher.assertThatEvents("Child should be enabled.").isNotEmpty();
  }
}
