plugins {
    id("maven-publish")
    id("java-gradle-plugin")
    id("de.crazydev22.resources-ftp") version "1.0.0"
    kotlin("jvm") version "2.0.20"
}

group = "de.crazydev22"
version = "1.0.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // https://mvnrepository.com/artifact/commons-net/commons-net
    implementation("commons-net:commons-net:3.11.1")
    // https://mvnrepository.com/artifact/commons-io/commons-io
    implementation("commons-io:commons-io:2.17.0")
}

gradlePlugin {
    plugins {
        register("resources-ftp") {
            id = "de.crazydev22.resources-ftp"
            implementationClass = "de.crazydev22.resourcesftp.ResourcePlugin"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

publishing {
    repositories {
        mavenLocal()
        maven {
            name = "ftp"
            url = uri("ftp://138.201.31.117:21")
            credentials {
                username = "bob"
                password = "bob"
            }
        }
    }
}