package com.huawei.cangjie.idea.run.cjpm

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionManager
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.runners.executeState
import com.intellij.execution.ui.RunContentDescriptor
import org.jetbrains.concurrency.resolvedPromise


abstract class CjDefaultProgramRunnerBase : ProgramRunner<RunnerSettings> {

    @Throws(ExecutionException::class)
    override fun execute(environment: ExecutionEnvironment) {
        val state = environment.state ?: return
        @Suppress("UnstableApiUsage")
        ExecutionManager.getInstance(environment.project).startRunProfile(environment) {
            resolvedPromise(doExecute(state, environment))
        }
    }

    protected open fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        return executeState(state, environment, this)
    }
}
