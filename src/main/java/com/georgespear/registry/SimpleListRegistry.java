package com.georgespear.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.georgespear.strategy.Strategy;

/**
 * List-based registry of providers.
 *
 * @param <T>
 */
public class SimpleListRegistry<T> implements Registry<T> {

    volatile List<T> queue = new ArrayList<>();
    private final Object registrationLock = new Object();

    public Optional<T> getInstance(Strategy strategy) {
        List<T> temp = queue;
        if (temp.size() == 0) {
            return Optional.empty();
        }
        int index = strategy.getIndex(temp.size());
        return Optional.of(temp.get(index));
    }

    public void register(T provider) {
        synchronized (registrationLock) {
            if (!queue.contains(provider)) {
                List<T> newQueue = new ArrayList<>(queue);
                newQueue.add(provider);
                queue = newQueue;
            }
        }
    }

    public void unregister(T provider) {
        synchronized (registrationLock) {
            if (queue.contains(provider)) {
                List<T> newQueue = new ArrayList<>(queue);
                newQueue.remove(provider);
                queue = newQueue;
            }
        }
    }

    @Override
    public int getElementCount() {
        return queue.size();
    }
}
