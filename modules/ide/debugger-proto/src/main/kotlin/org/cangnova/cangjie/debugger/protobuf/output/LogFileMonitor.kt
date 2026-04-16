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

package org.cangnova.cangjie.debugger.protobuf.output

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * 日志文件监控器
 *
 * 职责：
 * - 监控指定日志文件是否出现
 * - 文件出现时触发回调
 * - 支持停止监控
 */
class LogFileMonitor(
    private val file: File,
    private val onFileAppeared: (File) -> Unit
) {
    companion object {
        private val LOG = Logger.getInstance(LogFileMonitor::class.java)
        private const val CHECK_INTERVAL_MS = 100L
    }

    @Volatile
    private var stopped = false
    private var monitorFuture: Future<*>? = null

    /**
     * 启动监控
     */
    fun start() {
        if (monitorFuture != null) {
            LOG.warn("Monitor already started for file: ${file.name}")
            return
        }

        monitorFuture = ApplicationManager.getApplication().executeOnPooledThread {
            try {
                waitUntilFileExists()
                if (!stopped) {
                    onFileAppeared(file)
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                LOG.debug("Monitor interrupted for file: ${file.name}")
            } catch (e: Exception) {
                LOG.error("Monitor failed for file: ${file.name}", e)
            }
        }
    }

    /**
     * 停止监控
     */
    fun stop() {
        stopped = true
        monitorFuture?.cancel(true)
        monitorFuture = null
    }

    /**
     * 等待文件出现
     */
    private fun waitUntilFileExists() {
        while (!stopped && !file.exists()) {
            TimeUnit.MILLISECONDS.sleep(CHECK_INTERVAL_MS)
        }
    }

    /**
     * 检查是否已停止
     */
    fun isStopped(): Boolean = stopped
}