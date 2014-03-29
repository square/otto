package com.squareup.otto;

import org.junit.Test;
import static org.fest.assertions.api.Assertions.assertThat;

public class ProcessSuperClassTest {

  private final Bus bus = new Bus(ThreadEnforcer.ANY, "test-bus");

  @Test
  public void allowsSuperClassSubscription() {

    class Parent {

      protected boolean messageReceived;

      @Subscribe
      public void onMessage(String message) {
        messageReceived = true;
      }
    }

    @ProcessSuperClass
    class Child extends Parent {}

    final Child child = new Child();
    bus.register(child);
    bus.post("Foo");

    assertThat(child.messageReceived).isTrue();
  }

  @Test
  public void doesNotProcessSuperClassWhenAnnotationIsMissing() {

    class Parent {

      protected boolean messageReceived;

      @Subscribe
      public void onMessage(String message) {
        messageReceived = true;
      }
    }

    class Child extends Parent {}

    final Child child = new Child();
    bus.register(child);
    bus.post("Foo");

    assertThat(child.messageReceived).isFalse();
  }

}