plugins {
    id("java")
    alias(libs.plugins.run.paper)
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.shadow)
}

repositories {
    mavenCentral()
    maven {
        name = "ExtendedClip"
        url = uri("https://repo.extendedclip.com/releases/")
    }
    maven {
        name = "EngineHub"
        url = uri("https://maven.enginehub.org/repo/")
    }
    maven {
        name = "CodeMC"
        url = uri("https://repo.codemc.io/repository/maven-public/")
    }
    maven {
        name = "Central Portal Snapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        // Only search this repository for the specific dependency
        content {
            includeModule("dev.jorel", "commandapi")
        }
    }
}

dependencies {
    implementation(project(":api"))

    compileOnly(rootProject.libs.placeholderapi)
    compileOnly(rootProject.libs.worldedit.bukkit)
    compileOnly(rootProject.libs.worldguard.bukkit)
    compileOnly(platform(rootProject.libs.intellectualsites.bom))
    compileOnly(rootProject.libs.fawe.core)
    compileOnly(rootProject.libs.fawe.bukkit)
    implementation(rootProject.libs.commandapi.paper.shade)

    compileOnly(rootProject.libs.adventure.platform.bukkit)

    implementation(rootProject.libs.rapunzellib.api)
    implementation(rootProject.libs.rapunzellib.platform.paper)
    implementation(rootProject.libs.rapunzellib.events.paper)

    paperweight.paperDevBundle(rootProject.libs.versions.paper.dev.bundle.get())
}

tasks.processResources {
    val props = mapOf("version" to rootProject.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
runPaper.disablePluginJarDetection()

val zonesActiveProcessors: Int? = providers.gradleProperty("zones.activeProcessors")
    .orNull
    ?.toIntOrNull()

tasks.runServer {
    minecraftVersion("1.21.10")
    pluginJars(rootProject.tasks.named("shadowJar").get().outputs.files)        
    javaLauncher = javaToolchains.launcherFor {
        vendor.set(JvmVendorSpec.JETBRAINS)
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
    if (zonesActiveProcessors != null && zonesActiveProcessors > 0) {
        jvmArgs("-XX:ActiveProcessorCount=$zonesActiveProcessors")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    jar {
        archiveClassifier.set("dev")
    }

    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles()

        exclude("LICENSE*")
        exclude("net/kyori/**")
    }

    build {
        dependsOn(shadowJar)
    }
}
