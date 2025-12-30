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

package org.cangnova.cangjie.debugger.protobuf.process

import com.intellij.execution.ExecutionException
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ArrayUtil
import com.intellij.util.io.BaseDataReader.SleepingPolicy
import com.intellij.util.io.BaseOutputReader
import com.intellij.util.system.OS
import com.pty4j.unix.Pty
import org.cangnova.cangjie.debugger.protobuf.pty.NamedPipe
import org.cangnova.cangjie.debugger.protobuf.pty.WindowsPipe
import org.cangnova.cangjie.debugger.protobuf.services.impl.IOResourceManager
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference


/**
 * 进程输出读取器抽象类
 *
 * 负责管理和读取被调试进程的标准输出和标准错误流，支持跨平台的输出读取方式。
 *
 * @property host 主机机器信息
 * @property presentableName 可显示的名称，用于日志和调试
 * @property charset 字符编码
 * @property usePtyOnUnix 在Unix系统上是否使用PTY
 * @property emulateTerminal 是否模拟终端环境
 */
abstract class ProcessOutputReaders(
    val host: HostMachine,
    val presentableName: String,
    val charset: Charset,
    val usePtyOnUnix: Boolean,
    val emulateTerminal: Boolean = false
) {
    private val readers = AtomicReference<Array<OutputReader?>>(arrayOfNulls(READER_COUNT))

    init {
        initializeReaders()
    }

    /**
     * 初始化输出读取器
     */
    private fun initializeReaders() {
        try {
            val readerArray = readers.get()
            createReaders(readerArray)
            startReaders(readerArray)
        } catch (ioEx: IOException) {
            throw ExecutionException("Cannot create output file", ioEx)
        }
    }

    /**
     * 根据平台创建合适的读取器
     */
    private fun createReaders(readerArray: Array<OutputReader?>) {
        when {
            host.os == OS.Windows -> createWindowsReaders(readerArray)
            usePtyOnUnix && host.isRemote -> createRemoteUnixReaders(readerArray)
            usePtyOnUnix -> createUnixPtyReaders(readerArray)
            else -> createFileReaders(readerArray)
        }
    }

    /**
     * 创建Windows管道读取器
     */
    private fun createWindowsReaders(readerArray: Array<OutputReader?>) {
      try {
          readerArray[STDOUT_INDEX] = WindowsPipeOutputReader(ProcessOutputTypes.STDOUT)
          if (!emulateTerminal) {
              readerArray[STDERR_INDEX] = WindowsPipeOutputReader(ProcessOutputTypes.STDERR)
          }
      } catch (e: UnsatisfiedLinkError) {
           LOG.warn("Failed to load Kernel32 native library: ${e.message}", e)

      } catch (e: NoClassDefFoundError) {
          LOG.warn("Kernel32 class not found: ${e.message}", e)

      } catch (e: Exception) {
          LOG.error("Unexpected error while setting up Windows pipe: ${e.message}", e)

      }
    }

    /**
     * 创建远程Unix管道读取器
     */
    private fun createRemoteUnixReaders(readerArray: Array<OutputReader?>) {
        readerArray[STDOUT_INDEX] = RemoteUnixPipeOutputReader(host, ProcessOutputTypes.STDOUT)
        readerArray[STDERR_INDEX] = RemoteUnixPipeOutputReader(host, ProcessOutputTypes.STDERR)
    }

    /**
     * 创建Unix PTY读取器
     */
    private fun createUnixPtyReaders(readerArray: Array<OutputReader?>) {
        readerArray[STDOUT_INDEX] = UnixPtyOutputReader(ProcessOutputTypes.STDOUT)
        readerArray[STDERR_INDEX] = UnixPtyOutputReader(ProcessOutputTypes.STDERR)
    }

    /**
     * 创建文件读取器
     */
    private fun createFileReaders(readerArray: Array<OutputReader?>) {
        readerArray[STDOUT_INDEX] = FileOutputReader(ProcessOutputTypes.STDOUT)
        readerArray[STDERR_INDEX] = FileOutputReader(ProcessOutputTypes.STDERR)
    }

    /**
     * 启动所有读取器
     */
    private fun startReaders(readerArray: Array<OutputReader?>) {
        readerArray.forEach { it?.start() }
    }

    /**
     * 获取指定编号的输出读取器
     *
     * @param num 读取器编号（0=STDOUT，1=STDERR）
     * @return 对应的输出读取器实例
     * @throws ExecutionException 当读取器不可用时抛出
     */
    @Throws(ExecutionException::class)
    protected fun getReader(num: Int): OutputReader {
        val currentReaders = readers.get()
        if (ArrayUtil.isEmpty(currentReaders)) {
            throw ExecutionException("Reader is closed")
        }
        return currentReaders[num] ?: throw ExecutionException("Reader at index $num is null")
    }

    /**
     * 获取标准输出文件的绝对路径
     */
    @Throws(ExecutionException::class)
    fun getOutFileAbsolutePath(): String = getReader(STDOUT_INDEX).fileAbsolutePath

    /**
     * 获取标准错误文件的绝对路径
     */
    @Throws(ExecutionException::class)
    fun getErrFileAbsolutePath(): String = getReader(STDERR_INDEX).fileAbsolutePath

    /**
     * 等待所有输出读取器完成
     *
     * @return 如果所有读取器都正常完成返回true，否则返回false
     */
    fun waitFor(): Boolean = doWaitFor(readers.get())

    /**
     * 带超时的等待
     */
    fun waitFor(timeout: Long, unit: TimeUnit): Boolean =
        doWaitFor(readers.get(), timeout, unit)

    /**
     * 等待所有读取器完成（内部实现）
     */
    private fun doWaitFor(readerArray: Array<OutputReader?>): Boolean {
        return readerArray.all { reader ->
            runCatching {
                reader?.waitFor()
                true
            }.recover { exception ->
                if (exception is InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                false
            }.getOrDefault(false)
        }
    }

    /**
     * 带超时的等待（内部实现）
     */
    private fun doWaitFor(
        readerArray: Array<OutputReader?>,
        timeout: Long,
        unit: TimeUnit
    ): Boolean {
        return readerArray.all { reader ->
            runCatching {
                reader?.waitFor(timeout, unit)
                true
            }.recover { exception ->
                when (exception) {
                    is InterruptedException -> {
                        Thread.currentThread().interrupt()
                        false
                    }

                    is TimeoutException -> false
                    else -> throw exception
                }
            }.getOrDefault(false)
        }
    }

    /**
     * 关闭所有读取器
     */
    fun close() {
        val currentReaders = readers.getAndSet(arrayOfNulls(1)) ?: return
        currentReaders.forEach { it?.stop() }
        doWaitFor(currentReaders)
    }

    /**
     * 文本可用时的回调
     */
    protected abstract fun onTextAvailable(text: String, key: Key<*>)

    // ==================== 内部类 ====================

    /**
     * 读取器选项配置
     */
    private class ReaderOptions(private val policy: SleepingPolicy) : BaseOutputReader.Options() {
        override fun policy(): SleepingPolicy = policy
        override fun splitToLines(): Boolean = false
    }

    /**
     * 基础输出读取器
     */
    protected open inner class OutputReader(
        val type: Key<*>,
        val fileAbsolutePath: String,
        stream: InputStream,
        sleepingPolicy: SleepingPolicy
    ) : BaseOutputReader(stream, charset, ReaderOptions(sleepingPolicy)) {

        fun start() {
            start(presentableName)
        }

        override fun onTextAvailable(text: String) {
            val processedText = when {
                emulateTerminal -> text
                else -> text.trimEnd('\r').let(StringUtil::convertLineSeparators)
            }
            this@ProcessOutputReaders.onTextAvailable(processedText, type)
        }

        override fun executeOnPooledThread(runnable: Runnable): Future<*> =
            ApplicationManager.getApplication().executeOnPooledThread(runnable)

        override fun stop() {
            super.stop()
            if (mySleepingPolicy == SleepingPolicy.BLOCKING) {
                handleBlockingStop()
            }
        }

        private fun handleBlockingStop() {
            runCatching {
                waitFor(mySleepingPolicy.getTimeToSleep(false).toLong(), TimeUnit.MILLISECONDS)
            }.onFailure { exception ->
                when (exception) {
                    is TimeoutException, is InterruptedException -> closeQuietly()
                    else -> throw exception
                }
            }
        }

        protected fun closeQuietly() {
            runCatching { close() }.onFailure { LOG.error(it) }
        }
    }

    /**
     * 管道输出读取器
     */
    protected open inner class PipeOutputReader(
        val pipe: NamedPipe,
        type: Key<*>
    ) : OutputReader(type, pipe.name, pipe.inputStream, SleepingPolicy.BLOCKING) {

        @Throws(IOException::class)
        override fun close() {
            runCatching { super.close() }
                .also { pipe.close() }
                .getOrThrow()
        }
    }

    /**
     * Windows管道输出读取器
     */
    protected inner class WindowsPipeOutputReader(type: Key<*>) :
        PipeOutputReader(WindowsPipe.createInboundPipe(type.toString()), type) {

        override fun doRun() {
            if (connectToPipe()) {
                super.doRun()
            }
        }

        private fun connectToPipe(): Boolean = runCatching {
            pipe.waitForConnection().also { connected ->
                if (!connected) {
                    LOG.error("Stream reading can't be initiated: couldn't connect to pipe ${pipe.name}")
                }
            }
        }.onFailure { exception ->
            LOG.debug(exception)
            closeQuietly()
        }.getOrDefault(false)
    }

    /**
     * Unix PTY输出读取器
     */
    protected inner class UnixPtyOutputReader private constructor(
        private val pty: Pty,
        type: Key<*>
    ) : OutputReader(type, pty.slaveName, pty.inputStream, SleepingPolicy.BLOCKING) {

        constructor(type: Key<*>) : this(Pty(true), type)

        override fun close() {
            runCatching { super.close() }
                .also { runCatching { pty.close() } }
                .getOrThrow()
        }
    }

    /**
     * 远程Unix管道输出读取器
     */
    protected inner class RemoteUnixPipeOutputReader(
        host: HostMachine,
        type: Key<*>
    ) : PipeOutputReader(host.openNamedPipe(), type)

    /**
     * 文件输出读取器
     */
    protected inner class FileOutputReader private constructor(
        private val file: File,
        type: Key<*>
    ) : OutputReader(type, file.absolutePath, FileInputStream(file), SleepingPolicy.NON_BLOCKING) {

        constructor(type: Key<*>) : this(
            FileUtil.createTempFile(
                ProcessOutputReaders::class.java.simpleName,
                type.toString(),
                true
            ),
            type
        )

        @Throws(IOException::class)
        override fun close() {
            runCatching { super.close() }
                .also { FileUtil.delete(file) }
                .getOrThrow()
        }
    }

    companion object {
        private val LOG = Logger.getInstance(ProcessOutputReaders::class.java)
        private const val READER_COUNT = 2
        private const val STDOUT_INDEX = 0
        private const val STDERR_INDEX = 1
    }
}