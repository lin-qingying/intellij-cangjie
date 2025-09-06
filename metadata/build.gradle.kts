plugins {
    kotlin("jvm")
    id("io.netifi.flatbuffers") version "1.0.7"
}

dependencies {
    implementation("com.google.flatbuffers:flatbuffers-java:25.2.10")
    implementation(project(":common"))
    implementation(project(":descriptors"))

}

// FlatBuffers配置
flatbuffers {
    flatcPath = project(":metadata").projectDir.path + "/flatbuffers/flatc.exe" // 使用本地的flatc.exe
    language = "kotlin"
    flatBuffersVersion = "25.2.10"
}
tasks.register<io.netifi.flatbuffers.plugin.tasks.FlatBuffers>("generateKotlinFlatBuffers") {
    inputDir = file("flatbuffers")
    outputDir = file(project(":metadata").projectDir.path + "/gen")
    language = "kotlin"
    extraArgs = "--gen-mutable --gen-object-api"
}

tasks.compileKotlin {
    dependsOn(/*"generateFlatBuffers", */"generateKotlinFlatBuffers")
}
sourceSets {
    main {
        kotlin {
            srcDirs("gen")
        }
    }
}