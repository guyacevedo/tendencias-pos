package com.guycode.tendenciaspos.desktop.home;

import static org.assertj.core.api.Assertions.assertThat;

import com.guycode.tendenciaspos.contracts.ApiVersion;
import com.guycode.tendenciaspos.desktop.api.ApiException;
import com.guycode.tendenciaspos.desktop.core.UiExecutor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;

class HomePresenterTest {
    private final RecordingView view = new RecordingView();
    private final UiExecutor direct = new UiExecutor(Runnable::run, Runnable::run);

    @Test
    void showsBothVersions() {
        load(() -> new ApiVersion("0.2.0", "0.1.0"), "0.1.0-SNAPSHOT");

        assertThat(view.calls).containsExactly("loading:true", "loading:false", "versions:0.1.0-SNAPSHOT/0.2.0");
    }

    @Test
    void warnsWhenClientIsOlderThanServerMinimum() {
        load(() -> new ApiVersion("0.5.0", "0.4.0"), "0.3.0");

        assertThat(view.calls).endsWith("update:0.4.0");
    }

    @Test
    void connectivityErrorShowsFriendlyMessage() {
        load(
                () -> {
                    throw new ApiException(ApiException.NETWORK_ERROR, "detalle técnico", 0);
                },
                "0.1.0");

        assertThat(view.calls)
                .containsExactly("loading:true", "loading:false", "error:No se pudo contactar al servidor.");
    }

    @Test
    void businessErrorShowsServerMessage() {
        load(
                () -> {
                    throw new ApiException("FORBIDDEN", "Sin permiso.", 403);
                },
                "0.1.0");

        assertThat(view.calls).endsWith("error:Sin permiso.");
    }

    private void load(Callable<ApiVersion> loader, String clientVersion) {
        new HomePresenter(view, loader, direct, clientVersion).load();
    }

    private static final class RecordingView implements HomeView {
        final List<String> calls = new ArrayList<>();

        @Override
        public void showLoading(boolean loading) {
            calls.add("loading:" + loading);
        }

        @Override
        public void showVersions(String clientVersion, String serverVersion) {
            calls.add("versions:" + clientVersion + "/" + serverVersion);
        }

        @Override
        public void showUpdateRequired(String minClientVersion) {
            calls.add("update:" + minClientVersion);
        }

        @Override
        public void showError(String message) {
            calls.add("error:" + message);
        }
    }
}
