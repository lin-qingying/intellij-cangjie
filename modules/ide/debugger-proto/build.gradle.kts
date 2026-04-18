plugins {
    id("cangjie.intellij-module-protobuf")
}

dependencies {
    implementation(project(":modules:foundation"))
    implementation(project(":modules:domain:toolchain"))
    implementation(project(":modules:domain:project-model"))
    implementation(project(":modules:domain:telemetry"))
    implementation(project(":modules:ide:debugger-api"))
    implementation(project(":modules:ide:base"))
    compileOnly(libs.cangjiePsiForIde)
    compileOnly(libs.cangjieCommonForIde)
    implementation("org.jetbrains.pty4j:pty4j:0.13.11") {
        exclude(group = "net.java.dev.jna")
    }
    implementation(libs.bundles.protobuf)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.24.4"
    }

    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.58.0"
        }
    }

    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
            }
        }
    }
}
