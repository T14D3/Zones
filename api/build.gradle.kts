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
    compileOnly("org.apache.logging.log4j:log4j-core:2.24.3")
    compileOnly("it.unimi.dsi:fastutil:8.5.8")

    compileOnly("com.h2database:h2:2.3.232")
    compileOnly("org.postgresql:postgresql:42.7.2")

    implementation("com.github.Carleslc.Simple-YAML:Simple-Yaml:1.8.4")
}
