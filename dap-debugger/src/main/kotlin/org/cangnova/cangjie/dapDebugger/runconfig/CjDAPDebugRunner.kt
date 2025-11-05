package org.cangnova.cangjie.dapDebugger.runconfig

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import org.cangnova.cangjie.run.CangJieProgramRunConfiguration
import org.cangnova.cangjie.run.CangJieProgramRunState
import org.cangnova.cangjie.run.CangJieRunConfigurationBase
import org.cangnova.cangjie.run.CangJieRunState
import org.cangnova.cangjie.run.CjExecutableRunner
import java.io.File


private const val RUNNER_ID = "CjDAPDebugRunner"

class CjDAPDebugRunner : CjExecutableRunner(DefaultDebugExecutor.EXECUTOR_ID, CjDebugRunnerUtils.ERROR_MESSAGE_TITLE) {
    override fun getRunnerId(): String = RUNNER_ID
    override fun canRun(executorId: String, profile: RunProfile): Boolean {

        return profile is CangJieProgramRunConfiguration && super.canRun(executorId, profile)
    }

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (state !is CangJieProgramRunState) return null


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
            withEnvironment(state.configuration.env.envs)
        }

        return CjDebugRunnerUtils.showRunContent(state, environment, runExecutable)
    }


}

