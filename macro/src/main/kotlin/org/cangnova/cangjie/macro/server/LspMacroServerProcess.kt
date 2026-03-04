/*
 * Copyright 2026 LinQingYing. and contributors.
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
 */

package org.cangnova.cangjie.macro.server

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.Kernel32Util
import com.sun.jna.platform.win32.WinError
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference
import org.cangnova.cangjie.macro.messages.CangJieMacroBundle
import org.cangnova.cangjie.toolchain.api.CjSdk
import org.cangnova.namedpipe.WindowsInheritablePipe
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream

/**
 * LSPMacroServer 进程生命周期管理器
 *
 * 负责启动和停止 `LSPMacroServer` 进程。
 * 内部使用 [LspMacroProcessHandler] 管理进程，接入 IDE 生命周期。
 *
 * ## 进程通信方案
 *
 * ### Unix（Linux / macOS）
 * 通过匿名管道（stdin/stdout）通信，fd 0/1 由 [LspMacroProcessHandler] 自动创建：
 * - 客户端写 [outputStream] → 服务端从 fd 0 读
 * - 服务端写 fd 1 → 客户端从 [inputStream] 读
 *
 * ### Windows
 * 使用 [WindowsInheritablePipe] 创建两条可继承匿名管道，
 * 将 HANDLE 整数值作为参数传入子进程：
 * - [toMacroPipe]（父写子读）：IDE → LSPMacroServer
 * - [fromMacroPipe]（子写父读）：LSPMacroServer → IDE
 *
 * ## 启动命令
 *
 * **Linux/macOS**：
 * ```
 * LSPMacroServer 0 1 <enableParallel> <cjcFolder> <ppid>
 * ```
 *
 * **Windows**：
 * ```
 * LSPMacroServer.exe <readHandle> <writeHandle> <enableParallel> <cjcFolder>
 * ```
 *
 * ## 参数说明（Windows）
 *
 * | 位置 | 内容 | 说明 |
 * |------|------|------|
 * | argv[1] | readHandle  | C++ 读端句柄，即 [toMacroPipe] 的读端 HANDLE 整数值 |
 * | argv[2] | writeHandle | C++ 写端句柄，即 [fromMacroPipe] 的写端 HANDLE 整数值 |
 * | argv[3] | enableParallel | 是否启用并行宏，"1" 或 "0" |
 * | argv[4] | cjcFolder   | LSPMacroServer.exe 所在目录，用于推算运行时库路径 |
 */
