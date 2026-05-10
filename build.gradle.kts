plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    application
}

group = "walkthroughrenderer"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    // CLI
    implementation("com.github.ajalt.clikt:clikt:4.4.0")

    // YAML <-> data classes via kotlinx.serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")
    implementation("com.charleskorn.kaml:kaml:0.66.0")

    // HTML emission (steps 3+)
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")

    // Markdown rendering for captions and openQuestions (steps 3+)
    implementation("com.vladsch.flexmark:flexmark:0.64.8")
    implementation("com.vladsch.flexmark:flexmark-ext-tables:0.64.8")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

application {
    // Main.kt has no package declaration -> generated class is `MainKt`.
    mainClass.set("MainKt")
    applicationName = "walkthrough"
}

tasks.test {
    useJUnitPlatform()
}
