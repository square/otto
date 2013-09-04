package com.squareup.otto;

public interface DeadEventHandler {
  DeadEventHandler IGNORE_DEAD_EVENTS = new DeadEventHandler() {
    @Override public void onDeadEvent(Object event) {
    }
  };

  void onDeadEvent(Object event);
}
