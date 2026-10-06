package com.guycode.tendenciaspos.desktop.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ConnectionMonitorTest {

    @Test
    void notifiesOnlyWhenStateChanges() {
        List<Boolean> events = new CopyOnWriteArrayList<>();
        try (var monitor = new ConnectionMonitor(Duration.ofHours(1))) {
            monitor.addListener(events::add);

            monitor.reachable();
            monitor.unreachable();
            monitor.unreachable();
            monitor.reachable();

            assertThat(events).containsExactly(false, true);
            assertThat(monitor.isOnline()).isTrue();
        }
    }

    @Test
    void probesWhileOfflineAndStopsWhenServerAnswers() throws InterruptedException {
        var calls = new AtomicInteger();
        var recovered = new CountDownLatch(1);
        try (var monitor = new ConnectionMonitor(Duration.ofMillis(20))) {
            monitor.setProbe(() -> {
                if (calls.incrementAndGet() < 3) {
                    throw new ApiException(ApiException.NETWORK_ERROR, "sin red", 0);
                }
                monitor.reachable();
            });
            monitor.addListener(online -> {
                if (online) {
                    recovered.countDown();
                }
            });

            monitor.unreachable();

            assertThat(recovered.await(2, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(100);
            assertThat(calls.get()).isEqualTo(3);
        }
    }
}
