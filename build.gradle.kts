// Convenciones comunes a todos los módulos Java (sin build-logic para evitar kotlin-dsl).
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spotless) apply false
}

val catalog = libs

subprojects {
    pluginManager.withPlugin("java") {
        apply(plugin = "com.diffplug.spotless")

        extensions.configure<JavaPluginExtension> {
            toolchain { languageVersion = JavaLanguageVersion.of(catalog.versions.java.get()) }
        }

        dependencies {
            "testImplementation"(platform(catalog.junit.bom))
            "testImplementation"("org.junit.jupiter:junit-jupiter")
            "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
            "testImplementation"(catalog.assertj)
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.compilerArgs.addAll(listOf("-Xlint:all,-serial,-processing", "-Werror", "-parameters"))
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            testLogging { events("failed"); exceptionFormat = TestExceptionFormat.SHORT }
        }

        extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
            java {
                palantirJavaFormat(catalog.versions.palantir.format.get())
                removeUnusedImports()
                trimTrailingWhitespace()
                endWithNewline()
            }
        }
    }
}
