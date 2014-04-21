package com.squareup.otto.vanilla;

import com.squareup.otto.Produce;

public class StringProducer {
  public static final String VALUE = "Hello, Producer";

  @Produce public String gimme() {
    return VALUE;
  }
}
