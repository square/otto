package com.squareup.otto;

import java.util.Set;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class HierarchyFlattenerTest {

  private final HierarchyFlattener flattener = new HierarchyFlattener();

  @Test public void flattenHierarchy() {
    HierarchyFixture fixture = new HierarchyFixture();
    Set<Class<?>> hierarchy = flattener.flatten(fixture.getClass());

    assertThat(hierarchy).containsOnly( //
        Object.class, //
        HierarchyFixtureParent.class, //
        HierarchyFixture.class //
    );
  }

  public interface HierarchyFixtureInterface {
    // Exists only for hierarchy mapping; no members.
  }

  public interface HierarchyFixtureSubinterface extends HierarchyFixtureInterface {
    // Exists only for hierarchy mapping; no members.
  }

  public static class HierarchyFixtureParent implements HierarchyFixtureSubinterface {
    // Exists only for hierarchy mapping; no members.
  }

  public static class HierarchyFixture extends HierarchyFixtureParent {
    // Exists only for hierarchy mapping; no members.
  }
}
