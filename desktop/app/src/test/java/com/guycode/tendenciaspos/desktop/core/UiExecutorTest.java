package com.guycode.tendenciaspos.desktop.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class UiExecutorTest {
    private final AtomicReference<Thread> worker = new AtomicReference<>();
    private final AtomicReference<Object> delivered = new AtomicReference<>();
    private final CountDownLatch done = new CountDownLatch(1);

    @Test
    void runsTaskInVirtualThreadAndDeliversResultOnUiExecutor() throws InterruptedException {
        var uiThread = Executors.newSingleThreadExecutor();
        try (var executor = new UiExecutor(Executors.newVirtualThreadPerTaskExecutor(), uiThread)) {
            executor.submit(
                    () -> {
                        worker.set(Thread.currentThread());
                        return 42;
                    },
                    value -> {
                        delivered.set(value);
                        done.countDown();
                    },
                    error -> done.countDown());

            assertThat(done.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(worker.get().isVirtual()).isTrue();
            assertThat(delivered.get()).isEqualTo(42);
        } finally {
            uiThread.shutdownNow();
        }
    }

    @Test
    void routesExceptionsToErrorHandler() {
        var executor = new UiExecutor(Runnable::run, Runnable::run);

        executor.submit(
                () -> {
                    throw new IllegalStateException("falló");
                },
                value -> delivered.set("éxito"),
                delivered::set);

        assertThat(delivered.get()).isInstanceOf(IllegalStateException.class);
    }
}
