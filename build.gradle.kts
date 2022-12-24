import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("application")
    id("com.github.johnrengelman.shadow")
    id("com.diffplug.spotless")
}

application {
    mainClass.set("exporter.Exporter")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    implementation(libs.combo.core)
    implementation(libs.kaml)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization)
    implementation(libs.picocli)

    implementation(libs.bundles.logging)
    implementation(libs.bundles.prometheus)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs += arrayOf(
        "-opt-in=kotlin.RequiresOptIn",
        "-opt-in=kotlinx.coroutines.ObsoleteCoroutinesApi",
        "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
    )
}

testing {
    suites {
        named("test", JvmTestSuite::class) {
            useJUnitJupiter()

            dependencies {
                implementation(libs.kotest.assertions)
            }

            targets {
                all {
                    testTask.configure {
                        testLogging {
                            showStandardStreams = true
                            events("passed", "failed", "skipped")
                        }
                    }
                }
            }
        }
    }
}

spotless {
    kotlin {
        ktlint("0.47.1")
            .editorConfigOverride(
                mapOf(
                    "ktlint_disabled_rules" to "no-wildcard-imports"
                )
            )
    }
}

tasks.withType<ShadowJar> {
    isZip64 = true
}
