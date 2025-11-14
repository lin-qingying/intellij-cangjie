plugins {

    id("com.google.protobuf") version "0.9.4"
}
dependencies {
    //    intellijPlatform {
//        plugins("com.redhat.devtools.lsp4ij:0.18.0")
//    }
    implementation(project(":"))
    implementation(project(":psi"))
    implementation(project(":common"))

    implementation(project(":telemetry"))
    implementation(project(":toolchain"))
    implementation(project(":messages"))
    implementation(project(":util"))

    implementation(project(":cangjie-project"))




    implementation("com.google.protobuf:protobuf-java:3.24.4")
    implementation("com.google.protobuf:protobuf-kotlin:3.24.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation(kotlin("test"))
}



// Protobuf configuration
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
                create("grpc") {}
            }
        }
    }
}
project(":plugin") {
    dependencies {
        implementation(project(":proto_debugger"))
//        intellijPlatform {
//            plugins("com.redhat.devtools.lsp4ij:0.18.0")
//        }
    }
}