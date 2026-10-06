// Solo Maven Central: el portal de plugins de Gradle no es necesario.
pluginManagement { repositories { mavenCentral() } }
dependencyResolutionManagement { repositories { mavenCentral() } }

rootProject.name = "tendencias-pos"

include("contracts", "server", "desktop:ui-kit", "desktop:app")
