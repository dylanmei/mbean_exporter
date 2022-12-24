pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    plugins {
        kotlin("jvm") version "1.7.21"
        kotlin("plugin.serialization") version "1.7.21"
        id("com.github.johnrengelman.shadow") version "7.1.2"
        id("com.diffplug.spotless") version "6.12.0"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            library("combo.core", "com.glispa.combo", "combo-core").version("1.4.1")
            library("kaml", "com.charleskorn.kaml", "kaml").version("0.43.0")
            library("kotest.assertions", "io.kotest", "kotest-assertions-core-jvm").version("5.2.3")
            library("kotlinx.coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").version("1.6.1")
            library("kotlinx.serialization", "org.jetbrains.kotlinx", "kotlinx-serialization-core").version("1.3.2")
            library("picocli", "info.picocli", "picocli").version("4.6.3")
            library("prom.httpserver", "io.prometheus", "simpleclient").version("0.15.0")
            library("prom.simpleclient", "io.prometheus", "simpleclient_httpserver").version("0.15.0")
            library("log4j", "log4j", "log4j").version("1.2.17")
            library("slf4j.log4j", "org.slf4j", "slf4j-log4j12").version("1.7.7")

            bundle("prometheus", listOf("prom.httpserver", "prom.simpleclient"))
            bundle("logging", listOf("log4j", "slf4j.log4j"))
        }
    }
}

rootProject.name = "mbean_exporter"
