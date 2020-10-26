package com.georgespear.registry;

import java.util.Optional;

import com.georgespear.strategy.Strategy;

public interface Registry<T> {

    Optional<T> getInstance(Strategy strategy);

    void register(T element);

    void unregister(T element);

    int getElementCount();

}
