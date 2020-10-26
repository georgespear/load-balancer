package com.georgespear.provider;

import java.util.concurrent.atomic.AtomicInteger;

public class SimpleServiceProvider implements HeartbeatAwareServiceProvider {

    static AtomicInteger counter = new AtomicInteger(0);
    private final String id;

    public SimpleServiceProvider() {
        this.id = String.format("Hello from provider %d", counter.incrementAndGet());
    }

    public String get() {
        return id;
    }

    public boolean check() {
        return true;
    }
}
