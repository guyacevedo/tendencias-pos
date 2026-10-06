plugins { application }

dependencies {
    implementation(project(":contracts"))
    implementation(project(":desktop:ui-kit"))
    implementation(libs.jackson.databind)
}

application { mainClass = "com.guycode.tendenciaspos.desktop.DesktopApp" }

// La versión del escritorio viaja en la cabecera X-Client-Version.
tasks.processResources {
    val appVersion = project.version.toString()
    inputs.property("version", appVersion)
    filesMatching("tpos-client.properties") { expand("version" to appVersion) }
}

tasks.withType<Test>().configureEach { systemProperty("java.awt.headless", "true") }
