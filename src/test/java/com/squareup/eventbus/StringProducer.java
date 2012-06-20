package com.squareup.eventbus;

public class StringProducer {
  public static final String VALUE = "Hello, Producer";

  @Produce public String gimme() {
    return VALUE;
  }
}
