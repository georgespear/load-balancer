package com.georgespear.registry;

import org.junit.Before;
import org.junit.Test;

import com.georgespear.provider.SimpleServiceProvider;
import com.georgespear.strategy.RoundRobinStrategy;

import static org.assertj.core.api.Assertions.*;

public class SimpleListRegistryTest {

    SimpleListRegistry<SimpleServiceProvider> registry;

    @Before
    public void setup() {
        registry = new SimpleListRegistry<>();
    }

    @Test
    public void can_add_provider_only_once() {
        SimpleServiceProvider provider = new SimpleServiceProvider();

        registry.register(provider);
        registry.register(provider);

        assertThat(registry.queue.size()).isEqualTo(1);
    }

    @Test
    public void can_remove_provider_only_once() {
        SimpleServiceProvider provider = new SimpleServiceProvider();

        registry.register(provider);
        assertThat(registry.queue.size()).isEqualTo(1);

        registry.unregister(provider);
        assertThat(registry.queue.size()).isEqualTo(0);

        registry.unregister(provider);
        assertThat(registry.queue.size()).isEqualTo(0);
    }

    @Test
    public void registered_provider_is_serving() {
        SimpleServiceProvider provider = new SimpleServiceProvider();

        registry.register(provider);
        assertThat(registry.getInstance(size -> 0)).hasValue(provider);
    }

    @Test
    public void unregistered_provider_is_not_serving_single_provider() {
        SimpleServiceProvider provider = new SimpleServiceProvider();

        registry.register(provider);
        assertThat(registry.getInstance(size -> 0)).hasValue(provider);

        registry.unregister(provider);
        assertThat(registry.getInstance(size -> 0)).isEmpty();
    }

    @Test
    public void unregistered_provider_is_not_serving_multiple_provider() {
        SimpleServiceProvider provider1 = new SimpleServiceProvider();
        SimpleServiceProvider provider2 = new SimpleServiceProvider();

        registry.register(provider1);
        registry.register(provider2);
        assertThat(registry.queue.size()).isEqualTo(2);

        registry.unregister(provider1);
        assertThat(registry.queue.size()).isEqualTo(1);

        for (int i = 0; i < 100; i++) {
            assertThat(registry.getInstance(size -> 0)).hasValue(provider2);
        }
    }

    @Test
    public void unregistered_provider_is_unqueued() {
        SimpleServiceProvider provider1 = new SimpleServiceProvider();
        SimpleServiceProvider provider2 = new SimpleServiceProvider();

        registry.register(provider1);
        registry.register(provider2);
        assertThat(registry.queue.size()).isEqualTo(2);

        registry.unregister(provider1);
        assertThat(registry.queue.size()).isEqualTo(1);

    }

    @Test
    public void provider_served_in_round_robin() {
        SimpleServiceProvider provider1 = new SimpleServiceProvider();
        SimpleServiceProvider provider2 = new SimpleServiceProvider();
        RoundRobinStrategy roundRobinStrategy = new RoundRobinStrategy();

        registry.register(provider1);
        registry.register(provider2);
        assertThat(registry.queue.size()).isEqualTo(2);

        for (int i = 0; i < 100; i++) {
            assertThat(registry.getInstance(roundRobinStrategy)).hasValue(provider1);
            assertThat(registry.getInstance(roundRobinStrategy)).hasValue(provider2);
        }
    }
}
