plugins {
    id("maven-publish")
    id("java-gradle-plugin")
    id("com.gradleup.shadow") version "8.3.5"
}

group = "de.crazydev22"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://raw.github.com/asbachb/mvn-repo/master/releases")
}

val shadowOnly = configurations.create("shadowOnly")

dependencies {
    compileOnly("com.github.asbachb:ftp4j:1.7.3")
    compileOnly("commons-io:commons-io:2.17.0")

    shadowOnly("com.github.asbachb:ftp4j:1.7.3")
    shadowOnly("commons-io:commons-io:2.17.0")
}

gradlePlugin {
    plugins {
        register("resources-ftp") {
            id = "de.crazydev22.resources-ftp"
            implementationClass = "de.crazydev22.resourcesftp.ResourcePlugin"
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

tasks {
    shadowJar {
        archiveClassifier.set(null as String?)
        dependencies { configurations = listOf(shadowOnly) }

        relocate("org.apache.commons", "de.crazydev22.resourcesftp.commons")
        relocate("it.sauronsoftware.ftp4j", "de.crazydev22.resourcesftp.ftp4j")
    }

    named("build") {
        dependsOn(named("shadowJar"))
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}