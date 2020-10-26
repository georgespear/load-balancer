package com.georgespear.strategy;

import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinStrategy implements Strategy {

    final AtomicInteger pointer = new AtomicInteger(0);

    @Override
    public int getIndex(int queueSize) {
        return pointer.getAndIncrement() % queueSize;
    }
}
