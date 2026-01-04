plugins {
    id("java")
    alias(libs.plugins.run.paper)
    alias(libs.plugins.paperweight.userdev)
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
    implementation(platform(rootProject.libs.intellectualsites.bom))
    compileOnly(rootProject.libs.fawe.core)
    compileOnly(rootProject.libs.fawe.bukkit)
    implementation(rootProject.libs.commandapi.paper.shade)

    implementation(rootProject.libs.adventure.platform.bukkit)

    val localRapunzelLibsDir = rootProject.file("../RapunzelLib/build/libs")

    val localRapunzelPlatformPaper = localRapunzelLibsDir
        .takeIf { it.isDirectory }
        ?.listFiles()
        ?.filter { it.isFile && it.name.startsWith("rapunzellib-platform-paper-") && it.name.endsWith("-shaded.jar") }
        ?.maxByOrNull { it.lastModified() }

    val localRapunzelEventsPaper = localRapunzelLibsDir
        .takeIf { it.isDirectory }
        ?.listFiles()
        ?.filter {
            it.isFile &&
                    it.name.startsWith("rapunzellib-events-paper-") &&
                    it.name.endsWith(".jar") &&
                    !it.name.endsWith("-sources.jar") &&
                    !it.name.endsWith("-javadoc.jar")
        }
        ?.maxByOrNull { it.lastModified() }

    val localRapunzelEvents = localRapunzelLibsDir
        .takeIf { it.isDirectory }
        ?.listFiles()
        ?.filter {
            it.isFile &&
                    it.name.startsWith("rapunzellib-events-") &&
                    it.name.endsWith(".jar") &&
                    !it.name.contains("-paper-") &&
                    !it.name.contains("-fabric-") &&
                    !it.name.endsWith("-sources.jar") &&
                    !it.name.endsWith("-javadoc.jar")
        }
        ?.maxByOrNull { it.lastModified() }

    if (localRapunzelPlatformPaper != null) {
        implementation(files(localRapunzelPlatformPaper))
    } else {
        implementation("de.t14d3.rapunzellib:platform-paper:${libs.versions.rapunzellib.get()}:shaded")
    }

    if (localRapunzelEvents != null) {
        implementation(files(localRapunzelEvents))
    } else {
        implementation(rootProject.libs.rapunzellib.events)
    }

    if (localRapunzelEventsPaper != null) {
        implementation(files(localRapunzelEventsPaper))
    } else {
        implementation(rootProject.libs.rapunzellib.events.paper)
    }

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
