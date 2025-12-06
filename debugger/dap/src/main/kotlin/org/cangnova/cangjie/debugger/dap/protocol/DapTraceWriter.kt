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

package org.cangnova.cangjie.debugger.dap.protocol

import org.cangnova.cangjie.debugger.util.writeStringToFileDap
import java.io.Writer

/**
 * DAP消息跟踪Writer
 *
 * 用于捕获LSP4J的跟踪输出并写入文件
 */
class DapTraceWriter : Writer() {

    private val buffer = StringBuilder()
    private var lastDirection: String? = null

    override fun write(cbuf: CharArray, off: Int, len: Int) {
        val str = String(cbuf, off, len)
        buffer.append(str)

        // 检查是否是完整的一行
        if (str.contains('\n')) {
            flush()
        }
    }

    override fun flush() {
        val content = buffer.toString()
        if (content.isNotBlank()) {
            // 解析方向（发送/接收）
            val direction = when {
                content.contains("Sending request") || content.contains("Sending notification") -> "发送"
                content.contains("Received response") || content.contains("Received notification") -> "接收"
                else -> lastDirection ?: "消息"
            }

            writeStringToFileDap(content.trim(), direction)
            lastDirection = direction
            buffer.clear()
        }
    }

    override fun close() {
        flush()
    }
}