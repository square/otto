package com.squareup.otto;

import org.junit.Test;

import static com.squareup.otto.ReflectionFinder.loadAnnotatedMethods;
import static junit.framework.Assert.fail;

public class ReflectiveFinderTest {

  @Test public void subscribingOrProducingOnlyAllowedOnPublicMethods() {
    try {
      loadAnnotatedMethods(new Object() {
        @Subscribe protected void method(Object o) {
        }
      }.getClass());
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      loadAnnotatedMethods(new Object() {
        @Subscribe void method(Object o) {}
      }.getClass());
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      loadAnnotatedMethods(new Object() {
        @Subscribe private void method(Object o) {}
      }.getClass());
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      loadAnnotatedMethods(new Object() {
        @Produce protected Object method() { return null; }
      }.getClass());
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      loadAnnotatedMethods(new Object() {
        @Produce Object method() { return null; }
      }.getClass());
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      loadAnnotatedMethods(new Object() {
        @Produce private Object method() { return null; }
      }.getClass());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void voidProducerThrowsException() throws Exception {
    loadAnnotatedMethods(new Object() {
      @Produce public void things() {}
    }.getClass());
  }
}
