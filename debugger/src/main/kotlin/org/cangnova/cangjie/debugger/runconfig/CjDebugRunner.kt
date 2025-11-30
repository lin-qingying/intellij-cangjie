package org.cangnova.cangjie.debugger.runconfig

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import kotlinx.coroutines.runBlocking
import org.cangnova.cangjie.debugger.messages.DebuggerBundle
import org.cangnova.cangjie.debugger.toolchain.CjDebuggerToolchainService
import org.cangnova.cangjie.debugger.toolchain.DebuggerAvailability
import org.cangnova.cangjie.project.model.cjSdk
import org.cangnova.cangjie.run.CangJieProgramRunConfiguration
import org.cangnova.cangjie.run.CangJieProgramRunState
import org.cangnova.cangjie.run.CjExecutableRunner
import java.io.File

private const val RUNNER_ID = "CjDebugRunner"


class CjDebugRunner : CjExecutableRunner(DefaultDebugExecutor.EXECUTOR_ID, CjDebugRunnerUtils.ERROR_MESSAGE_TITLE) {
    override fun getRunnerId(): String = RUNNER_ID
    override fun canRun(executorId: String, profile: RunProfile): Boolean {

        return profile is CangJieProgramRunConfiguration && super.canRun(executorId, profile)
    }

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (state !is CangJieProgramRunState) return null

        // 检查调试器是否配置
        if (!checkDebuggerConfigured(environment.project)) {
            return null
        }

        if (state.configuration.buildConfiguration?.runState == null) return null

        val buildEnvironment = state.configuration.buildConfiguration?.runState!!.environment

        val artifacts = buildEnvironment.artifacts.orEmpty()
        if (artifacts.isEmpty()) {
            // No artifacts available, run the configuration directly
            return super.doExecute(state, environment)
        }

        val artifact = artifacts.firstOrNull() ?: return null
        val binaries = artifact.executables

        if (binaries.isEmpty()) {
            return super.doExecute(state, environment)
        }

        val runExecutable = GeneralCommandLine().apply {
            exePath = binaries.first()
            workDirectory = state.configuration.workingDirectory?.toFile() ?: File(environment.project.basePath ?: ".")


            // Add environment variables
            val sdkEnv = environment.project.cjSdk?.getEnvironment() ?: emptyMap()
            withEnvironment(state.configuration.env.envs + sdkEnv)
        }

        return CjDebugRunnerUtils.showRunContent(state, environment, runExecutable)
    }

    /**
     * 检查调试器是否已配置
     *
     * 参考 Rust 插件的实现
     *
     * @param project 当前项目
     * @return true 如果调试器可用，否则 false
     */
    private fun checkDebuggerConfigured(project: Project): Boolean {
        val debuggerProvider = project.getDebuggerProvider()
        val debuggerAvailability = runBlocking {
            CjDebuggerToolchainService.getInstance().debuggerAvailability(debuggerProvider)
        }

        val (message, action) = when (debuggerAvailability) {
            DebuggerAvailability.Unavailable -> return false
            DebuggerAvailability.NeedToDownload ->
                DebuggerBundle.message("debugger.not.loaded") to DebuggerBundle.message("debugger.download")
            DebuggerAvailability.NeedToUpdate ->
                DebuggerBundle.message("debugger.outdated") to DebuggerBundle.message("debugger.update")
            DebuggerAvailability.Available -> return true
        }

        // TODO: 检查是否自动下载设置
        // val downloadAutomatically = CjDebuggerSettings.getInstance().downloadAutomatically
        val downloadAutomatically = false

        val downloadDebugger =   showDialog(project, message, action)



        if (downloadDebugger) {
            val result = CjDebuggerToolchainService.getInstance().downloadDebugger(project, debuggerProvider)
            if (result is CjDebuggerToolchainService.DownloadResult.Ok) {
                return true
            }
        }
        return false
    }

    /**
     * 显示确认对话框
     */
    private fun showDialog(project: Project, message: String, action: String): Boolean {
        val result = Messages.showOkCancelDialog(
            project,
            message,
            DebuggerBundle.message("debugger.setup.required"),
            action,
            Messages.getCancelButton(),
            Messages.getWarningIcon()
        )
        return result == Messages.OK
    }
}