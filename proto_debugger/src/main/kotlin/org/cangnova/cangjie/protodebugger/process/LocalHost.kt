package org.cangnova.cangjie.protodebugger.process

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*


import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.Alarm
import com.intellij.util.io.BaseOutputReader.Options
import org.cangnova.cangjie.messages.DebuggerBundle
import org.cangnova.cangjie.protodebugger.ipc.NamedPipe
import org.cangnova.cangjie.protodebugger.util.sudo
import org.cangnova.cangjie.protodebugger.util.sudoCommand


import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 本地主机实现
 *
 * 在本地机器上执行所有操作的 HostMachine 实现
 */
object LocalHost : HostMachine {

    private val LOG = Logger.getInstance(LocalHost::class.java)
    private const val SUDO_KILL_DELAY_MS = 1000

    // ==================== 主机特性 ====================

    override val isRemote: Boolean = false
    override val hasRemoteFS: Boolean = false
    override val osType: OSType = OSType.current
    override val hostId: String = "__localhost_machine__"

    // ==================== 进程管理 ====================

    override fun createProcessBuilder():ProcessBuilder = object :ProcessBuilder(){
        override fun build(
            cmd: GeneralCommandLine,
            parameters: Parameters
        ): BaseProcessHandler<*> {
            // Note: External console support temporarily disabled due to internal API usage
            // TODO: Find public API alternative for WinRunnerMediator.withExternalConsole

            val handler = when {
                parameters.isElevated -> createElevatedProcessHandler(cmd)
                else -> createStandardProcessHandler(cmd, parameters)
            }

            // 设置 PTY 标志
            if (handler is OSProcessHandler) {
                RunProcessUtil.setHasPty(handler, parameters.isPty)
            }

            return handler
        }


        private fun createStandardProcessHandler(
            cmd: GeneralCommandLine,
            params: ProcessBuilder.Parameters
        ): BaseProcessHandler<*> = when {
            params.isEmulateTerminal -> TerminalEmulatorHandler(cmd)
            params.isColored -> ColoredHandler(cmd, params)
            params.isCapturedOutput -> CapturingHandler(cmd, params)
            else -> StandardHandler(cmd, params)
        }

        private fun createElevatedProcessHandler(cmd: GeneralCommandLine): BaseProcessHandler<*> {
            return if (Registry.`is`("cidr.elevation.daemon.enabled", true)) {
                ElevationService.getInstance().createProcessHandler(cmd)
            } else {
                createSudoProcessHandler(cmd)
            }
        }

        private fun createSudoProcessHandler(cmd: GeneralCommandLine): OSProcessHandler {
            val sudoCmd = try {
                 sudoCommand(cmd, DebuggerBundle.message("sudo.prompt"))
            } catch (e: IOException) {
                throw ExecutionException(
                    DebuggerBundle.message("sudo.error", cmd),
                    e
                )
            }

            return when {
                SystemInfo.isWindows -> OSProcessHandler(sudoCmd)
                else -> SudoProcessHandler(sudoCmd)
            }
        }
    }

    override fun destroyProcess(handler: BaseProcessHandler<*>) {
        handler.destroyProcess()
    }

    override fun killProcessTree(handler: BaseProcessHandler<*>) {
        OSProcessUtil.killProcessTree(handler.process)
    }

    override fun getProcessList(): List<ProcessInfo> =
        OSProcessUtil.getProcessList().toList()

    override fun sendSignal(signal: Int, message: String): Int {
        require(osType != OSType.WIN) {
            "Not supported for Windows OS, use winbreak instead"
        }
        return UnixProcessManager.sendSignal(signal, message)
    }

    // ==================== 文件系统操作 ====================

    override fun getPath(first: String, vararg more: String): Path =
        Paths.get(first, *more)

    override fun toCanonicalPath(paths: List<String>, resolveSymlink: Boolean): Array<String> =
        paths.map { FileUtil.toCanonicalPath(it, resolveSymlink) }.toTypedArray()

    override fun getTempDirectory(): Path =
        Paths.get(FileUtil.getTempDirectory())

    override fun createTempDirectory(prefix: String, suffix: String?): Path =
        FileUtil.createTempDirectory(prefix, suffix).toPath()

    override fun openNamedPipe(): NamedPipe {
        throw UnsupportedOperationException("Named pipes not supported on local host")
    }

    override fun resolvePath(path: Path): File = path.toFile()

    override fun resolvePath(file: File): File = file

    override fun resolveAndCache(paths: List<String>): List<String> = paths

    override fun waitForFilesSync() {
        ApplicationManager.getApplication().assertIsNonDispatchThread()
    }



    // ==================== 进程处理器实现 ====================

    /**
     * 标准进程处理器
     */
    private class StandardHandler(
        cmd: GeneralCommandLine,
        private val params: ProcessBuilder.Parameters
    ) : OSProcessHandler(cmd) {

        override fun readerOptions(): Options =
            buildReaderOptions(params, super.readerOptions())
    }

    /**
     * 捕获输出的进程处理器
     */
    private class CapturingHandler(
        cmd: GeneralCommandLine,
        private val params: ProcessBuilder.Parameters
    ) : CapturingProcessHandler(cmd) {

        override fun readerOptions(): Options =
            buildReaderOptions(params, super.readerOptions())
    }

    /**
     * 彩色输出的进程处理器
     */
    private class ColoredHandler(
        cmd: GeneralCommandLine,
        private val params: ProcessBuilder.Parameters
    ) : ColoredProcessHandler(cmd) {

        override fun readerOptions(): Options =
            buildReaderOptions(params, super.readerOptions())
    }

    /**
     * 终端模拟器处理器
     */
    private class TerminalEmulatorHandler(
        cmd: GeneralCommandLine
    ) : OSProcessHandler(cmd) {

        override fun readerOptions(): Options =
            Options.forTerminalPtyProcess()
    }

    /**
     * Sudo 进程处理器
     *
     * 处理需要提升权限的进程，支持延迟 sudo terminate
     */
    private class SudoProcessHandler(
        cmd: GeneralCommandLine
    ) : KillableProcessHandler(cmd) {

        private val sudoKillAlarm = Alarm()

        override fun doDestroyProcess() {
            scheduleSudoKill()
            super.doDestroyProcess()
        }

        override fun killProcess() {
            scheduleSudoKill()
            super.killProcess()
        }

        override fun onOSProcessTerminated(exitCode: Int) {
            sudoKillAlarm.cancelAllRequests()
            super.onOSProcessTerminated(exitCode)
        }

        private fun scheduleSudoKill() {
            sudoKillAlarm.addRequest(
                { runSudoKill(process) },
                SUDO_KILL_DELAY_MS,
                ModalityState.any()
            )
        }

        private fun runSudoKill(process: Process) {
            if (!process.isAlive) return

            try {
                val killCmd = GeneralCommandLine("terminate", process.pid().toString())
              sudo(killCmd, DebuggerBundle.message("sudo.prompt"))
            } catch (e: IOException) {
                LOG.error("Failed to terminate process ${process.pid()}", e)
            }
        }
    }

    // ==================== 辅助函数 ====================

    /**
     * 构建读取器选项
     */
    private fun buildReaderOptions(params: ProcessBuilder.Parameters, base: Options): Options {
        var options = base

        if (params.isPty) {
            options = Options.forTerminalPtyProcess()
        }

//        if (params.isSplitToLines) {
//            options = options.withSeparateOutputForStderr(true)
//        }

        return options
    }
}