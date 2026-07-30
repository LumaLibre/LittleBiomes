plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.0"
    id("io.freefair.lombok") version "9.1.0"
    id("xyz.jpenilla.run-paper") version "3.0.1"
}

group = "dev.lumas.biomes"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://storehouse.okaeri.eu/repository/maven-public/")
    maven("https://repo.wyck.dev/public/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation("eu.okaeri:okaeri-configs-yaml-bukkit:5.0.5")
    implementation("dev.wyck:wyck:3.3.0-4e557c5")

    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9-beta1")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    test {
        useJUnitPlatform()
    }

    jar {
        enabled = false
    }

    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        val shaded = "dev.lumas.biomes.shaded"
        relocate("eu.okaeri", "$shaded.okaeri")
        relocate("dev.wyck", "$shaded.wyck")
        archiveClassifier.set("")
    }

    runServer {
        minecraftVersion("26.1.2")
        downloadPlugins {
            modrinth("plugmanx", "3.0.2")
        }
    }

}


java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}