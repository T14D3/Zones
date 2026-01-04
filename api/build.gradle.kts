plugins {
    id("java")
}

repositories {
    mavenCentral()
    maven {
        name = "JitPack"
        url = uri("https://jitpack.io")
    }
}

dependencies {
    compileOnly(rootProject.libs.log4j.core)
    compileOnly(rootProject.libs.fastutil)

    compileOnly(rootProject.libs.h2)
    compileOnly(rootProject.libs.postgresql)


    compileOnly(rootProject.libs.rapunzellib.api)
    compileOnly(rootProject.libs.rapunzellib.events)
}
