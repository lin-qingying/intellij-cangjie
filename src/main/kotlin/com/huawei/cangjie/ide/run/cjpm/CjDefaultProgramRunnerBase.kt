package com.huawei.cangjie.ide.run.cjpm

import com.intellij.execution.ExecutionManager
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.runners.*
import com.intellij.execution.ui.RunContentDescriptor
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise

//abstract class CjDefaultProgramRunnerBase : ProgramRunner<RunnerSettings> {
//
//    final override fun execute(environment: ExecutionEnvironment) {
//        val state = environment.state ?: return
//        ExecutionManager.getInstance(environment.project).startRunProfile(environment) {
//              resolvedPromise(doExecute(state, environment))
//
//        }
//    }
//
//    protected open fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
//        return executeState(state, environment, this)
//    }
//}

abstract class CjDefaultProgramRunnerBase : GenericProgramRunner<RunnerSettings>() {


    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        return executeState(state, environment, this)
    }
}
