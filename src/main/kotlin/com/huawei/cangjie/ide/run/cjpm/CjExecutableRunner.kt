package com.huawei.cangjie.ide.run.cjpm

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.ide.run.cjpm.runconfig.buildtool.CjpmBuildManager.getBuildConfiguration
import com.huawei.cangjie.ide.run.cjpm.runconfig.buildtool.CjpmBuildManager.isBuildConfiguration
import com.huawei.cangjie.ide.run.cjpm.runconfig.buildtool.CjpmBuildManager.isBuildToolWindowAvailable
import com.huawei.cangjie.ide.run.cjpm.runconfig.toPath
import com.huawei.cangjie.ide.run.hasRemoteTarget
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.runners.showRunContent
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.io.systemIndependentPath
import java.io.File
import java.util.concurrent.CompletableFuture

abstract class CjExecutableRunner(
    protected val executorId: String,
    @NlsContexts.DialogTitle private val errorMessageTitle: String
) : CjDefaultProgramRunnerBase() {
    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        if (executorId != this.executorId || profile !is CjpmCommandConfiguration) return false
        if (profile.clean().ok == null) return false
        return !profile.hasRemoteTarget &&
                profile.isBuildToolWindowAvailable &&
                !isBuildConfiguration(profile) &&
                getBuildConfiguration(profile) != null
    }

    override fun execute(environment: ExecutionEnvironment, state: RunProfileState) {
        val project = environment.project

        if (!checkToolchainConfigured(project)) return
//        val toolchainError = checkToolchainSupported(project, host)
//        if (toolchainError != null) {
//            processInvalidToolchain(project, toolchainError)
//            return
//        }

        environment.putUserData(ARTIFACTS, CompletableFuture())
        super.execute(environment,state)
    }






    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {


        if (state !is CjpmRunStateBase) return null

        val artifacts = environment.artifacts.orEmpty()
        val artifact = artifacts.firstOrNull()
        val binaries = artifact?.executables.orEmpty()

        val runExecutable = GeneralCommandLine().apply {
            exePath = binaries.single().toPath().systemIndependentPath
            workDirectory = state.project.basePath?.let { File(it) }

            val toolchain = state.project.toolchain

            withEnvironment(toolchain?.getEnvironment())
        }
        return showRunContent(state, environment, runExecutable)
    }


    protected open fun showRunContent(
        state: CjpmRunStateBase,
        environment: ExecutionEnvironment,
        runExecutable: GeneralCommandLine
    ): RunContentDescriptor? = showRunContent(executeCommandLine(state, runExecutable, environment), environment)

    private fun executeCommandLine(
        state: CjpmRunStateBase,
        commandLine: GeneralCommandLine,
        environment: ExecutionEnvironment
    ): DefaultExecutionResult = state.executeCommandLine(commandLine, environment)

    open fun checkToolchainSupported(project: Project, host: String): BuildResult.ToolchainError? = null

    open fun checkToolchainConfigured(project: Project): Boolean = true

    open fun processInvalidToolchain(project: Project, toolchainError: BuildResult.ToolchainError) {
        project.showErrorDialog(toolchainError.message)
    }

    private fun Project.showErrorDialog(@Suppress("UnstableApiUsage") @NlsContexts.DialogMessage message: String) {
        Messages.showErrorDialog(this, message, errorMessageTitle)
    }

    companion object {
        private val ARTIFACTS: Key<CompletableFuture<List<CompilerArtifactMessage>>> =
            Key.create("CJPM.CONFIGURATION.ARTIFACTS")

        var ExecutionEnvironment.artifacts: List<CompilerArtifactMessage>?
            get() = getUserData(this@Companion.ARTIFACTS)?.get()
            set(value) {
                getUserData(this@Companion.ARTIFACTS)?.complete(value)
            }
    }
}

sealed class BuildResult {
    data class Binaries(val paths: List<String>) : BuildResult()
    sealed class ToolchainError(@Suppress("UnstableApiUsage") @NlsContexts.DialogMessage val message: String) :
        BuildResult() {
        // TODO: move into bundle
        object UnsupportedMSVC :
            ToolchainError(CangJieBundle.message("dialog.message.msvc.toolchain.not.supported.please.use.gnu.toolchain"))

        object UnsupportedGNU :
            ToolchainError(CangJieBundle.message("dialog.message.gnu.toolchain.not.supported.please.use.msvc.toolchain"))

        object UnsupportedWSL : ToolchainError(CangJieBundle.message("dialog.message.wsl.toolchain.not.supported"))
        object MSVCWithCangJieGNU :
            ToolchainError(CangJieBundle.message("dialog.message.msvc.debugger.cannot.be.used.with.gnu.cangjie.toolchain"))

        object GNUWithCangJieMSVC :
            ToolchainError(CangJieBundle.message("dialog.message.gnu.debugger.cannot.be.used.with.msvc.cangjie.toolchain"))

        object WSLWithNonWSL : ToolchainError(
            CangJieBundle.message("dialog.message.html.local.debugger.cannot.be.used.with.wsl.br.use.href.https.www.jetbrains.com.help.clion.how.to.use.wsl.development.environment.in.product.html.instructions.to.configure.wsl.toolchain.html")
        )

        object NonWSLWithWSL :
            ToolchainError(CangJieBundle.message("dialog.message.wsl.debugger.cannot.be.used.with.non.wsl.cangjie.toolchain"))

        class Other(@NlsContexts.DialogMessage message: String) : ToolchainError(message)
    }
}
