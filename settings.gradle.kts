plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "resources-ftp"

pluginManagement.repositories {
    gradlePluginPortal()
    mavenLocal()
}