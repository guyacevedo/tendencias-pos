package com.guycode.tendenciaspos.desktop.shell;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;

/**
 * Ventana principal: barra de desconexión arriba, menú lateral y área de contenido con
 * {@link CardLayout}. Por debajo de {@value #COLLAPSE_BELOW} px de ancho el menú muestra solo iconos.
 */
public final class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    static final int COLLAPSE_BELOW = 1100;

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final SideMenu menu = new SideMenu(this::navigate);
    private Boolean narrow;

    public MainFrame(Map<Route, ? extends JComponent> screens, OfflineBar offlineBar) {
        super("Tendencias POS");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        for (var route : Route.values()) {
            JComponent screen = screens.containsKey(route) ? screens.get(route) : new PlaceholderPanel(route);
            content.add(screen, route.name());
        }

        var root = new JPanel(new MigLayout("fill, insets 0, gap 0", "[][grow, fill]", "[][grow, fill]"));
        root.add(offlineBar, "span 2, growx, wrap, hidemode 3");
        root.add(menu, "growy");
        root.add(content, "grow");
        setContentPane(root);

        setMinimumSize(new Dimension(800, 560));
        setSize(1280, 800);
        setLocationRelativeTo(null);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adaptToWidth();
            }
        });
        adaptToWidth();
    }

    /** Muestra la pantalla de la ruta y marca su entrada en el menú. Llamar en el EDT. */
    public void navigate(Route route) {
        cards.show(content, route.name());
        menu.select(route);
        setTitle("Tendencias POS — " + route.title());
    }

    /** Contrae o expande el menú solo al cruzar el umbral, para respetar el cambio manual del usuario. */
    private void adaptToWidth() {
        boolean nowNarrow = getWidth() < COLLAPSE_BELOW;
        if (narrow == null || narrow != nowNarrow) {
            narrow = nowNarrow;
            menu.setCollapsed(nowNarrow);
        }
    }
}
