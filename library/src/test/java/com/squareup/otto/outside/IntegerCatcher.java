package com.squareup.otto.outside;

import com.squareup.otto.Subscribe;
import java.util.ArrayList;
import java.util.List;

final class IntegerCatcher {
  private List<Integer> events = new ArrayList<Integer>();

  @Subscribe
  public void hereHaveAnInteger(Integer integer) {
    events.add(integer);
  }

  public List<Integer> getEvents() {
    return events;
  }
}
