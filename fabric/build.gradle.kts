plugins {
    id("java")
    id("fabric-loom")
}

repositories {
    mavenCentral()
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
    maven {
        name = "PaperMC"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

loom {
    splitEnvironmentSourceSets()
    mods {
        create("zones") {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    implementation(project(":api"))

    minecraft("com.mojang:minecraft:${rootProject.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())

    modImplementation(compileOnly("net.fabricmc:fabric-loader:${rootProject.property("loader_version")}")!!)

    val fabricVersion = rootProject.property("fabric_version") as String
    modImplementation(fabricApi.module("fabric-lifecycle-events-v1", fabricVersion))
    modImplementation(fabricApi.module("fabric-command-api-v2", fabricVersion))
    modImplementation(fabricApi.module("fabric-events-interaction-v0", fabricVersion))

    modImplementation(include("me.lucko:fabric-permissions-api:0.3.1")!!)
    modImplementation(include("net.kyori:adventure-platform-fabric:6.2.0")!!)
    implementation("net.kyori:adventure-text-minimessage:4.19.0")
}

tasks.processResources {
    val props = mapOf("version" to rootProject.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}

tasks.test {
    useJUnitPlatform()
}