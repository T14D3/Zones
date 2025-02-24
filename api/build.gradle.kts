plugins {
    id("java")
}

repositories {
    mavenCentral()
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
}

dependencies {
    compileOnly("org.slf4j:slf4j-api:2.1.0-alpha1")
    compileOnly("it.unimi.dsi:fastutil:8.5.8")
    compileOnly("com.google.code.gson:gson:2.9.0")
    implementation("org.yaml:snakeyaml:2.5-SNAPSHOT")
    implementation("net.kyori:adventure-api:4.19.0")
}
