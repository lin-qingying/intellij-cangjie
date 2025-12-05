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

package org.cangnova.cangjie.lsp4ij

import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.server.OSProcessStreamConnectionProvider
import org.cangnova.cangjie.lsp.CangJieLspServerManager
import java.io.*

class CangJieOSProcessStreamConnectionProvider(
    val project: Project
) : OSProcessStreamConnectionProvider(
    CangJieLspServerManager.getInstance(project).getCommandLine()
) {
    private val modifiedInputStream = PipedInputStream()
    private val pipedOutputStream = PipedOutputStream(modifiedInputStream)

    override fun start() {
        super.start() // 启动 LSP 服务器
//        Thread { interceptAndModifyOutput() }.start() // 启动拦截线程
    }

    private fun interceptAndModifyOutput() {
        try {
            val reader = BufferedReader(InputStreamReader(super.getInputStream()))
            val writer = BufferedWriter(OutputStreamWriter(pipedOutputStream))

            while (true) {
                // 读取 Content-Length 头部
                val contentLengthLine = reader.readLine() ?: break
                if (!contentLengthLine.startsWith("Content-Length:")) {
                    continue
                }
                val contentLength = contentLengthLine.split(":")[1].trim().toInt()

                // 将 BufferedReader 转换为 PushbackReader
                val pushbackReader = PushbackReader(reader)

                // 读取换行符
                val separator = readNewlineChars(pushbackReader)
//                val separator = "\r\n\r\n"

                // 读取 JSON 数据
                val jsonBuilder = StringBuilder()
                var bytesRead = 0
                while (bytesRead < contentLength) {
                    val char = pushbackReader.read().toChar()
                    jsonBuilder.append(char)
                    bytesRead++
                }
                var json = jsonBuilder.toString()

                // 修改 JSON
                json = modifyLspResponse(json)

                // 重新计算 Content-Length
                val newContentLength = json.toByteArray().size

//                $separator
                // 发送修改后的数据
                writer.write("Content-Length: $newContentLength")
                writer.write(13)
                writer.write(10)
                writer.write(13)
                writer.write(10)
                writer.write(json)
                writer.flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun modifyLspResponse(json: String): String {
        return json.replace("\"codeLensProvider\":true", "\"codeLensProvider\":{\"resolveProvider\": true}")
    }

    fun readNewlineChars(reader: PushbackReader): String {
        val buffer = StringBuilder()
        var consecutiveNewlines = 0
        var currentChar: Int

        // 读取直到找到两组换行符（即 \r\n\r\n）
        while (reader.read().also { currentChar = it } != -1) {
            when (currentChar.toChar()) {
                '\r', '\n' -> {
                    buffer.append(currentChar.toChar())
                    if (currentChar.toChar() == '\n') {
                        consecutiveNewlines++
                        if (consecutiveNewlines == 2) break // 两个 \n 表示空行结束
                    }
                }

                else -> {
                    reader.unread(currentChar)
                    break
                }
            }
        }

        return buffer.toString()
    }


//    override fun getInputStream(): InputStream? {
//        return modifiedInputStream
//    }

}