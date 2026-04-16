plugins {
    id("cangjie.intellij-module")
}

dependencies {
    implementation(libs.cangjieCommonForIde)
    implementation(libs.jansi)
    implementation(libs.vavr)
    implementation("com.squareup.okio:okio:3.9.0")
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.9")
    implementation("org.slf4j:slf4j-api:2.0.13")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")

    compileOnly("net.java.dev.jna:jna:5.14.0")
    compileOnly("net.java.dev.jna:jna-platform:5.14.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("net.java.dev.jna:jna:5.14.0")
    testImplementation("net.java.dev.jna:jna-platform:5.14.0")
}
