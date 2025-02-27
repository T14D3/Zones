import xyz.jpenilla.runtask.task.AbstractRun

plugins {
    id("java")
    id("maven-publish")
    id("com.gradleup.shadow") version "9.0.0-beta8"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "de.t14d3"
version = "0.2.1"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
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
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.10")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.13")
    implementation(platform("com.intellectualsites.bom:bom-newest:1.52"))
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit")
    implementation("dev.jorel:commandapi-bukkit-shade-mojang-mapped:9.7.0")
    compileOnly("org.slf4j:slf4j-api:2.1.0-alpha1")

    implementation("com.h2database:h2:2.2.220")
    implementation("org.postgresql:postgresql:42.7.5")
}

val targetJavaVersion = 21
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"

    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}
tasks.withType<AbstractRun>().configureEach {
    javaLauncher = javaToolchains.launcherFor {
        vendor.set(JvmVendorSpec.JETBRAINS)
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("dev.jorel.commandapi", "de.t14d3.zones.commandapi")
        relocate("org.h2", "de.t14d3.zones.db.h2")
        relocate("org.postgresql", "de.t14d3.zones.db.postgresql")

        dependencies {
            exclude(dependency("org.checkerframework:checker-qual"))
        }
    }
    build {
        dependsOn(shadowJar)
    }
    runServer {
        minecraftVersion("1.21.4")
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/t14d3/zones")
            credentials {
                username = project.findProperty("gpr.user").toString() ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key").toString() ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("gpr") {
            artifactId = project.name.lowercase()
            groupId = group.toString().lowercase()
            version = version.toString()
            from(components["java"])
        }
    }
}
