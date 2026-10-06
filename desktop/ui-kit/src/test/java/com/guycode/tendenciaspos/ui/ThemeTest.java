package com.guycode.tendenciaspos.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;

class ThemeTest {

    @Test
    void eachModeKeepsOnlyItsOwnPrefixedKeys() {
        var light = Theme.defaultsFor(Theme.Mode.LIGHT);
        var dark = Theme.defaultsFor(Theme.Mode.DARK);

        assertThat(light).containsEntry("@accentColor", "#C2410C").containsEntry("Button.arc", "8");
        assertThat(dark).containsEntry("@accentColor", "#FB923C").containsEntry("Button.arc", "8");
        assertThat(light.keySet()).noneMatch(k -> k.startsWith("["));
    }

    @Test
    void applyInstallsBrandColors() {
        Theme.apply(Theme.Mode.DARK);
        assertThat(UIManager.getColor("Component.accentColor")).isEqualTo(new Color(0xFB923C));
        assertThat(UIManager.getColor("Tpos.dangerColor")).isEqualTo(new Color(0xDC2626));

        Theme.apply(Theme.Mode.LIGHT);
        assertThat(UIManager.getColor("Component.accentColor")).isEqualTo(new Color(0xC2410C));
        assertThat(Theme.current()).isEqualTo(Theme.Mode.LIGHT);
    }
}
