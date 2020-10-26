package com.georgespear.heartbeater;

import java.util.List;

public interface HeartbeatCallback<T extends HeartbeatAware> {

    void onHeartbeatResults(List<T> wentLive, List<T> wentDead);
}
