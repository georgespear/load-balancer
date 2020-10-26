package com.georgespear.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.georgespear.heartbeater.HeartbeatAware;
import com.georgespear.heartbeater.HeartbeatCallback;
import com.georgespear.strategy.Strategy;

public class HeartbeatRegistry<T extends HeartbeatAware> implements Registry<T> {

    private final int recoveryHeartbeatCount;
    private final HeartbeatCallback<T> callback;
    private final ConcurrentHashMap<T, Integer> heartbeatSuccesses = new ConcurrentHashMap<>();

    public HeartbeatRegistry(int recoveryHeartbeatCount, HeartbeatCallback<T> callback) {
        this.recoveryHeartbeatCount = recoveryHeartbeatCount;
        this.callback = callback;
    }

    @Override
    public Optional<T> getInstance(Strategy strategy) {
        return Optional.empty();
    }

    @Override
    public void register(T element) {
        heartbeatSuccesses.put(element, recoveryHeartbeatCount);
    }

    @Override
    public void unregister(T element) {
        heartbeatSuccesses.remove(element);
    }

    @Override
    public int getElementCount() {
        return heartbeatSuccesses.size();
    }

    public void doHeartbeat() {
        List<T> wentLive = new ArrayList<>();
        List<T> wentDead = new ArrayList<>();

        for (Map.Entry<T, Integer> entry : heartbeatSuccesses.entrySet()) {
            T provider = entry.getKey();
            Integer previousSuccesses = entry.getValue();
            if (provider.check()) {
                if (previousSuccesses + 1 == recoveryHeartbeatCount) {
                    wentLive.add(provider);
                    entry.setValue(recoveryHeartbeatCount);
                } else if (previousSuccesses + 1 < recoveryHeartbeatCount) {
                    entry.setValue(previousSuccesses + 1);
                }
            } else {
                wentDead.add(provider);
                entry.setValue(0);
            }
        }
        callback.onHeartbeatResults(wentLive, wentDead);

    }
}
