plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.apache.logging.log4j:log4j-core:2.16.0")
    compileOnly("it.unimi.dsi:fastutil:8.5.8")

    compileOnly("com.h2database:h2:2.3.232")
    compileOnly("org.postgresql:postgresql:42.7.2")

    implementation("org.spongepowered:configurate-yaml:4.1.2")
}
