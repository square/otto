package com.squareup.otto;

public interface OttoBus {
    void register(Object object);
    void unregister(Object object);
    void post(Object event);
}
