/*
 * Copyright 2025 LinQingYing.
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
 */

package org.cangnova.cangjie.protodebugger.output

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.BaseOutputReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * 通用日志读取器基类。
 *
 * 自动监控日志目录下的指定日志文件（按类型划分），
 * 当文件出现时异步启动后台读取线程，并回调 [onTextAvailable]。
 *
 * 所有日志文件在关闭时自动停止并清理。
 */
abstract class GLogOutputReaders(
    private val logDir: File,
    @field:NlsSafe val logName: String
) {
    private val readers = CopyOnWriteArrayList<LogReader>()
    private val app = ApplicationManager.getApplication()

    companion object {
        private val LOG = Logger.getInstance(GLogOutputReaders::class.java)
    }

    /**
     * 初始化日志读取器。
     */
    open fun init() {
        readers += LogReader(LogType.INFO)
    }

    /**
     * 日志目录。
     */
    fun getLogDir(): File = logDir

    /**
     * 停止所有日志读取器并删除对应日志文件。
     */
    @Synchronized
    fun close() {
        readers.forEach { it.stop() }

        readers.forEach {
            try {
                it.waitFor()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                LOG.warn("Interrupted while waiting for log reader: ${it.file.name}", e)
            }
        }

        readers.forEach {
            if (it.file.exists()) FileUtil.delete(it.file)
        }

        readers.clear()
    }

    /**
     * 文本输出事件回调。
     */
    protected abstract fun onTextAvailable(text: String, type: LogType)

    // =====================================================
    // 内部实现类
    // =====================================================

    private inner class LogReader(private val type: LogType) {
        val file = File(logDir, "$logName.$type")

        @Volatile private var closed = false
        private var reader: BaseOutputReader? = null
        private var monitorFuture: Future<*>? = null

        init {
            prepareFile()
            startMonitor()
        }

        private fun prepareFile() {
            if (file.exists()) FileUtil.delete(file)
        }

        private fun startMonitor() {
            monitorFuture = app.executeOnPooledThread {
                try {
                    waitUntilExists()
                    if (!closed) startReading()
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                } catch (e: IOException) {
                    LOG.warn("Unable to start reading log file: ${file.name}", e)
                }
            }
        }

        private fun waitUntilExists() {
            while (!closed && !file.exists()) {
                TimeUnit.MILLISECONDS.sleep(100)
            }
        }

        private fun startReading() {
            reader = object : BaseOutputReader(FileInputStream(file), null) {
                init {
                    start("log: ${file.name}")
                }

                override fun onTextAvailable(text: String) {
                    if (!closed) {
                        this@GLogOutputReaders.onTextAvailable(text, type)
                    }
                }

                override fun executeOnPooledThread(runnable: Runnable): Future<*> =
                    app.executeOnPooledThread(runnable)
            }
        }

        fun stop() {
            closed = true
            reader?.stop()
            monitorFuture?.cancel(true)
        }

        @Throws(InterruptedException::class)
        fun waitFor() {
            reader?.waitFor()
        }
    }

    /**
     * 日志类型。
     */
    enum class LogType(@field:NlsSafe private val typeName: String) {
        INFO("INFO"),
        WARNING("WARNING"),
        ERROR("ERROR"),
        FATAL("FATAL");

        override fun toString(): String = typeName
    }
}
