package com.guycode.tendenciaspos.desktop.home;

import com.formdev.flatlaf.FlatClientProperties;
import com.guycode.tendenciaspos.ui.Icons;
import com.guycode.tendenciaspos.ui.LoadingOverlay;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;

/** Pantalla de inicio: identidad de la app y versiones de escritorio y servidor. */
public final class HomePanel extends JPanel implements HomeView {
    private static final long serialVersionUID = 1L;
    private static final String PENDING = "—";

    private final LoadingOverlay overlay = new LoadingOverlay();
    private final JLabel clientValue = new JLabel(PENDING);
    private final JLabel serverValue = new JLabel(PENDING);
    private final JLabel status = new JLabel();
    private final JButton retry = new JButton("Reintentar", Icons.of(MaterialDesignR.REFRESH, Icons.SMALL));

    public HomePanel() {
        super(new MigLayout("fill, insets 32", "[center]", "[center]"));

        var card = new JPanel(new MigLayout("wrap 2, insets 28, gap 10 8", "[][grow, fill, 220::]", ""));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: $Tpos.sidebarBackground");

        var title = new JLabel("Tendencias POS");
        title.putClientProperty(FlatClientProperties.STYLE_CLASS, "h1");
        card.add(title, "span 2");
        var subtitle = new JLabel("Punto de venta de calzado");
        subtitle.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.disabledForeground");
        card.add(subtitle, "span 2, gapbottom 8");
        card.add(new JSeparator(), "span 2, growx, gapbottom 8");

        card.add(caption("Escritorio"));
        card.add(clientValue);
        card.add(caption("Servidor"));
        card.add(serverValue);

        status.setIconTextGap(8);
        status.setVisible(false);
        card.add(status, "span 2, gaptop 8, hidemode 3");
        retry.setVisible(false);
        card.add(retry, "span 2, alignx left, growx 0, hidemode 3");

        add(overlay.wrap(card));
    }

    public void onRetry(Runnable action) {
        retry.addActionListener(e -> action.run());
    }

    @Override
    public void showLoading(boolean loading) {
        overlay.setActive(loading);
        if (loading) {
            status.setVisible(false);
            retry.setVisible(false);
        }
    }

    @Override
    public void showVersions(String clientVersion, String serverVersion) {
        clientValue.setText(clientVersion);
        serverValue.setText(serverVersion);
    }

    @Override
    public void showUpdateRequired(String minClientVersion) {
        showStatus("Actualice el escritorio: el servidor exige la versión " + minClientVersion + " o superior.");
    }

    @Override
    public void showError(String message) {
        serverValue.setText(PENDING);
        showStatus(message);
        retry.setVisible(true);
    }

    private void showStatus(String message) {
        status.setText(message);
        status.setIcon(Icons.of(MaterialDesignA.ALERT_OUTLINE, Icons.SMALL, "Tpos.dangerColor"));
        status.putClientProperty(FlatClientProperties.STYLE, "foreground: $Tpos.dangerColor");
        status.setVisible(true);
        revalidate();
    }

    private static JLabel caption(String text) {
        var label = new JLabel(text);
        label.putClientProperty(FlatClientProperties.STYLE, "font: semibold");
        return label;
    }
}
