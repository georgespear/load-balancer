package com.georgespear.strategy;

@FunctionalInterface
public interface Strategy {

    int getIndex(int queueSize);
}
