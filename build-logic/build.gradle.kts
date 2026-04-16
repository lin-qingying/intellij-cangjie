plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.intellij.platform:intellij-platform-gradle-plugin:2.10.5")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.2.0")
    implementation("org.gradle.test-retry:org.gradle.test-retry.gradle.plugin:1.6.0")
    implementation("com.google.protobuf:protobuf-gradle-plugin:0.9.4")
}
