package com.georgespear.balancer;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.georgespear.heartbeater.HeartbeatCallback;
import com.georgespear.provider.HeartbeatAwareServiceProvider;
import com.georgespear.provider.ServiceProvider;
import com.georgespear.registry.HeartbeatRegistry;
import com.georgespear.registry.SimpleListRegistry;
import com.georgespear.strategy.RoundRobinStrategy;
import com.georgespear.registry.Registry;
import com.georgespear.strategy.RandomStrategy;
import com.georgespear.strategy.Strategy;

public class HeartbeatingBalancer implements ServiceProvider, Registry<HeartbeatAwareServiceProvider> {

    static final Strategy RANDOM = new RandomStrategy();
    static final Strategy ROUND_ROBIN = new RoundRobinStrategy();

    private final int maxProviders;
    private final int heartbeatIntervalSeconds;
    private final int maxParallelRequests;

    private final AtomicInteger providerCount = new AtomicInteger();
    private final AtomicInteger parallelRequests = new AtomicInteger();

    private final Registry<HeartbeatAwareServiceProvider> serviceProviderRegistry;
    private final HeartbeatRegistry<HeartbeatAwareServiceProvider> heartbeatRegistry;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private volatile BalancerMode mode;
    private volatile Strategy strategy;

    public HeartbeatingBalancer(int maxProviders, int heartbeatIntervalSeconds, int recoveryHeartbeatCount, int maxParallelRequests) {
        this.maxProviders = maxProviders;
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        this.maxParallelRequests = maxParallelRequests;

        serviceProviderRegistry = new SimpleListRegistry<>();
        heartbeatRegistry = new HeartbeatRegistry<>(recoveryHeartbeatCount, new HeartbeatCallbackImpl());

        setMode(BalancerMode.RANDOM);

    }

    public void startHeartbeat() {
        executorService.scheduleAtFixedRate(heartbeatRegistry::doHeartbeat, 0, heartbeatIntervalSeconds, TimeUnit.SECONDS);
    }

    public void setMode(BalancerMode mode) {
        this.mode = mode;
        switch (mode) {
            case RANDOM: {
                strategy = RANDOM;
                break;
            }
            case ROUND_ROBIN: {
                strategy = ROUND_ROBIN;
                break;
            }
        }
    }

    public BalancerMode getMode() {
        return mode;
    }

    public String get() {
        if (parallelRequests.getAndIncrement() >= maxParallelRequests) {
            parallelRequests.getAndDecrement();
            throw new ServiceUnavailableException();
        } else {
            try {
                return getInstance(strategy)
                    .map(ServiceProvider::get)
                    .orElseThrow(ServiceUnavailableException::new);
            } finally {
                parallelRequests.getAndDecrement();
            }
        }
    }

    @Override
    public void register(HeartbeatAwareServiceProvider provider) {
        if (providerCount.get() >= maxProviders) {
            // TODO: Do some error handling here. In real life, e.g. for HTTP balancer, that would be status code 509 or 400 or 429
        } else {
            providerCount.incrementAndGet();
            serviceProviderRegistry.register(provider);
            heartbeatRegistry.register(provider);
        }
    }

    @Override
    public void unregister(HeartbeatAwareServiceProvider provider) {
        serviceProviderRegistry.unregister(provider);
        heartbeatRegistry.unregister(provider);
        providerCount.decrementAndGet();
    }

    @Override
    public int getElementCount() {
        return providerCount.get();
    }

    @Override
    public Optional<HeartbeatAwareServiceProvider> getInstance(Strategy strategy) {
        return serviceProviderRegistry.getInstance(strategy);
    }

    public class HeartbeatCallbackImpl implements HeartbeatCallback<HeartbeatAwareServiceProvider> {

        @Override
        public void onHeartbeatResults(List<HeartbeatAwareServiceProvider> wentLive, List<HeartbeatAwareServiceProvider> wentDead) {
            for (HeartbeatAwareServiceProvider deadProvider : wentDead) {
                serviceProviderRegistry.unregister(deadProvider);
            }
            for (HeartbeatAwareServiceProvider liveProvider : wentLive) {
                serviceProviderRegistry.register(liveProvider);
            }
        }
    }
}
