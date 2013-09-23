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

package com.squareup.otto.outside;

import com.squareup.otto.Bus;
import com.squareup.otto.Otto;
import com.squareup.otto.StringCatcher;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DestructionTest {
  private static final String EVENT = "Hello";
  private Bus root = Otto.createBus();
  private Bus child = root.spawn();
  private StringCatcher catcher = new StringCatcher();

  @Test public void destroyedBusDoesNotDispatchEvents() {
    root.register(catcher);
    root.destroy();
    root.post(EVENT);
    catcher.assertThatEvents("Destroyed bus should not dispatch post.").isEmpty();
  }

  @Test public void destroyingIsIdempotent() {
    root.register(catcher);
    root.destroy();
    root.destroy();
    // ta da!
  }

  @Test public void destroyedBusCannotRegisterSubscribers() {
    root.destroy();
    try {
      root.register(catcher);
      Assert.fail("Should not be possible to register subscribers on a destroyed bus.");
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
