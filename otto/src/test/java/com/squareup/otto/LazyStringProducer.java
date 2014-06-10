package com.squareup.otto;

public class LazyStringProducer {
  public String value = null;

  @Produce public String gimme() {
    return value;
  }
}
