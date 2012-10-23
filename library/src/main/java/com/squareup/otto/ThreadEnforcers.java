package com.squareup.otto;

import android.os.Looper;

public abstract class ThreadEnforcers {

  private ThreadEnforcers() {
  }

  /** A {@link com.squareup.otto.ThreadEnforcer} that does no verification or enforcement for any action. */
  public static final ThreadEnforcer NONE = new ThreadEnforcer() {
    @Override public void enforce(Bus bus) {
      // Allow any thread.
    }
  };

  /** A {@link ThreadEnforcer} that confines {@link Bus} methods to the main thread. */
  public static final ThreadEnforcer MAIN = new ThreadEnforcer() {
    @Override public void enforce(Bus bus) {
      if (Looper.myLooper() != Looper.getMainLooper()) {
        throw new IllegalStateException("Event bus " + bus + " accessed from non-main thread " + Looper.myLooper());
      }
    }
  };

}
