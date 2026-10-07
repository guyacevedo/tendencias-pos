package com.guycode.tendenciaspos.desktop.home;

import com.guycode.tendenciaspos.contracts.ApiVersion;
import com.guycode.tendenciaspos.desktop.core.ApiErrors;
import com.guycode.tendenciaspos.desktop.core.UiExecutor;
import com.guycode.tendenciaspos.desktop.core.Versions;
import java.util.concurrent.Callable;

/** Consulta la versión del servidor y la muestra junto a la del escritorio. */
public final class HomePresenter {
    private final HomeView view;
    private final Callable<ApiVersion> versionLoader;
    private final UiExecutor executor;
    private final String clientVersion;

    public HomePresenter(HomeView view, Callable<ApiVersion> versionLoader, UiExecutor executor, String clientVersion) {
        this.view = view;
        this.versionLoader = versionLoader;
        this.executor = executor;
        this.clientVersion = clientVersion;
    }

    public void load() {
        view.showLoading(true);
        executor.submit(versionLoader, this::onVersion, this::onError);
    }

    private void onVersion(ApiVersion version) {
        view.showLoading(false);
        view.showVersions(clientVersion, version.serverVersion());
        if (Versions.isOlder(clientVersion, version.minClientVersion())) {
            view.showUpdateRequired(version.minClientVersion());
        }
    }

    private void onError(Exception error) {
        view.showLoading(false);
        view.showError(ApiErrors.messageFor(error));
    }
}
