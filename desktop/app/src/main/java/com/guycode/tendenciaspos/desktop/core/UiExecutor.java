package com.guycode.tendenciaspos.desktop.core;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

/**
 * Ejecuta trabajo lento (llamadas a la API) en hilos virtuales y entrega el resultado o el error en el
 * EDT. Los presentadores lo reciben por constructor; en pruebas se usan ejecutores directos.
 */
public final class UiExecutor implements AutoCloseable {
    private final Executor background;
    private final Executor ui;

    public UiExecutor() {
        this(Executors.newVirtualThreadPerTaskExecutor(), SwingUtilities::invokeLater);
    }

    public UiExecutor(Executor background, Executor ui) {
        this.background = background;
        this.ui = ui;
    }

    public <T> void submit(Callable<T> task, Consumer<? super T> onSuccess, Consumer<? super Exception> onError) {
        background.execute(() -> {
            T result;
            try {
                result = task.call();
            } catch (Exception e) {
                ui.execute(() -> onError.accept(e));
                return;
            }
            ui.execute(() -> onSuccess.accept(result));
        });
    }

    @Override
    public void close() {
        if (background instanceof ExecutorService service) {
            service.shutdownNow();
        }
    }
}
