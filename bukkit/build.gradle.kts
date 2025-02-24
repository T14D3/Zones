plugins {
    id("java")
}

group = "de.t14d3"
version = "0.2.1"

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
}

dependencies {
    implementation(project(":api"))
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.10")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.13")
    implementation(platform("com.intellectualsites.bom:bom-newest:1.52"))
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit")
    implementation("dev.jorel:commandapi-bukkit-shade-mojang-mapped:9.7.0")
}

tasks.test {
    useJUnitPlatform()
}