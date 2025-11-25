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

package org.cangnova.cangjie.protodebugger.output

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.BaseOutputReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * 日志文件读取器
 *
 * 职责：
 * - 读取日志文件内容
 * - 监听新增内容并触发回调
 * - 管理读取器生命周期
 */
class LogFileReader(
    private val file: File,
    private val logType: LogType,
    private val onTextAvailable: (String, LogType) -> Unit
) {
    companion object {
        private val LOG = Logger.getInstance(LogFileReader::class.java)
    }

    @Volatile
    private var closed = false
    private var reader: BaseOutputReader? = null

    /**
     * 启动读取
     */
    fun start() {
        if (reader != null) {
            LOG.warn("Reader already started for file: ${file.name}")
            return
        }

        if (!file.exists()) {
            LOG.warn("Cannot start reader, file does not exist: ${file.absolutePath}")
            return
        }

        try {
            reader = createReader()
        } catch (e: IOException) {
            LOG.error("Failed to start reader for file: ${file.name}", e)
        }
    }

    /**
     * 停止读取
     */
    fun stop() {
        closed = true
        reader?.stop()
    }

    /**
     * 等待读取完成
     */
    @Throws(InterruptedException::class)
    fun waitFor() {
        reader?.waitFor()
    }

    /**
     * 检查是否已关闭
     */
    fun isClosed(): Boolean = closed

    /**
     * 创建输出读取器
     */
    private fun createReader(): BaseOutputReader {
        return object : BaseOutputReader(FileInputStream(file), null) {
            init {
                start("log: ${file.name}")
            }

            override fun onTextAvailable(text: String) {
                if (!closed) {
                    this@LogFileReader.onTextAvailable(text, logType)
                }
            }

            override fun executeOnPooledThread(runnable: Runnable) =
                ApplicationManager.getApplication().executeOnPooledThread(runnable)
        }
    }
}