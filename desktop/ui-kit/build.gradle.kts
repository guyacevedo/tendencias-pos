// Tema FlatLaf y componentes Swing reutilizables.
plugins { `java-library` }

dependencies {
    api(libs.flatlaf)
    api(libs.miglayout.swing)
    api(libs.ikonli.swing)
    implementation(libs.ikonli.mdi2)
}
