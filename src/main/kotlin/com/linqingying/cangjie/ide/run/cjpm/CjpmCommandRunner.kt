package com.linqingying.cangjie.ide.run.cjpm

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.executeState
import com.intellij.execution.target.RunTargetsEnabled
import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.execution.target.TargetEnvironmentsManager
import com.intellij.execution.ui.RunContentDescriptor
import com.linqingying.cangjie.cjpm.toolchain.impl.PathConverter
import com.linqingying.cangjie.ide.run.cjpm.runconfig.CjRunConfigurationExtensionManager
import com.linqingying.cangjie.ide.run.cjpm.runconfig.ConfigurationExtensionContext
import com.linqingying.cangjie.ide.run.cjpm.runconfig.buildtool.CjpmBuildManager.getBuildConfiguration
import com.linqingying.cangjie.ide.run.cjpm.runconfig.buildtool.CjpmBuildManager.isBuildConfiguration
import com.linqingying.cangjie.ide.run.cjpm.runconfig.buildtool.CjpmBuildManager.isBuildToolWindowAvailable

import com.linqingying.cangjie.ide.run.hasRemoteTarget
import com.linqingying.cangjie.ide.run.startProcess


open class CjpmCommandRunner : CjExecutableRunner(DefaultRunExecutor.EXECUTOR_ID, "") {
    override fun canRun(executorId: String, profile: RunProfile): Boolean {

        if (executorId != DefaultRunExecutor.EXECUTOR_ID || profile !is CjpmCommandConfiguration) return false
        val cleaned = profile.clean().ok ?: return false
        val isLocalRun = !profile.hasRemoteTarget || profile.buildTarget.isRemote
        val isLegacyTestRun = !profile.isBuildToolWindowAvailable &&
                cleaned.cmd.command in listOf("test") &&
                getBuildConfiguration(profile) != null
        return isLocalRun && !isLegacyTestRun
    }


    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        val configuration = environment.runProfile
        return if (configuration is CjpmCommandConfiguration &&
            !(isBuildConfiguration(configuration) && configuration.isBuildToolWindowAvailable)
        ) {
//            super.doExecute(state, environment)

//            if (configuration.command == "run") {
//                val binaries = environment.binaries
//
//
//                if (binaries.isNotEmpty()) {
//
//                    state.setCommand(binaries.first())
//                }
//            }
            executeState(state, environment, this)


        } else {

            environment.putUserData(ExecutionManagerImpl.EXECUTION_SKIP_RUN, true)
            null
        }
    }

    override fun getRunnerId(): String = RUNNER_ID

    companion object {

        val RUNNER_ID: String = "CjpmCommandRunner"

    }
}


sealed class CompilerMessage {
//    abstract val package_id: PackageId

    abstract fun convertPaths(converter: PathConverter): CompilerMessage

    companion object {
        fun fromJson(json: JsonObject): CompilerMessage? {
            val reason = json.getAsJsonPrimitive("reason")?.asString ?: return null
            val cls: Class<out CompilerMessage> = when (reason) {
//                BuildScriptMessage.REASON -> BuildScriptMessage::class.java
                CompilerArtifactMessage.REASON -> CompilerArtifactMessage::class.java
                else -> return null
            }
            return Gson().fromJson(json, cls)
        }
    }
}


data class CompilerArtifactMessage(
//    override val package_id: PackageId,
//    val target: CjpmMetadata.Target,
//    val profile: Profile,
//    val filenames: List<String>,
    val executable: String?
) : CompilerMessage() {

    val executables: List<String>
        get() {
            return if (executable != null) {
                listOf(executable)
            } else {
                listOf()
//                filenames.filter { !it.endsWith(".cjo")   }
            }
        }

    override fun convertPaths(converter: PathConverter): CompilerArtifactMessage = copy(
//        target = target.convertPaths(converter),
//        filenames = filenames.map(converter),
        executable = executable?.let(converter)
    )

    companion object {
        const val REASON: String = "compiler-artifact"

        fun fromJson(json: JsonObject): CompilerArtifactMessage? {
            if (json.getAsJsonPrimitive("reason").asString != REASON) {
                return null
            }
            return Gson().fromJson(json, CompilerArtifactMessage::class.java)
        }
    }
}

fun CjpmRunStateBase.executeCommandLine(
    commandLine: GeneralCommandLine,
    environment: ExecutionEnvironment
): DefaultExecutionResult {
    val runConfiguration = configuration
    val targetEnvironment = runConfiguration.targetEnvironment
    val context = ConfigurationExtensionContext()

    val extensionManager = CjRunConfigurationExtensionManager.getInstance()
    extensionManager.patchCommandLine(runConfiguration, environment, commandLine, context)
    extensionManager.patchCommandLineState(runConfiguration, environment, this, context)
    val handler =
        commandLine.startProcess(environment.project, targetEnvironment, processColors = true, uploadExecutable = true)
    extensionManager.attachExtensionsToProcess(runConfiguration, handler, environment, context)

    val console = consoleBuilder.console
    handler.let { console.attachToProcess(it) }
    return DefaultExecutionResult(console, handler)

}

val CjpmCommandConfiguration.targetEnvironment: TargetEnvironmentConfiguration?
    get() {
        if (!RunTargetsEnabled.get()) return null
        val targetName = defaultTargetName ?: return null
        return TargetEnvironmentsManager.getInstance(project).targets.findByName(targetName)
    }


fun RunProfileState.setCommand(command: String) {
    if (this !is CjpmRunStateBase) return

    this.commandLine.command = command
    this.configuration.command = command
}
