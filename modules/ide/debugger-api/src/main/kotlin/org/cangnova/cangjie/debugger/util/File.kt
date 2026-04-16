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

package org.cangnova.cangjie.debugger.util

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

fun writeStringToFileDap(content: String, direction: String) {
    return
    val filePath = "D:\\code\\intellij\\intellij-cangjie\\log\\dap.txt"
    val file = File(filePath)

    // 获取当前时间
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val currentTime = dateFormat.format(Date())

    // 如果文件存在，则追加内容，否则创建新文件
    val writer = PrintWriter(FileWriter(file, true))  // 设置为追加模式
    writer.use {
        if (file.exists()) {
            // 文件已存在，追加两个换行符、方向、时间和内容
            writer.println()
            writer.println("Direction: $direction")
            writer.println("Time: $currentTime")
            writer.println(content)
        } else {
            // 文件不存在，直接写入方向、时间和内容
            writer.println("Direction: $direction")
            writer.println("Time: $currentTime")
            writer.write(content)
        }
    }
}

fun writeStringToFile(content: String, direction: String) {
    return
    val filePath = "D:\\code\\intellij\\intellij-cangjie\\log\\lldb.txt"
    val file = File(filePath)

    // 获取当前时间
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val currentTime = dateFormat.format(Date())

    // 如果文件存在，则追加内容，否则创建新文件
    val writer = PrintWriter(FileWriter(file, true))  // 设置为追加模式
    writer.use {
        if (file.exists()) {
            // 文件已存在，追加两个换行符、方向、时间和内容
            writer.println()
            writer.println("Direction: $direction")
            writer.println("Time: $currentTime")
            writer.println(content)
        } else {
            // 文件不存在，直接写入方向、时间和内容
            writer.println("Direction: $direction")
            writer.println("Time: $currentTime")
            writer.write(content)
        }
    }
}
