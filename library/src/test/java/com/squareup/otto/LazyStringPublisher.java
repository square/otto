package com.squareup.otto;

public class LazyStringPublisher {
  public String value = null;

  @Publish public String gimme() {
    return value;
  }
}
