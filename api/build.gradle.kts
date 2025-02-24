plugins {
    id("java")
}

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:23.0.0")
    compileOnly("org.slf4j:slf4j-api:2.1.0-alpha1")
    compileOnly("it.unimi.dsi:fastutil:8.5.8")
    compileOnly("com.google.code.gson:gson:2.9.0")
}
