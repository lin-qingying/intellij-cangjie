package com.huawei.cangjie.debugger.runconfig.legacy

import com.huawei.cangjie.debugger.runconfig.CjDebugRunnerUtils
import com.huawei.cangjie.debugger.runconfig.CjDebugRunnerUtils.ERROR_MESSAGE_TITLE
import com.huawei.cangjie.ide.run.cjpm.BuildResult
import com.huawei.cangjie.ide.run.cjpm.CjpmRunStateBase
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise

abstract class CjDebugRunnerLegacyBase : CjAsyncRunner(DefaultDebugExecutor.EXECUTOR_ID, ERROR_MESSAGE_TITLE) {

    override fun getRunnerId(): String = RUNNER_ID
    override fun getRunContentDescriptor(
        state: CjpmRunStateBase,
        environment: ExecutionEnvironment,
        runCommand: GeneralCommandLine
    ): RunContentDescriptor? = CjDebugRunnerUtils.showRunContent(state, environment, runCommand)

    override fun processUnsupportedToolchain(
        project: Project,
        toolchainError: BuildResult.ToolchainError,
        promise: AsyncPromise<CjAsyncRunner.Companion.Binary?>
    ) {
        project.showErrorDialog(toolchainError.message)
        promise.setResult(null)
    }

    companion object {
        const val RUNNER_ID: String = "CjDebugRunnerLegacy"
    }
}