class LspMacroServerProcess(
    private val sdk: CjSdk,
    private val enableParallel: Boolean = true,
) : Closeable {

    private val logger = Logger.getInstance(LspMacroServerProcess::class.java)

    private var processHandler: LspMacroProcessHandler? = null

    /**
     * Windows 专用：父写子读管道（IDE → LSPMacroServer）
     *
     * - 子进程使用读端（[WindowsInheritablePipe.readHandle]）
     * - 父进程使用写端（[WindowsInheritablePipe.writeHandle]）
     */
    private var toMacroPipe: WindowsInheritablePipe? = null

    /**
     * Windows 专用：子写父读管道（LSPMacroServer → IDE）
     *
     * - 子进程使用写端（[WindowsInheritablePipe.writeHandle]）
     * - 父进程使用读端（[WindowsInheritablePipe.readHandle]）
     */
    private var fromMacroPipe: WindowsInheritablePipe? = null

    /** 进程是否正在运行 */
    val isAlive: Boolean
        get() = processHandler?.process?.isAlive == true

    /**
     * Unix 下子进程的 stdin（客户端写入 → 服务端读取）。
     * Windows 下请使用 [unifiedOutputStream]。
     */
    val outputStream: OutputStream
        get() {
            check(!SystemInfo.isWindows) { "Windows 下请使用 unifiedOutputStream" }
            return processHandler?.process?.outputStream
                ?: error(CangJieMacroBundle.message("macro.lsp.error.not.started"))
        }

    /**
     * Unix 下子进程的 stdout（服务端写入 → 客户端读取）。
     * Windows 下请使用 [unifiedInputStream]。
     */
    val inputStream: InputStream
        get() {
            check(!SystemInfo.isWindows) { "Windows 下请使用 unifiedInputStream" }
            return processHandler?.process?.inputStream
                ?: error(CangJieMacroBundle.message("macro.lsp.error.not.started"))
        }

    /**
     * 跨平台统一读取流（服务端 → 客户端）
     *
     * - Unix：子进程 stdout
     * - Windows：[fromMacroPipe] 读端，通过 JNA ReadFile 包装为 [InputStream]
     */
    val unifiedInputStream: InputStream
        get() = if (SystemInfo.isWindows) {
            fromMacroPipe?.let { HandleInputStream(it.readHandle) }
                ?: error(CangJieMacroBundle.message("macro.lsp.error.not.started"))
        } else {
            inputStream
        }

    /**
     * 跨平台统一写入流（客户端 → 服务端）
     *
     * - Unix：子进程 stdin
     * - Windows：[toMacroPipe] 写端，通过 JNA WriteFile 包装为 [OutputStream]
     */
    val unifiedOutputStream: OutputStream
        get() = if (SystemInfo.isWindows) {
            toMacroPipe?.let { HandleOutputStream(it.writeHandle) }
                ?: error(CangJieMacroBundle.message("macro.lsp.error.not.started"))
        } else {
            outputStream
        }

    /**
     * 启动 LSPMacroServer 进程
     *
     * @throws IllegalStateException 进程已在运行，或可执行文件不存在
     * @throws com.intellij.execution.ExecutionException 进程启动失败
     */
    fun start() {
        check(processHandler == null || !isAlive) {
            CangJieMacroBundle.message("macro.lsp.error.already.running")
        }

        val executable = resolveExecutable()
        check(executable.exists()) {
            CangJieMacroBundle.message("macro.lsp.error.executable.not.found", executable)
        }

        if (SystemInfo.isWindows) {
            startWindows(executable)
        } else {
            startUnix(executable)
        }
    }

    /**
     * 优雅停止进程（等待退出，超时则强制终止）
     *
     * @param timeoutMs 最长等待毫秒数，超时后强制终止
     */
    fun stopGracefully(timeoutMs: Long = 3000) {
        val handler = processHandler ?: return
        if (!isAlive) {
            processHandler = null
            closeWindowsPipes()
            return
        }

        logger.debug("等待 LSPMacroServer 正常退出...")
        handler.destroyProcess()
        if (!handler.waitFor(timeoutMs)) {
            logger.warn("LSPMacroServer 未在 ${timeoutMs}ms 内退出，强制终止")
            handler.process.destroyForcibly()
        }
        processHandler = null
        closeWindowsPipes()
    }

    /** 强制终止进程 */
    fun forceStop() {
        processHandler?.process?.destroyForcibly()
        processHandler = null
        closeWindowsPipes()
        logger.debug("LSPMacroServer 已强制终止")
    }

    override fun close() = stopGracefully()

    // ── 私有方法 ──────────────────────────────────────────────────────────────

    /**
     * Windows 启动流程：
     * 1. 创建两条可继承匿名管道
     * 2. 构建命令行，将子进程使用的两端 HANDLE 整数值作为参数传入
     * 3. 启动进程（GeneralCommandLine 内部 CreateProcess 时 bInheritHandles=TRUE）
     * 4. 关闭父进程不再需要的句柄（子进程使用的那端），避免管道永不关闭
     */
    private fun startWindows(executable: java.io.File) {
        // 管道1：父写 → 子读（对应 C++ argv[1] readHandle）
        val pipe1 = WindowsInheritablePipe()
        // 管道2：子写 → 父读（对应 C++ argv[2] writeHandle）
        val pipe2 = WindowsInheritablePipe()

        toMacroPipe   = pipe1
        fromMacroPipe = pipe2

        logger.debug(
            "Windows 匿名管道已创建: " +
                    "toMacro(read=${pipe1.readHandleValue}, write=${pipe1.writeHandleValue}), " +
                    "fromMacro(read=${pipe2.readHandleValue}, write=${pipe2.writeHandleValue})"
        )

        val commandLine = GeneralCommandLine().apply {
            exePath = executable.absolutePath
            addParameters(
                // argv[1]：C++ 读端（子进程从这里读 IDE 发来的数据）
                pipe1.readHandleValue.toString(),
                // argv[2]：C++ 写端（子进程向这里写，IDE 从 pipe2 读端读）
                pipe2.writeHandleValue.toString(),
                if (enableParallel) "1" else "0",
                // argv[4]：传可执行文件所在目录（tools/bin），
                // C++ 端 GetRuntimeLibPath() 基于此路径推算运行时库位置，
                // 避免路径中出现多余的 ".." 导致 LoadLibrary 失败
                executable.parent
            )
            withEnvironment(sdk.getEnvironment())
        }

        processHandler = LspMacroProcessHandler(commandLine).also { it.startNotify() }

        // 子进程已启动，关闭父进程持有的子进程那端句柄
        // 不关闭会导致管道引用计数不归零，子进程退出后 ReadFile/WriteFile 永不返回 EOF
        runCatching { Kernel32Util.closeHandle(pipe1.readHandle) }
            .onFailure { logger.warn("关闭 pipe1 读端失败", it) }
        runCatching { Kernel32Util.closeHandle(pipe2.writeHandle) }
            .onFailure { logger.warn("关闭 pipe2 写端失败", it) }

        logger.info("LSPMacroServer 已启动（Windows）")
    }

    /**
     * Unix 启动流程：
     * 使用 stdin(fd=0)/stdout(fd=1) 作为通信管道，由 ProcessBuilder 自动创建。
     */
    private fun startUnix(executable: java.io.File) {
        val ppid = ProcessHandle.current().pid().toString()
        val commandLine = GeneralCommandLine().apply {
            exePath = executable.absolutePath
            addParameters(
                "0",                                    // argv[1]：stdin，服务端读端
                "1",                                    // argv[2]：stdout，服务端写端
                if (enableParallel) "1" else "0",      // argv[3]：是否并行
                executable.parent,                      // argv[4]：可执行文件所在目录
                ppid                                    // argv[5]：父进程 PID，供服务端监控存活
            )
            withEnvironment(sdk.getEnvironment())
        }
        processHandler = LspMacroProcessHandler(commandLine).also { it.startNotify() }
        logger.info("LSPMacroServer 已启动（Unix）")
    }

    /** 解析 LSPMacroServer 可执行文件路径 */
    private fun resolveExecutable(): java.io.File =
        sdk.getExecutable("LSPMacroServer", "tools", "bin").toFile()

    /** 关闭 Windows 管道，释放父进程持有的句柄 */
    private fun closeWindowsPipes() {
        runCatching { toMacroPipe?.close() }
        runCatching { fromMacroPipe?.close() }
        toMacroPipe   = null
        fromMacroPipe = null
    }
}

