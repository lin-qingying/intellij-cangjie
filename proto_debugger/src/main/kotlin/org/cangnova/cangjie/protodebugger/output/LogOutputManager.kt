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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 日志输出管理器
 *
 * 职责：
 * - 管理多个日志文件的监控和读取
 * - 协调监控器和读取器的生命周期
 * - 提供统一的清理接口
 */
abstract class LogOutputManager(
    private val logDir: File,
    @field:NlsSafe val logName: String
) {
    companion object {
        private val LOG = Logger.getInstance(LogOutputManager::class.java)
    }

    private val entries = CopyOnWriteArrayList<LogEntry>()

    /**
     * 初始化日志管理器
     *
     * 默认监控 INFO 级别日志，子类可重写以监控其他级别
     */
    open fun init() {
        addLogType(LogType.INFO)
    }

    /**
     * 添加要监控的日志类型
     */
    protected fun addLogType(type: LogType) {
        val file = File(logDir, "$logName.$type")

        // 清理已存在的文件
        if (file.exists()) {
            FileUtil.delete(file)
        }

        val entry = LogEntry(file, type)
        entries += entry
        entry.start()
    }

    /**
     * 获取日志目录
     */
    fun getLogDir(): File = logDir

    /**
     * 关闭所有日志读取器
     */
    @Synchronized
    fun close() {
        // 停止所有监控和读取
        entries.forEach { it.stop() }

        // 等待所有读取器完成
        entries.forEach { entry ->
            try {
                entry.waitFor()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                LOG.warn("Interrupted while waiting for log reader: ${entry.file.name}", e)
            }
        }

        // 删除日志文件
        entries.forEach { entry ->
            if (entry.file.exists()) {
                FileUtil.delete(entry.file)
            }
        }

        entries.clear()
    }

    /**
     * 文本可用时的回调
     *
     * 子类必须实现此方法以处理日志输出
     */
    protected abstract fun onTextAvailable(text: String, type: LogType)

    // ==================== 内部实现 ====================

    /**
     * 日志条目
     *
     * 组合监控器和读取器，管理单个日志文件的完整生命周期
     */
    private inner class LogEntry(
        val file: File,
        private val type: LogType
    ) {
        private val monitor = LogFileMonitor(file) { startReader() }
        private var reader: LogFileReader? = null

        fun start() {
            monitor.start()
        }

        fun stop() {
            monitor.stop()
            reader?.stop()
        }

        @Throws(InterruptedException::class)
        fun waitFor() {
            reader?.waitFor()
        }

        private fun startReader() {
            reader = LogFileReader(file, type) { text, logType ->
                this@LogOutputManager.onTextAvailable(text, logType)
            }
            reader?.start()
        }
    }
}