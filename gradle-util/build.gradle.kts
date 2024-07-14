
plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
//    id("org.jetbrains.kotlin.jvm")
}
repositories {
    mavenCentral()
    google()
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    gradlePluginPortal()
//
//    extra["bootstrapKotlinRepo"]?.let {
//        maven(url = it)
//    }
}

dependencies {
    implementation(kotlin("stdlib", embeddedKotlinVersion))
//    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:0.0.40")

    compileOnly(gradleApi())

//    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
//    implementation("org.jetbrains.kotlin:kotlin-stdlib")
//    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}
gradlePlugin {
    plugins {
        register("jps-compatible") {
            id = "jps-compatible"
            implementationClass = "org.jetbrains.kotlin.pill.JpsCompatiblePlugin"
        }
        register("kotlin-build-publishing") {
            id = "kotlin-build-publishing"
            implementationClass = "plugins.KotlinBuildPublishingPlugin"
        }
    }
}
tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