// ── Windows HANDLE → InputStream/OutputStream 包装 ────────────────────────────

/**
 * 将 Windows HANDLE 包装为标准 [InputStream]
 *
 * 通过 JNA ReadFile 实现读取，供 PipeTransport 在 Windows 下使用。
 * 当管道对端关闭时（ERROR_BROKEN_PIPE / 109），read 返回 -1（EOF）。
 */
private class HandleInputStream(private val handle: WinNT.HANDLE) : InputStream() {
    private val k32 = Kernel32.INSTANCE

    override fun read(): Int {
        val buf = ByteArray(1)
        val n = read(buf, 0, 1)
        return if (n == -1) -1 else buf[0].toInt() and 0xFF
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        val buf = ByteArray(len)
        val read = IntByReference()
        val ok = k32.ReadFile(handle, buf, len, read, null)
        if (!ok) {
            val err = k32.GetLastError()
            // ERROR_BROKEN_PIPE(109)：管道对端已关闭，视为 EOF
            if (err == WinError.ERROR_BROKEN_PIPE || err == 109) return -1
            throw java.io.IOException("ReadFile 失败，错误码=$err")
        }
        val n = read.value
        if (n == 0) return -1
        System.arraycopy(buf, 0, b, off, n)
        return n
    }
}

/**
 * 将 Windows HANDLE 包装为标准 [OutputStream]
 *
 * 通过 JNA WriteFile 实现写入，供 PipeTransport 在 Windows 下使用。
 */
private class HandleOutputStream(private val handle: WinNT.HANDLE) : OutputStream() {
    private val k32 = Kernel32.INSTANCE

    override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)

    override fun write(b: ByteArray, off: Int, len: Int) {
        if (len == 0) return
        val buf = b.copyOfRange(off, off + len)
        val written = IntByReference()
        val ok = k32.WriteFile(handle, buf, len, written, null)
        if (!ok) throw java.io.IOException("WriteFile 失败，错误码=${k32.GetLastError()}")
    }

    override fun flush() {
        k32.FlushFileBuffers(handle)
    }
}