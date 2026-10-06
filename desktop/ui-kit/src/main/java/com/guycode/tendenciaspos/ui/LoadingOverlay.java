package com.guycode.tendenciaspos.ui;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeEvent;
import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.plaf.LayerUI;

/**
 * Velo con indicador giratorio sobre un componente mientras se espera una respuesta. Bloquea el ratón y
 * el teclado del componente cubierto.
 *
 * <pre>{@code
 * var overlay = new LoadingOverlay();
 * panel.add(overlay.wrap(contenido));
 * overlay.setActive(true);
 * }</pre>
 */
public final class LoadingOverlay extends LayerUI<JComponent> {
    private static final long serialVersionUID = 1L;
    private static final int SIZE = 36;
    private static final String TICK = "tick";

    private final Timer timer;
    private boolean active;
    private int angle;

    public LoadingOverlay() {
        timer = new Timer(40, e -> {
            angle = (angle + 12) % 360;
            firePropertyChange(TICK, false, true);
        });
    }

    public JLayer<JComponent> wrap(JComponent content) {
        return new JLayer<>(content, this);
    }

    public boolean isActive() {
        return active;
    }

    /** Muestra u oculta el velo. Llamar en el EDT. */
    public void setActive(boolean active) {
        if (this.active == active) {
            return;
        }
        this.active = active;
        if (active) {
            timer.start();
        } else {
            timer.stop();
        }
        firePropertyChange(TICK, !active, active);
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        ((JLayer<?>) c)
                .setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK
                        | AWTEvent.MOUSE_MOTION_EVENT_MASK
                        | AWTEvent.MOUSE_WHEEL_EVENT_MASK
                        | AWTEvent.KEY_EVENT_MASK);
    }

    @Override
    public void uninstallUI(JComponent c) {
        ((JLayer<?>) c).setLayerEventMask(0);
        super.uninstallUI(c);
    }

    @Override
    public void eventDispatched(AWTEvent e, JLayer<? extends JComponent> l) {
        if (active && e instanceof InputEvent input) {
            input.consume();
        }
    }

    @Override
    public void applyPropertyChange(PropertyChangeEvent evt, JLayer<? extends JComponent> l) {
        if (TICK.equals(evt.getPropertyName())) {
            l.repaint();
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);
        if (!active) {
            return;
        }
        var g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color veil = UIManager.getColor("Panel.background");
            g2.setColor(new Color(veil.getRed(), veil.getGreen(), veil.getBlue(), 180));
            g2.fillRect(0, 0, c.getWidth(), c.getHeight());

            int x = (c.getWidth() - SIZE) / 2;
            int y = (c.getHeight() - SIZE) / 2;
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(UIManager.getColor("Component.borderColor"));
            g2.drawOval(x, y, SIZE, SIZE);
            g2.setColor(UIManager.getColor("Component.accentColor"));
            g2.drawArc(x, y, SIZE, SIZE, -angle, 100);
        } finally {
            g2.dispose();
        }
    }
}
