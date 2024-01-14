package com.huawei.cangjie.debugger.runconfig

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.debugger.CjDebuggerToolchainService
import com.huawei.cangjie.debugger.DebuggerAvailability
import com.huawei.cangjie.debugger.DebuggerKind
import com.huawei.cangjie.debugger.settings.CjDebuggerSettings
import com.huawei.cangjie.idea.run.cjpm.BuildResult
import com.huawei.cangjie.idea.run.cjpm.CjpmRunStateBase
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DoNotAskOption
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.SystemInfo
import org.jetbrains.annotations.Nls

object CjDebugRunnerUtils {
    fun checkToolchainSupported(project: Project, host: String): BuildResult.ToolchainError? {
        if (SystemInfo.isWindows) {


            val isGNURustToolchain = "gnu" in host
            val isMSVCRustToolchain = "msvc" in host
            val isGdbAvailable = CjDebuggerToolchainService.getInstance().gdbAvailability() !is DebuggerAvailability.Unavailable
            val debuggerKind = CjDebuggerSettings.getInstance().debuggerKind

            return when {
                isGNURustToolchain && !isGdbAvailable -> BuildResult.ToolchainError.UnsupportedGNU
                isGNURustToolchain && debuggerKind == DebuggerKind.LLDB -> BuildResult.ToolchainError.MSVCWithRustGNU
                isMSVCRustToolchain && debuggerKind == DebuggerKind.GDB -> BuildResult.ToolchainError.GNUWithRustMSVC
                else -> null
            }
        }
        return null

    }

    fun showRunContent(
        state: CjpmRunStateBase,
        environment: ExecutionEnvironment,
        runExecutable: GeneralCommandLine
    ): RunContentDescriptor {
        TODO()

    }

    private fun showDialog(
        project: Project,
        @Suppress("UnstableApiUsage") @NlsContexts.DialogMessage message: String,
        @Suppress("UnstableApiUsage") @NlsContexts.Button action: String
    ): Boolean {
        val doNotAsk = object : DoNotAskOption.Adapter() {
            override fun rememberChoice(isSelected: Boolean, exitCode: Int) {
                if (exitCode == Messages.OK) {
                    CjDebuggerSettings.getInstance().downloadAutomatically = isSelected
                }
            }
        }

        return MessageDialogBuilder.okCancel(ERROR_MESSAGE_TITLE, message)
            .yesText(action)
            .icon(Messages.getErrorIcon())
            .doNotAsk(doNotAsk)
            .ask(project)
    }

    fun checkToolchainConfigured(project: Project): Boolean {
        val debuggerKind = CjDebuggerSettings.getInstance().debuggerKind
        val debuggerAvailability = CjDebuggerToolchainService.getInstance().debuggerAvailability(debuggerKind)

        val (message, action) = when (debuggerAvailability) {
            DebuggerAvailability.Unavailable -> return false
            DebuggerAvailability.NeedToDownload -> "Debugger is not loaded yet" to "Download"
            DebuggerAvailability.NeedToUpdate -> "Debugger is outdated" to "Update"
            DebuggerAvailability.Bundled,
            is DebuggerAvailability.Binaries<*> -> return true
        }

        val downloadDebugger = if (!CjDebuggerSettings.getInstance().downloadAutomatically) {
            showDialog(project, message, action)
        } else {
            true
        }

        if (downloadDebugger) {
            val result = CjDebuggerToolchainService.getInstance().downloadDebugger(project, debuggerKind)
            if (result is CjDebuggerToolchainService.DownloadResult.Ok) {
                return true
            }
        }
        return false
    }
    @Nls
    val ERROR_MESSAGE_TITLE: String = CangJieBundle.message("unable.to.run.debugger")

}