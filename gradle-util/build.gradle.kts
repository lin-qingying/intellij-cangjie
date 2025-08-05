plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}
dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    implementation(gradleApi())
}
//gradlePlugin {
//    plugins {
//        create("sourceSetsPlugin") {
//            id = "gradle-util"
//            implementationClass = "SourceSetsPlugin"
//        }
//    }
//}

repositories {
    mavenCentral()
    gradlePluginPortal()
}