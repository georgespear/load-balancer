package com.georgespear.balancer;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import com.georgespear.provider.HeartbeatAwareServiceProvider;
import com.georgespear.strategy.Strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HeartbeatingBalancerTest {

    private static final int MAX_PROVIDERS = 10;
    private static final int MAX_PARALLEL_REQUESTS = 10;

    HeartbeatingBalancer balancer;

    @Before
    public void setup() {
        balancer = Mockito.spy(new HeartbeatingBalancer(MAX_PROVIDERS, 4, 2, MAX_PARALLEL_REQUESTS));
    }

    @Test(expected = ServiceUnavailableException.class)
    public void does_not_serve_if_no_providers_registered() {
        balancer.get();
    }

    @Test
    public void serves_with_providers_registered() {
        HeartbeatAwareServiceProvider mockProvider = mock(HeartbeatAwareServiceProvider.class);
        when(mockProvider.get()).thenReturn("Hello from serves_with_providers_registered");
        balancer.register(mockProvider);
        assertThat(balancer.get()).isEqualTo("Hello from serves_with_providers_registered");
    }

    @Test
    public void set_random_mode() {
        ArgumentCaptor<Strategy> strategyArgumentCaptor = ArgumentCaptor.forClass(Strategy.class);

        HeartbeatAwareServiceProvider mockProvider = mock(HeartbeatAwareServiceProvider.class);
        when(mockProvider.get()).thenReturn("Hello");
        when(balancer.getInstance(any())).thenReturn(Optional.of(mockProvider));

        balancer.setMode(BalancerMode.RANDOM);
        balancer.get();
        verify(balancer).getInstance(strategyArgumentCaptor.capture());

        assertThat(strategyArgumentCaptor.getValue()).isEqualTo(HeartbeatingBalancer.RANDOM);

    }

    @Test
    public void set_round_robin_mode() {
        ArgumentCaptor<Strategy> strategyArgumentCaptor = ArgumentCaptor.forClass(Strategy.class);

        HeartbeatAwareServiceProvider mockProvider = mock(HeartbeatAwareServiceProvider.class);
        when(mockProvider.get()).thenReturn("Hello");
        when(balancer.getInstance(any())).thenReturn(Optional.of(mockProvider));

        balancer.setMode(BalancerMode.ROUND_ROBIN);
        balancer.get();
        verify(balancer).getInstance(strategyArgumentCaptor.capture());

        assertThat(strategyArgumentCaptor.getValue()).isEqualTo(HeartbeatingBalancer.ROUND_ROBIN);

    }

    @Test
    public void does_not_register_more_than_max_providers() {
        for (int i = 1; i <= MAX_PROVIDERS; i++) {
            balancer.register(mock(HeartbeatAwareServiceProvider.class));
            assertThat(balancer.getElementCount()).isEqualTo(i);
        }
        balancer.register(mock(HeartbeatAwareServiceProvider.class));
        assertThat(balancer.getElementCount()).isEqualTo(MAX_PROVIDERS);
    }

    @Test
    public void does_not_serve_more_than_MAX_parallel_requests() throws InterruptedException {
        HeartbeatAwareServiceProvider mockProvider = mock(HeartbeatAwareServiceProvider.class);

        final int EXTRA_REQUESTS_COUNT = 4;
        CountDownLatch startRequestingSignal = new CountDownLatch(1);

        CountDownLatch startServingSignal = new CountDownLatch(MAX_PARALLEL_REQUESTS + EXTRA_REQUESTS_COUNT);
        CountDownLatch doneServingSignal = new CountDownLatch(MAX_PARALLEL_REQUESTS + EXTRA_REQUESTS_COUNT);

        when(mockProvider.get()).thenAnswer((Answer<String>) invocation -> {
            startServingSignal.countDown();
            startServingSignal.await();
            return "Hello from does_not_serve_more_than_Y_parallel_requests";
        });
        balancer.register(mockProvider);

        AtomicInteger successCounter = new AtomicInteger();
        AtomicInteger errorCounter = new AtomicInteger();

        for (int i = 1; i <= MAX_PARALLEL_REQUESTS + EXTRA_REQUESTS_COUNT; i++) {
            new Thread(new BalancerClient(startRequestingSignal, startServingSignal, doneServingSignal, successCounter, errorCounter)).start();
        }
        startRequestingSignal.countDown();

        boolean doneServing = doneServingSignal.await(4, TimeUnit.SECONDS);

        assertThat(doneServing).isEqualTo(true);
        assertThat(successCounter.get()).isEqualTo(MAX_PARALLEL_REQUESTS);
        assertThat(errorCounter.get()).isEqualTo(EXTRA_REQUESTS_COUNT);

    }

    private class BalancerClient implements Runnable {

        private final CountDownLatch startRequestingSignal;
        private final CountDownLatch startServingSignal;
        private final CountDownLatch doneServingSignal;
        private final AtomicInteger successCounter;
        private final AtomicInteger errorCounter;

        BalancerClient(CountDownLatch startRequestingSignal, CountDownLatch startServingSignal, CountDownLatch doneServingSignal, AtomicInteger successCounter,
            AtomicInteger errorCounter) {
            this.startRequestingSignal = startRequestingSignal;
            this.startServingSignal = startServingSignal;
            this.doneServingSignal = doneServingSignal;
            this.successCounter = successCounter;
            this.errorCounter = errorCounter;
        }

        public void run() {
            try {
                startRequestingSignal.await();
                doWork();
                doneServingSignal.countDown();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        void doWork() {
            try {
                balancer.get();
                successCounter.incrementAndGet();
            } catch (Exception ex) {
                startServingSignal.countDown();
                errorCounter.incrementAndGet();
            }
        }
    }
}