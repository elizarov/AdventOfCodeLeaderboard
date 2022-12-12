plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
}

sourceSets.main {
    java.srcDirs("src")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

application {
    mainClass.set("MainKt")
    val passThroughProps = setOf("excludeIds", "daysRange")
    applicationDefaultJvmArgs = properties.asSequence()
        .filter { it.key in passThroughProps }
        .map { "-D${it.key}=${it.value}" }
        .toList()
}