package com.squareup.otto;

import java.lang.reflect.InvocationTargetException;

/**
 * <p>This interface represents something that an EventHandler can call whenever it is posted an Event.</p>
 *
 * @author Guillermo Gutierrez
 */
public interface Callback<T> {
    /**
     * <p>This method will be called whenever an Event is posted to the EventHandler that has an implementation of this
     * interface.</p>
     *
     * @param event event object that has been posted
     * @throws InvocationTargetException
     */
    void call(T event) throws InvocationTargetException;
}
