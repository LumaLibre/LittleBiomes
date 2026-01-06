plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.0"
    id("io.freefair.lombok") version "9.1.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

group = "dev.lumas.biomes"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://storehouse.okaeri.eu/repository/maven-public/")
    maven("https://repo.jsinco.dev/releases")
}

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    implementation("eu.okaeri:okaeri-configs-yaml-bukkit:5.0.5")
    implementation("me.outspending.biomesapi:BiomesAPI:1.0.2-932c824")

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
        relocate("me.outspending", "$shaded.biomesapi")
        archiveClassifier.set("")
    }

    runServer {
        minecraftVersion("1.21.11")
        downloadPlugins {
            modrinth("plugmanx", "3.0.2")
        }
    }

}


java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}