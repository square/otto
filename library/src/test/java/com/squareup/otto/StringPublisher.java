package com.squareup.otto;

public class StringPublisher {
  public static final String VALUE = "Hello, Provider";

  @Publish public String gimme() {
    return VALUE;
  }
}
