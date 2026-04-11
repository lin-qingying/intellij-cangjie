/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

plugins {
    id("org.jetbrains.intellij.platform.module")
    kotlin("jvm")
    id("com.google.protobuf") version "0.9.4"
}
dependencies {
    implementation(project(":common"))

    implementation(project(":"))
    implementation(libs.cangjieCommonForIde)
    implementation(libs.cangjiePsiForIde)
// https://mvnrepository.com/artifact/org.jetbrains.pty4j/pty4j
    api("org.jetbrains.pty4j:pty4j:0.13.11") {
        // JNA 由 IDE 平台提供（通过 JnaLoader 正确初始化）。
        // 若随插件打包，插件 classloader（child-first）会加载自己的 JNA 副本，
        // 绕过 IDE 的初始化，导致 Windows 上 UnsatisfiedLinkError
        exclude(group = "net.java.dev.jna")
    }
    implementation(project(":telemetry"))
    implementation(project(":toolchain"))
    implementation(project(":messages"))

    implementation(project(":cangjie-project"))

    implementation(project(":debugger:common"))


    implementation(libs.bundles.protobuf)

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
