package com.georgespear.registry;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.georgespear.heartbeater.HeartbeatCallback;
import com.georgespear.provider.SimpleServiceProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HeartbeatRegistryTest {

    @Captor
    ArgumentCaptor<List<SimpleServiceProvider>> liveProvidersArgumentCaptor;
    @Captor
    ArgumentCaptor<List<SimpleServiceProvider>> deadProvidersArgumentCaptor;

    @Mock
    private HeartbeatCallback<SimpleServiceProvider> mockCallback;

    private HeartbeatRegistry<SimpleServiceProvider> registry;

    int recoveryHeartbeatCount = 2;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        registry = Mockito.spy(new HeartbeatRegistry<SimpleServiceProvider>(recoveryHeartbeatCount, mockCallback));

    }

    @Test
    public void dead_reported() {

        SimpleServiceProvider provider = spy(new SimpleServiceProvider());
        registry.register(provider);

        when(provider.check()).thenReturn(false);
        registry.doHeartbeat();

        verify(mockCallback).onHeartbeatResults(liveProvidersArgumentCaptor.capture(), deadProvidersArgumentCaptor.capture());

        assertThat(deadProvidersArgumentCaptor.getValue()).containsExactly(provider);
        assertThat(liveProvidersArgumentCaptor.getValue()).isEmpty();
    }

    @Test
    public void live_reported() {

        SimpleServiceProvider provider = spy(new SimpleServiceProvider());
        registry.register(provider);

        when(provider.check()).thenReturn(false);
        registry.doHeartbeat();

        when(provider.check()).thenReturn(true);
        for (int i = 0; i < recoveryHeartbeatCount; i++) {
            registry.doHeartbeat();
        }

        verify(mockCallback, times(recoveryHeartbeatCount + 1))
            .onHeartbeatResults(liveProvidersArgumentCaptor.capture(), deadProvidersArgumentCaptor.capture());

        // First heartbeat reports provider is dead
        assertThat(deadProvidersArgumentCaptor.getAllValues().get(0)).containsExactly(provider);
        assertThat(liveProvidersArgumentCaptor.getAllValues().get(0)).isEmpty();

        // Upto recoveryHeartbeatCount-1 heartbeat don't report anything
        for (int i = 0; i < recoveryHeartbeatCount - 1; i++) {
            assertThat(deadProvidersArgumentCaptor.getAllValues().get(1)).isEmpty();
            assertThat(liveProvidersArgumentCaptor.getAllValues().get(1)).isEmpty();

        }

        // Last heartbeat reports provider is back
        assertThat(deadProvidersArgumentCaptor.getAllValues().get(recoveryHeartbeatCount)).isEmpty();
        assertThat(liveProvidersArgumentCaptor.getAllValues().get(recoveryHeartbeatCount)).containsExactly(provider);

    }

}