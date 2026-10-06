// Tema FlatLaf y componentes Swing reutilizables.
plugins { `java-library` }

dependencies {
    api(libs.flatlaf)
    api(libs.miglayout.swing)
    api(libs.ikonli.swing)
    api(libs.ikonli.mdi2)
    implementation(libs.flatlaf.extras)
    implementation(libs.flatlaf.fonts.inter)
}

tasks.withType<Test>().configureEach { systemProperty("java.awt.headless", "true") }
