package com.guycode.tendenciaspos.desktop.api;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Estado de la conexión con la API. Avisa a los oyentes solo cuando cambia y, mientras no hay conexión,
 * ejecuta una sonda periódica hasta que el servidor vuelva a responder.
 */
public final class ConnectionMonitor implements ApiClient.ConnectionObserver, AutoCloseable {
    private final Duration retryInterval;
    private final List<Consumer<Boolean>> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean online = new AtomicBoolean(true);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("tpos-connection").factory());
    private volatile Runnable probe = () -> {};
    private ScheduledFuture<?> probing;

    public ConnectionMonitor(Duration retryInterval) {
        this.retryInterval = retryInterval;
    }

    public Duration retryInterval() {
        return retryInterval;
    }

    /** Llamada de prueba (normalmente {@code GET /api/version}) que se repite mientras no hay conexión. */
    public void setProbe(Runnable probe) {
        this.probe = probe;
    }

    /** El oyente recibe {@code true} al recuperar la conexión y {@code false} al perderla, fuera del EDT. */
    public void addListener(Consumer<Boolean> listener) {
        listeners.add(listener);
    }

    public boolean isOnline() {
        return online.get();
    }

    @Override
    public void reachable() {
        if (online.compareAndSet(false, true)) {
            stopProbing();
            listeners.forEach(l -> l.accept(true));
        }
    }

    @Override
    public void unreachable() {
        if (online.compareAndSet(true, false)) {
            startProbing();
            listeners.forEach(l -> l.accept(false));
        }
    }

    private synchronized void startProbing() {
        if (probing == null) {
            long ms = retryInterval.toMillis();
            probing = scheduler.scheduleWithFixedDelay(this::runProbe, ms, ms, TimeUnit.MILLISECONDS);
        }
    }

    private synchronized void stopProbing() {
        if (probing != null) {
            probing.cancel(false);
            probing = null;
        }
    }

    private void runProbe() {
        try {
            probe.run();
        } catch (RuntimeException e) {
            // Sigue sin conexión: se reintenta en el siguiente ciclo.
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
