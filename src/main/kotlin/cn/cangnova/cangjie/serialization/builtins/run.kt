/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.serialization.builtins

import java.io.File

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    if (args.size < 2) {
        println(
            """CangJie built-ins serializer

Usage: ... <destination dir> (<source dir>)+

Analyzes CangJie sources found in the given source directories and serializes
found top-level declarations to <destination dir> (*.cangjie_builtins files)"""
        )
        return
    }

    val destDir = File(args[0])

    val srcDirs = args.drop(1).map( ::File)
    assert(srcDirs.isNotEmpty()) { "At least one source directory should be specified" }

    val missing = srcDirs.filterNot(File::exists)
    assert(missing.isEmpty()) { "These source directories are missing: $missing" }

    BuiltInsSerializer.analyzeAndSerialize(
        destDir,
        srcDirs,
        extraClassPath = listOf(),

        dependOnOldBuiltIns = false
    ) { totalSize, totalFiles ->
        if (System.getProperty("cangjie.builtins.serializer.log") == "true") {
            println("Total bytes written: $totalSize to $totalFiles files")
        }
    }
}
