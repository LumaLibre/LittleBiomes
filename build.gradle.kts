plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.0"
    id("io.freefair.lombok") version "9.1.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

group = "net.lumamc.biomes"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://storehouse.okaeri.eu/repository/maven-public/")
    maven("https://repo.jsinco.dev/releases")
}

dependencies {
    //compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    implementation("eu.okaeri:okaeri-configs-yaml-bukkit:5.0.5")
    implementation("me.outspending.biomesapi:BiomesAPI:0.0.17")
    implementation("com.zaxxer:HikariCP:6.3.0")
    //implementation("com.jeff-media:custom-block-data:2.2.4")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    test {
        useJUnitPlatform()
    }
//
//    jar {
//        enabled = false
//    }
//
//    build {
//        dependsOn(shadowJar)
//    }

//    shadowJar {
//        val shaded = "net.lumamc.biomes.shaded"
//        relocate("eu.okaeri", "$shaded.okaeri")
//        relocate("com.zaxxer.hikari", "$shaded.hikari")
//        relocate("me.outspending", "$shaded.biomesapi")
//        //relocate("com.jeff-media", "$shaded.customblockdata")
//        archiveClassifier.set("")
//    }

    runServer {
        minecraftVersion("1.21.11")
    }

}


java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}