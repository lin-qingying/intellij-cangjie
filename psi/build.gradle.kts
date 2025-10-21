/*
 * Copyright 2025 LinQingYing. and contributors.
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

import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
//    `java-test-fixtures`
}
val tomlPlugin = "org.toml.lang"
val jsonPlugin = "com.intellij.modules.json"
val copyright = "com.intellij.copyright"

dependencies {
    testImplementation(kotlin("test"))

    implementation(project(":util"))
    implementation(project(":icon"))
    implementation(project(":messages"))

    implementation(project(":common"))
    implementation(project(":telemetry"))
//    intellijPlatform {
//        bundledPlugins(tomlPlugin, copyright, jsonPlugin)
//    }
}

// 配置 Lexer 生成任务
tasks.register<GenerateLexerTask>("generateCangJieLexer") {
    sourceFile.set(file("src/main/kotlin/org/cangnova/cangjie/lexer/CangJieLexer.flex"))
    targetOutputDir.set(file("src/gen/org/cangnova/cangjie/lexer"))
//    purgeOldFiles.set(true)
}

tasks.register<GenerateLexerTask>("generateCDocLexer") {
    sourceFile.set(file("src/main/kotlin/org/cangnova/cangjie/lexer/cdoc/lexer/CDoc.flex"))
    targetOutputDir.set(file("src/gen/org/cangnova/cangjie/lexer/cdoc/lexer"))
//    要确保generateCangJieLexer在generateCDocLexer之前执行
//    purgeOldFiles.set(true)
}

// 创建一个组合任务来生成所有 Lexer

tasks.register<Task>("generateLexers") {
    dependsOn("generateCangJieLexer", "generateCDocLexer")
    group = "build"
    description = "Generate all lexers for the project"
}

// 确保在编译 Java 之前生成 Lexer
tasks.compileJava {
    dependsOn("generateLexers")
}

// 确保在编译 Kotlin 之前也生成 Lexer（如果 Kotlin 代码依赖 Lexer）
tasks.compileKotlin {
    dependsOn("generateLexers")
}

// 将生成的代码目录添加到源码路径
sourceSets {
    main {
        java {
            srcDirs("src/gen")
        }
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xcontext-parameters"))
}