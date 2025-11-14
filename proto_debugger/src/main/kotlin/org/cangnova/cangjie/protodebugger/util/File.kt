package org.cangnova.cangjie.protodebugger.util

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

fun writeStringToFile(content: String, direction: String) {
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
