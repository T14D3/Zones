import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("maven-publish")
    alias(libs.plugins.shadow)
    `java-library`
    alias(libs.plugins.fabric.loom) apply false
}


group = "de.t14d3"
val buildVersion = System.getenv("VERSION")?.takeIf { it.isNotBlank() } ?: "0.3.0"
version = buildVersion

abstract class CheckReposiliteConfig : DefaultTask() {
    @get:Input
    @get:Optional
    abstract val reposiliteBaseUrl: Property<String>
    @get:Input
    @get:Optional
    abstract val reposiliteUsername: Property<String>
    @get:Input
    @get:Optional
    abstract val reposilitePassword: Property<String>

    @TaskAction
    fun run() {
        val missingKeys = mutableListOf<String>()

        fun require(name: String, value: String?) {
            if (value.isNullOrBlank()) missingKeys += name
        }

        require("reposiliteUsername/REPOSILITE_USERNAME", reposiliteUsername.orNull)
        require("reposilitePassword/REPOSILITE_PASSWORD", reposilitePassword.orNull)

        reposiliteBaseUrl.orNull?.let { baseUrl ->
            val url = baseUrl.trim()
            if (url.isNotBlank() && !url.startsWith("http://") && !url.startsWith("https://")) {
                missingKeys += "reposiliteBaseUrl/REPOSILITE_BASE_URL must start with http:// or https://"
            }
        }

        if (missingKeys.isNotEmpty()) {
            throw GradleException(
                "Missing Reposilite publishing configuration:\n" +
                        missingKeys.joinToString(separator = "\n") { "- $it" } +
                        "\n\nConfigure these as Gradle properties (recommended: ~/.gradle/gradle.properties) or environment variables."
            )
        }
    }
}

val reposiliteBaseUrl =
    (findProperty("reposiliteBaseUrl") as String?)
        ?: System.getenv("REPOSILITE_BASE_URL")
        ?: "https://maven.t14d3.de"
val reposiliteUsername: String? =
    (findProperty("reposiliteUsername") as String?) ?: System.getenv("REPOSILITE_USERNAME")
val reposilitePassword: String? =
    (findProperty("reposilitePassword") as String?) ?: System.getenv("REPOSILITE_PASSWORD")

val reposiliteRepo = if (buildVersion.endsWith("SNAPSHOT")) "snapshots" else "releases"
val reposiliteRepoUrl = "${reposiliteBaseUrl.trimEnd('/')}/$reposiliteRepo"

val checkReposiliteConfig = tasks.register<CheckReposiliteConfig>("checkReposiliteConfig") {
    group = "publishing"
    description = "Validates required configuration for publishing to Reposilite."

    reposiliteBaseUrl.set(
        providers.gradleProperty("reposiliteBaseUrl").orElse(providers.environmentVariable("REPOSILITE_BASE_URL"))
    )
    reposiliteUsername.set(
        providers.gradleProperty("reposiliteUsername").orElse(providers.environmentVariable("REPOSILITE_USERNAME"))
    )
    reposilitePassword.set(
        providers.gradleProperty("reposilitePassword").orElse(providers.environmentVariable("REPOSILITE_PASSWORD"))
    )
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        name = "t14d3-releases"
        url = uri("${reposiliteBaseUrl.trimEnd('/')}/releases")
    }
    maven {
        name = "t14d3-snapshots"
        url = uri("${reposiliteBaseUrl.trimEnd('/')}/snapshots")
    }
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

}

allprojects {
    group = rootProject.group
    version = rootProject.version

    plugins.apply("java")
    plugins.apply("maven-publish")
    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            name = "t14d3-releases"
            url = uri("${reposiliteBaseUrl.trimEnd('/')}/releases")
        }
        maven {
            name = "t14d3-snapshots"
            url = uri("${reposiliteBaseUrl.trimEnd('/')}/snapshots")
        }
        maven {
            name = "papermc-repo"
            url = uri("https://repo.papermc.io/repository/maven-public/")       
        }
        maven {
            name = "sonatype"
            url = uri("https://oss.sonatype.org/content/groups/public/")        
        }
    }
    dependencies {
        compileOnly(rootProject.libs.paper.api)
        compileOnly(rootProject.libs.annotations)

        compileOnly(rootProject.libs.adventure.api)
        compileOnly(rootProject.libs.adventure.minimessage)
    }
}

subprojects {
    extensions.configure<BasePluginExtension> {
        archivesName.set("${rootProject.name.lowercase()}-${project.name}")
    }
}

extensions.configure<BasePluginExtension> {
    archivesName.set(rootProject.name.lowercase())
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
    jar {
        enabled = false
    }
    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles()
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

val syncCentralJars = tasks.register<Copy>("syncCentralJars") {
    group = "build"
    description = "Copies API/Bukkit/Fabric jars into the root build/libs directory."

    val apiJar = project(":api").tasks.named<Jar>("jar")
    val bukkitJar = project(":bukkit").tasks.named<ShadowJar>("shadowJar")
    val fabricJar = project(":fabric").tasks.named<RemapJarTask>("remapJar")

    dependsOn(apiJar, bukkitJar, fabricJar)

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    includeEmptyDirs = false
    into(layout.buildDirectory.dir("libs"))

    from(apiJar.flatMap { it.archiveFile })
    from(bukkitJar.flatMap { it.archiveFile })
    from(fabricJar.flatMap { it.archiveFile })
}

tasks.named("build") {
    dependsOn(syncCentralJars)
}


allprojects {

    tasks.withType<ShadowJar> {
        relocate("org.yaml.snakeyaml", "de.t14d3.zones.dependencies.snakeyaml")
        relocate("dev.jorel.commandapi", "de.t14d3.zones.dependencies.commandapi")


        dependencies {
            exclude(dependency("org.checkerframework:checker-qual"))
            exclude(dependency("io.leangen.geantyref:geantyref"))
        }
    }

    extensions.configure<PublishingExtension> {
        repositories {
            maven {
                name = "reposilite"
                url = uri(reposiliteRepoUrl)

                credentials {
                    username = reposiliteUsername
                    password = reposilitePassword
                }
            }
        }
    }

    tasks.withType<PublishToMavenRepository>().configureEach {
        if (name.contains("ToReposiliteRepository")) {
            dependsOn(rootProject.tasks.named("checkReposiliteConfig"))
        }
    }
}

extensions.configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = rootProject.name.lowercase()
            groupId = rootProject.group.toString()
            version = buildVersion
            artifact(tasks.named("shadowJar"))
        }
    }
}

project(":api") {
    plugins.withId("java") {
        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    artifactId = "${rootProject.name.lowercase()}-api"
                    groupId = rootProject.group.toString()
                    version = buildVersion
                    from(components["java"])
                }
            }
        }
    }
}

project(":bukkit") {
    plugins.withId("com.gradleup.shadow") {
        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    artifactId = "${rootProject.name.lowercase()}-bukkit"
                    groupId = rootProject.group.toString()
                    version = buildVersion
                    artifact(tasks.named("shadowJar"))
                }
            }
        }
    }
}

project(":fabric") {
    plugins.withId("fabric-loom") {
        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    artifactId = "${rootProject.name.lowercase()}-fabric"
                    groupId = rootProject.group.toString()
                    version = buildVersion
                    artifact(tasks.named("remapJar"))
                }
            }
        }
    }
}
