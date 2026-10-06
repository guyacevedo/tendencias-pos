plugins { application }

dependencies {
    implementation(project(":contracts"))
    implementation(project(":desktop:ui-kit"))
    implementation(libs.jackson.databind)
}

application { mainClass = "com.guycode.tendenciaspos.desktop.DesktopApp" }
