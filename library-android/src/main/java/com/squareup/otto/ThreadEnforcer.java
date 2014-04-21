package com.squareup.otto;

import android.os.Looper;
import com.squareup.otto.vanilla.Bus;

/**
 * Enforces a thread confinement policy for methods on a particular event bus.
 *
 * @author Jake Wharton
 * @author Francisco Javier Fernandez
 */
public interface ThreadEnforcer extends com.squareup.otto.vanilla.ThreadEnforcer {

    /**
     * Enforce a valid thread for the given {@code bus}. Implementations may throw any runtime exception.
     *
     * @param bus Event bus instance on which an action is being performed.
     */
    void enforce(Bus bus);


    /** A {@link ThreadEnforcer} that does no verification. */
    ThreadEnforcer ANY = new ThreadEnforcer() {
        @Override public void enforce(Bus bus) {
            // Allow any thread.
        }
    };

    /** A {@link ThreadEnforcer} that confines {@link Bus} methods to the main thread. */
    ThreadEnforcer MAIN = new ThreadEnforcer() {
        @Override public void enforce(Bus bus) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                throw new IllegalStateException("Event bus " + bus
                        + " accessed from non-main thread " + Looper.myLooper());
            }
        }
    };

}
