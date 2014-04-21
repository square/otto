package com.squareup.otto.vanilla;

import com.squareup.otto.Produce;

public class LazyStringProducer {
  public String value = null;

  @Produce public String gimme() {
    return value;
  }
}
