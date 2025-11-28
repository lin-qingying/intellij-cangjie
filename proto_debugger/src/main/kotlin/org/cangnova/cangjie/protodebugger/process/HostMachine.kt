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

package org.cangnova.cangjie.protodebugger.process

import com.intellij.execution.ExecutionException
import com.intellij.execution.Platform
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.execution.process.ProcessInfo
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.system.OS
import org.cangnova.cangjie.protodebugger.pty.NamedPipe
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

val currentOS: OS
    get() =
        when {
            SystemInfo.isWindows -> OS.Windows
            SystemInfo.isMac -> OS.macOS
            else -> OS.Linux
        }

fun OS.toPlatform(): Platform =
    if (this == OS.Windows) Platform.WINDOWS else Platform.UNIX


/**
 * 主机机器抽象接口
 *
 * 提供统一的主机操作接口，支持本地、远程和 WSL 环境
 *
 * 核心职责：
 * - 进程生命周期管理
 * - 文件系统操作
 * - 路径解析和转换
 * - 跨平台兼容性
 */
interface HostMachine {

    // ==================== 主机特性 ====================

    /** 是否为远程主机 */
    val isRemote: Boolean

    /** 是否为 WSL 环境 */
    val isWsl: Boolean get() = false

    /** 是否具有远程文件系统 */
    val hasRemoteFS: Boolean

    /** 操作系统类型 */
    val os: OS

    /** 主机唯一标识 */
    val hostId: String

    /** 主机名称（用于显示） */
    val name: String get() = "${if (isRemote) "Remote" else "Local"} $os"

    // ==================== 进程管理 ====================

    /**
     * 创建进程构建器
     * 用于配置和创建进程处理器
     */
    fun createProcessBuilder(): ProcessBuilder

    /**
     * 运行进程并获取输出
     *
     * @param commandLine 命令行
     * @param indicator 进度指示器（可选）
     * @param timeout 超时时间（毫秒）
     * @return 进程输出
     */
    @Throws(ExecutionException::class)
    fun runProcess(
        commandLine: GeneralCommandLine,
        indicator: ProgressIndicator? = null,
        timeout: Int
    ): ProcessOutput {
        val handler = createProcessBuilder()
            .withCapturedOutput(true)
            .withShortLived(true)
            .withProgressIndicator(indicator)
            .build(commandLine)

        return RunProcessUtil.runProcess(handler, indicator, timeout)
    }

    /**
     * 创建进程（简化版）
     */
    @Throws(ExecutionException::class)
    fun createProcess(
        commandLine: GeneralCommandLine,
        colored: Boolean = false,
        usePty: Boolean = false
    ): BaseProcessHandler<*> = createProcess(commandLine, colored, usePty, true, false, false)

    /**
     * 创建进程（完整版）
     */
    @Throws(ExecutionException::class)
    fun createProcess(
        commandLine: GeneralCommandLine,
        colored: Boolean = false,
        usePty: Boolean = false,
        captureOutput: Boolean = true,
        splitLines: Boolean = false,
        elevated: Boolean = false
    ): BaseProcessHandler<*> {
        return createProcessBuilder()
            .withColoredOutput(colored)
            .withPty(usePty)
            .withCapturedOutput(captureOutput)
            .withSplitToLines(splitLines)
            .withElevated(elevated)
            .build(commandLine)
    }

    /**
     * 销毁进程
     */
    fun destroyProcess(handler: BaseProcessHandler<*>)

    /**
     * 杀死进程树（包括所有子进程）
     */
    fun killProcessTree(handler: BaseProcessHandler<*>)

    /**
     * 获取进程列表
     */
    @Throws(ExecutionException::class)
    fun getProcessList(): List<ProcessInfo>

    /**
     * 发送信号给进程
     *
     * @param pid 进程 ID
     * @param signalName 信号名称（如 "SIGTERM", "SIGKILL"）
     * @param mainProcess 主进程（可选）
     */
    fun sendSignal(pid: Int, signalName: String, mainProcess: Process? = null): Int =
        sendSignal(pid, signalName)

    /**
     * 发送信号（底层实现）
     */
    fun sendSignal(signal: Int, message: String): Int

    // ==================== 文件系统操作 ====================

    /**
     * 构建路径
     */
    fun getPath(first: String, vararg more: String): Path

    /**
     * 转换为规范路径
     *
     * @param paths 路径列表
     * @param resolveSymlink 是否解析符号链接
     */
    fun toCanonicalPath(paths: List<String>, resolveSymlink: Boolean): Array<String>


    /**
     * 解析路径为文件对象
     */
    @Throws(IOException::class)
    fun resolvePath(path: Path): File

    @Throws(IOException::class)
    fun resolvePath(file: File): File

    /**
     * 批量解析并缓存路径
     */
    fun resolveAndCache(paths: List<String>): List<String>

    // ==================== 临时文件和 IPC ====================

    /**
     * 获取临时目录
     */
    fun getTempDirectory(): Path

    /**
     * 创建临时目录
     */
    @Throws(IOException::class)
    fun createTempDirectory(prefix: String, suffix: String? = null): Path

    /**
     * 打开命名管道（用于进程间通信）
     */
    fun openNamedPipe(): NamedPipe

    // ==================== 高级功能 ====================

    /**
     * 查找指定扩展名的文件
     *
     * @param rootPath 根路径
     * @param recursive 是否递归查找
     * @param extensions 文件扩展名列表
     */
    @Throws(IOException::class, ExecutionException::class)
    fun findFilesWithExtension(
        rootPath: Path,
        recursive: Boolean,
        vararg extensions: String
    ): List<Path> {
        if (!Files.exists(rootPath)) return emptyList()

        return if (recursive) {
            findFilesRecursively(rootPath, extensions)
        } else {
            findFilesInDirectory(rootPath, extensions)
        }
    }

    /**
     * 等待文件同步完成（后台线程）
     */
    fun waitForFilesSync()

    /**
     * 清除缓存
     */
    fun invalidateCache() {}

    // ==================== 私有辅助方法 ====================

    private fun findFilesRecursively(rootPath: Path, extensions: Array<out String>): List<Path> {
        val result = mutableListOf<Path>()
        Files.walk(rootPath).use { stream ->
            stream.filter { Files.isRegularFile(it) && hasMatchingExtension(it, extensions) }
                .forEach { result.add(it) }
        }
        return result
    }

    private fun findFilesInDirectory(rootPath: Path, extensions: Array<out String>): List<Path> {
        return Files.list(rootPath).use { stream ->
            stream.filter { Files.isRegularFile(it) && hasMatchingExtension(it, extensions) }
                .toList()
        }
    }

    private fun hasMatchingExtension(file: Path, extensions: Array<out String>): Boolean =
        extensions.any { file.toString().endsWith(it) }
}
