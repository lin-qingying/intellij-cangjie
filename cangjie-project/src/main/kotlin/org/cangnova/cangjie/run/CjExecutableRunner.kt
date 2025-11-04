/*
 * Copyright 2025 LinQingYing. and contributors.
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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.run

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.runners.showRunContent

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.UserDataHolderBase
import org.cangnova.cangjie.process.CjProcessHandler
import org.cangnova.cangjie.project.CjProjectBundle
import org.cangnova.cangjie.run.CompilerArtifact.Companion.REASON

import org.cangnova.cangjie.run.target.startProcess
import java.io.File
import java.util.concurrent.CompletableFuture

/**
 * Base class for executable runners that can run compiled CangJie programs.
 * Handles toolchain validation and artifact execution.
 */
abstract class CjExecutableRunner(
    protected val executorId: String,
    @field:NlsContexts.DialogTitle private val errorMessageTitle: String
) : CjDefaultProgramRunnerBase() {

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        if (executorId != this.executorId) return false
        if (profile !is CangJieRunConfigurationBase) return false

        // Check if configuration is valid
        try {
            profile.checkConfiguration()
        } catch (e: Exception) {
            return false
        }

        return true
    }

    override fun execute(environment: ExecutionEnvironment, state: RunProfileState) {
        val project = environment.project

        if (!checkToolchainConfigured(project)) return

        // Don't initialize artifacts future here
        // It will be initialized by attachBuildAdapter if needed

        super.execute(environment, state)
    }

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        if (state !is CangJieCommandRunState) return null

        val artifacts = environment.artifacts.orEmpty()
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

        return showRunContent(state, environment, runExecutable)
    }

    protected open fun showRunContent(
        state: CangJieCommandRunState,
        environment: ExecutionEnvironment,
        runExecutable: GeneralCommandLine
    ): RunContentDescriptor? {
        val executionResult = state.executeCommandLine(runExecutable, environment)
        return showRunContent(executionResult, environment)
    }

    /**
     * Check if the toolchain is supported for the given host
     */
    open fun checkToolchainSupported(project: Project, host: String): BuildResult.ToolchainError? = null

    /**
     * Check if the toolchain is configured
     */
    open fun checkToolchainConfigured(project: Project): Boolean = true

    /**
     * Process invalid toolchain error
     */
    open fun processInvalidToolchain(project: Project, toolchainError: BuildResult.ToolchainError) {
        project.showErrorDialog(toolchainError.message)
    }

    private fun Project.showErrorDialog(@NlsContexts.DialogMessage message: String) {
        Messages.showErrorDialog(this, message, errorMessageTitle)
    }

    companion object {
        private val ARTIFACTS: Key<CompletableFuture<List<CompilerArtifact>>> =
            Key.create("CANGJIE.CONFIGURATION.ARTIFACTS")

        /**
         * Initialize artifacts future for this environment.
         * This should be called when we expect artifacts to be produced.
         */
        fun ExecutionEnvironment.initializeArtifactsFuture() {
            putUserData(ARTIFACTS, CompletableFuture())
        }

        var ExecutionEnvironment.artifacts: List<CompilerArtifact>?
            get() {
                val future = getUserData(ARTIFACTS) ?: return null
                // If future exists, it means we expect artifacts
                // Block and wait for them to be ready
                return try {
                    future.get()
                } catch (e: Exception) {
                    null
                }
            }
            set(value) {
                getUserData(ARTIFACTS)?.complete(value)
            }
    }
}


typealias PathConverter = (String) -> String
sealed class CompilerMessage {

    abstract fun convertPaths(converter: PathConverter): CompilerMessage

    companion object {
        fun fromJson(json: JsonObject): CompilerMessage? {
            val reason = json.getAsJsonPrimitive("reason")?.asString ?: return null
            val cls: Class<out CompilerMessage> = when (reason) {
//                BuildScriptMessage.REASON -> BuildScriptMessage::class.java
               REASON ->  CompilerArtifact::class.java
                else -> return null
            }
            return Gson().fromJson(json, cls)
        }
    }
}

data class CompilerArtifact(
    val name: String,
    val executables: List<String> = emptyList(),
    val profile: BuildProfile = BuildProfile.DEBUG,
    val files: List<ArtifactFile> = emptyList()
) : CompilerMessage() {

    override fun convertPaths(converter: PathConverter): CompilerArtifact = copy(
        executables = executables.map(converter),
        files = files.map { it.copy(path = converter(it.path)) }
    )

    companion object {
        const val REASON: String = "compiler-artifact"

        fun fromJson(json: JsonObject): CompilerArtifact? {
            if (json.getAsJsonPrimitive("reason").asString != REASON) {
                return null
            }
            return Gson().fromJson(json, CompilerArtifact::class.java)
        }
    }
}

/**
 * Build profile (debug or release)
 */
enum class BuildProfile {
    DEBUG,
    RELEASE
}

/**
 * Artifact file with type information
 */
data class ArtifactFile(
    val path: String,
    val type: ArtifactFileType
)

/**
 * Types of artifact files
 */
enum class ArtifactFileType {
    /** Executable binary file */
    EXECUTABLE,
    /** Compiled object file (.cjo) */
    OBJECT,
    /** Library file */
    LIBRARY,
    /** Debug symbols */
    DEBUG_SYMBOLS,
    /** Other files */
    OTHER
}

/**
 * Build result types
 */
sealed class BuildResult {
    data class Binaries(val paths: List<String>) : BuildResult()

    sealed class ToolchainError(@NlsContexts.DialogMessage val message: String) : BuildResult() {
        object UnsupportedMSVC : ToolchainError(
            CjProjectBundle.message("run.configuration.error.msvc.toolchain.not.supported")
        )

        object UnsupportedGNU : ToolchainError(
            CjProjectBundle.message("run.configuration.error.gnu.toolchain.not.supported")
        )

        object UnsupportedWSL : ToolchainError(
            CjProjectBundle.message("run.configuration.error.wsl.toolchain.not.supported")
        )

        object MSVCWithCangJieGNU : ToolchainError(
            CjProjectBundle.message("run.configuration.error.msvc.debugger.with.gnu.toolchain")
        )

        object GNUWithCangJieMSVC : ToolchainError(
            CjProjectBundle.message("run.configuration.error.gnu.debugger.with.msvc.toolchain")
        )

        object WSLWithNonWSL : ToolchainError(
            CjProjectBundle.message("run.configuration.error.wsl.debugger.with.non.wsl.toolchain")
        )

        object NonWSLWithWSL : ToolchainError(
            CjProjectBundle.message("run.configuration.error.non.wsl.debugger.with.wsl.toolchain")
        )

        class Other(@NlsContexts.DialogMessage message: String) : ToolchainError(message)
    }
}
class ConfigurationExtensionContext : UserDataHolderBase()

/**
 * Extension function to execute a command line and return execution result
 */

fun CangJieCommandRunState.executeCommandLine(
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
    console.attachToProcess(handler)
    return DefaultExecutionResult(console, handler)

}