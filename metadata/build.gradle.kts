plugins {
    kotlin("jvm")
//    id("jps-compatible")
}

//project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
//    api(protobufLite())
// https://mvnrepository.com/artifact/com.google.protobuf/protobuf-javalite
//    implementation("com.google.protobuf:protobuf-javalite:4.27.2")
//    implementation(project(":protobuf2.6.1"))
// https://mvnrepository.com/artifact/com.google.protobuf/protobuf-java
    implementation("com.google.protobuf:protobuf-java:4.27.2")


    api("org.jetbrains.kotlin:protobuf-lite:2.6.1-1")
    implementation(kotlin("stdlib", embeddedKotlinVersion))


}
sourceSets {
    main {
        java.srcDirs("src")

        resources.srcDir("resources")

    }
    test {}
}
