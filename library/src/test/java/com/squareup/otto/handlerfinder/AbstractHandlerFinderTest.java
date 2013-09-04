package com.squareup.otto.handlerfinder;

import com.squareup.otto.Bus;
import com.squareup.otto.OttoBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

import static com.squareup.otto.DeadEventHandler.IGNORE_DEAD_EVENTS;

@Ignore // Tests are in extending classes.
public abstract class AbstractHandlerFinderTest<H> {
  static final Object EVENT = new Object();

  abstract H createHandler();

  private H handler;

  H getHandler() {
    return handler;
  }

  @Before
  public void setUp() throws Exception {
    handler = createHandler();
    Bus bus = new OttoBus(IGNORE_DEAD_EVENTS);
    bus.register(handler);
    bus.post(EVENT);
  }

  @After
  public void tearDown() throws Exception {
    handler = null;
  }
}
