package com.guycode.tendenciaspos.desktop.shell;

import com.guycode.tendenciaspos.contracts.identity.SessionUser;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import net.miginfocom.swing.MigLayout;

/**
 * Ventana principal: barra de desconexión arriba, menú lateral con las rutas del rol y área de
 * contenido con {@link CardLayout}. Por debajo de {@value #COLLAPSE_BELOW} px el menú muestra solo
 * iconos.
 */
public final class MainFrame extends JFrame implements OfflineAware {
    private static final long serialVersionUID = 1L;
    static final int COLLAPSE_BELOW = 1100;

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final OfflineBar offlineBar;
    private final SideMenu menu;
    private final Map<Route, Runnable> onShow = new EnumMap<>(Route.class);
    private Boolean narrow;

    public MainFrame(
            Map<Route, ? extends JComponent> screens, SessionUser user, Duration retryInterval, Runnable onLogout) {
        super("Tendencias POS");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        var routes = Route.visibleFor(user.roles());
        for (var route : routes) {
            JComponent screen = screens.containsKey(route) ? screens.get(route) : new PlaceholderPanel(route);
            content.add(screen, route.name());
        }
        this.offlineBar = new OfflineBar(retryInterval);
        this.menu = new SideMenu(routes, user.fullName(), this::navigate, onLogout);

        var root = new JPanel(new MigLayout("fill, insets 0, gap 0", "[][grow, fill]", "[][grow, fill]"));
        root.add(offlineBar, "span 2, growx, wrap, hidemode 3");
        root.add(menu, "growy, h 100%");
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
        navigate(routes.getFirst());
    }

    /** Acción que se ejecuta cada vez que se entra a la ruta (normalmente recargar sus datos). */
    public void whenShown(Route route, Runnable action) {
        onShow.put(route, action);
    }

    /** Muestra la pantalla de la ruta y marca su entrada en el menú. Llamar en el EDT. */
    public void navigate(Route route) {
        cards.show(content, route.name());
        menu.select(route);
        setTitle("Tendencias POS — " + route.title());
        var action = onShow.get(route);
        if (action != null) {
            action.run();
        }
    }

    @Override
    public void setOffline(boolean offline) {
        offlineBar.setOffline(offline);
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
