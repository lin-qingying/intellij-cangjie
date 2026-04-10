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

dependencies {
    implementation(project(":common"))

    // IntelliJ Platform modules
    implementation(project(":"))
    implementation(project(":toolchain"))
    implementation(project(":cangjie-project"))
    implementation(project(":messages"))
    implementation(libs.cangjieCommonForIde)
    implementation(libs.cangjiePsiForIde)

    implementation(project(":debugger:common"))

    // LSP4J for DAP (Debug Adapter Protocol)
    implementation(libs.lsp4j.debug)

    // Kotlin Coroutines
    compileOnly(libs.bundles.kotlinx.coroutines)

    // Testing
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
