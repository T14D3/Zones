plugins {
    id("maven-publish")
    alias(libs.plugins.shadow)
    `java-library`
    alias(libs.plugins.fabric.loom) apply false
}


group = "de.t14d3"
version = "0.2.2"

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        name = "t14d3-snapshots"
        url = uri("https://maven.t14d3.de/snapshots")
    }
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "JitPack"
        url = uri("https://jitpack.io")
    }

}

allprojects {
    plugins.apply("java")
    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            name = "t14d3-snapshots"
            url = uri("https://maven.t14d3.de/snapshots")
        }
        maven {
            name = "papermc-repo"
            url = uri("https://repo.papermc.io/repository/maven-public/")       
        }
        maven {
            name = "sonatype"
            url = uri("https://oss.sonatype.org/content/groups/public/")
        }
        maven {
            name = "JitPack"
            url = uri("https://jitpack.io")
        }
    }
    dependencies {
        compileOnly(rootProject.libs.paper.api)
        compileOnly(rootProject.libs.annotations)

        compileOnly(rootProject.libs.adventure.api)
        compileOnly(rootProject.libs.adventure.minimessage)
    }
}

dependencies {
    implementation(project(":api"))
    implementation(project(":bukkit"))
    implementation(project(":fabric"))
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



tasks {
    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles()
        relocate("dev.jorel.commandapi", "de.t14d3.zones.dependencies.commandapi")

        relocate("org.simpleyaml", "de.t14d3.zones.dependencies.simpleyaml")
        relocate("org.yaml.snakeyaml", "de.t14d3.zones.dependencies.snakeyaml")


        dependencies {
            exclude(dependency("org.checkerframework:checker-qual"))
            exclude(dependency("io.leangen.geantyref:geantyref"))
        }
        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }

        // Very hacky, but works for now
        exclude("*mixins.json")
        exclude("*refmap.json")
        exclude("*.accesswidener")
        exclude("fabric-installer*")
        exclude("LICENSE*")

        exclude("/assets/")
        exclude("/net/")
        exclude("/ui/")
    }
    build {
        dependsOn(shadowJar)
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
