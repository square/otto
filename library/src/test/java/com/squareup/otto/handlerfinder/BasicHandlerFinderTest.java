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
