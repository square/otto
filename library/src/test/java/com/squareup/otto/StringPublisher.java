package com.squareup.otto;

public class StringPublisher {
  public static final String VALUE = "Hello, Publisher";

  @Publish public String gimme() {
    return VALUE;
  }
}
