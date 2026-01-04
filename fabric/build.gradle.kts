plugins {
    id("java")
    id("fabric-loom")
    alias(libs.plugins.shadow)
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

// Dedicated server only: exclude client rendering deps that fail remapping under Mojmap.
configurations.configureEach {
    exclude(group = "net.fabricmc.fabric-api", module = "fabric-rendering-v1")
    exclude(group = "net.fabricmc.fabric-api", module = "fabric-rendering-data-attachment-v1")
    exclude(group = "net.fabricmc.fabric-api", module = "fabric-renderer-indigo")
    exclude(group = "net.fabricmc.fabric-api", module = "fabric-renderer-api-v1")
    exclude(group = "net.fabricmc.fabric-api", module = "fabric-model-loading-api-v1")
    exclude(group = "net.fabricmc.fabric-api", module = "fabric-models-v0")
}

dependencies {
    implementation(project(":api"))

    minecraft(rootProject.libs.minecraft)
    mappings(loom.officialMojangMappings())

    modImplementation(rootProject.libs.fabric.loader)
    modImplementation(rootProject.libs.fabric.api)
    // Required because Loom injects Fabric API interfaces (e.g. DataResourceStore) into Minecraft classes at compile-time.
    // The umbrella fabric-api artifact doesn't currently add this v1 module to the compile classpath for this setup.
    modImplementation(rootProject.libs.fabric.resource.loader.v1)

    modImplementation(include("net.kyori:adventure-platform-fabric:${libs.versions.adventure.platform.fabric.get()}")!!)
    implementation(rootProject.libs.adventure.minimessage)

    modImplementation(include("de.t14d3.rapunzellib:platform-fabric:${libs.versions.rapunzellib.get()}")!!)
    modImplementation(include("de.t14d3.rapunzellib:events-fabric:${libs.versions.rapunzellib.get()}")!!)
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

tasks {
    shadowJar {
        exclude("*mixins.json")
        exclude("*refmap.json")
        exclude("*.accesswidener")
        exclude("fabric-installer*")
        exclude("LICENSE*")
        exclude("/assets/")
        exclude("/net/")
        exclude("/ui/")
    }
}
