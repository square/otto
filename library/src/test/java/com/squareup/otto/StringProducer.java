package com.squareup.otto;

public class StringProducer {
  public static final String VALUE = "Hello, Producer";

  @Produce public String gimme() {
    return VALUE;
  }
}
