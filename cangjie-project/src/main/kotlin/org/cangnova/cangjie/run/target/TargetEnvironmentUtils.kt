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

@file:Suppress("UnstableApiUsage")

package org.cangnova.cangjie.run.target

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.target.*
import com.intellij.execution.target.value.TargetValue
import com.intellij.execution.target.value.getUploadRootForLocalPath
import com.intellij.lang.LangCoreBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.cangnova.cangjie.process.CjProcessHandler
import org.cangnova.cangjie.project.CjProjectBundle
import java.nio.file.Path

private val LOG: Logger = logger<TargetEnvironmentConfiguration>()

/**
 * Check if running in unit test mode
 */
private val isUnitTestMode: Boolean
    get() = ApplicationManager.getApplication().isUnitTestMode

/**
 * Execute a task with a cancelable progress indicator
 *
 * @param title Progress dialog title
 * @param supplier Task to execute
 * @return Task result
 */
fun <T> Project.computeWithCancelableProgress(
    title: String,
    supplier: () -> T
): T {
    if (isUnitTestMode) {
        return supplier()
    }
    return ProgressManager.getInstance().runProcessWithProgressSynchronously<T, Exception>(
        supplier,
        title,
        true,
        this
    )
}

/**
 * Start a process with support for remote target environments
 *
 * @param project Current project
 * @param config Target environment configuration (null for local execution)
 * @param processColors Whether to enable colored output
 * @param uploadExecutable Whether to upload the executable to the target
 * @return Process handler
 */
fun GeneralCommandLine.startProcess(
    project: Project,
    config: TargetEnvironmentConfiguration?,
    processColors: Boolean,
    uploadExecutable: Boolean
): ProcessHandler {
    // Local execution
    if (config == null) {
        val handler = CjProcessHandler(this, processColors = processColors)
        ProcessTerminatedListener.attach(handler)
        return handler
    }

    // Remote execution
    val request = config.createEnvironmentRequest(project)
    val setup = CjCommandLineSetup(request)
    val targetCommandLine = toTargeted(setup, uploadExecutable)
    val progressIndicator = ProgressManager.getInstance().progressIndicator ?: EmptyProgressIndicator()

    val environment = project.computeWithCancelableProgress(
        CjProjectBundle.message("run.configuration.preparing.remote.environment")
    ) {
        request.prepareEnvironment(setup, progressIndicator)
    }

    val process = environment.createProcess(targetCommandLine, progressIndicator)
    val commandRepresentation = targetCommandLine.getCommandPresentation(environment)

    LOG.debug("Executing command: `$commandRepresentation`")

    val handler = CjProcessHandler(process, commandRepresentation, targetCommandLine.charset, processColors)
    ProcessTerminatedListener.attach(handler)
    return handler
}

/**
 * Convert a GeneralCommandLine to a TargetedCommandLine
 */
private fun GeneralCommandLine.toTargeted(
    setup: CjCommandLineSetup,
    uploadExecutable: Boolean
): TargetedCommandLine {
    val commandLineBuilder = TargetedCommandLineBuilder(setup.request)
    commandLineBuilder.charset = charset

    // Set executable path
    val targetedExePath = if (uploadExecutable) {
        setup.requestUploadIntoTarget(exePath)
    } else {
        TargetValue.fixed(exePath)
    }
    commandLineBuilder.exePath = targetedExePath

    // Set working directory
    val workDirectory = workDirectory
    if (workDirectory != null) {
        val targetWorkingDirectory = setup.requestUploadIntoTarget(workDirectory.absolutePath)
        commandLineBuilder.setWorkingDirectory(targetWorkingDirectory)
    }

    // Set input file
    val inputFile = inputFile
    if (inputFile != null) {
        val targetInput = setup.requestUploadIntoTarget(inputFile.absolutePath)
        commandLineBuilder.setInputFile(targetInput)
    }

    // Add parameters
    commandLineBuilder.addParameters(parametersList.parameters)

    // Add environment variables
    for ((key, value) in environment.entries) {
        commandLineBuilder.addEnvironmentVariable(key, value)
    }

    return commandLineBuilder.build()
}

/**
 * Prepare the target environment
 */
private fun TargetEnvironmentRequest.prepareEnvironment(
    setup: CjCommandLineSetup,
    progressIndicator: ProgressIndicator
): TargetEnvironment {
    val targetProgressIndicator = object : TargetProgressIndicator {
        override fun isCanceled(): Boolean = progressIndicator.isCanceled
        override fun stop() = progressIndicator.cancel()
        override fun isStopped(): Boolean = isCanceled
        override fun addText(text: String, key: Key<*>) {
            progressIndicator.text2 = text.trim()
        }
    }

    return try {
        val environment = prepareEnvironment(targetProgressIndicator)
        setup.provideEnvironment(environment, targetProgressIndicator)
        environment
    } catch (e: ProcessCanceledException) {
        throw e
    } catch (e: Exception) {
        throw ExecutionException(
            CjProjectBundle.message(
                "run.configuration.error.failed.to.prepare.remote.environment",
                e.localizedMessage
            ),
            e
        )
    }
}

/**
 * Helper class for setting up command line execution in target environments
 */
class CjCommandLineSetup(val request: TargetEnvironmentRequest) {
    private val environmentPromise =
        org.jetbrains.concurrency.AsyncPromise<Pair<TargetEnvironment, TargetProgressIndicator>>()
    private val dependingOnEnvironmentPromise: MutableList<org.jetbrains.concurrency.Promise<Unit>> = mutableListOf()
    private val uploads: MutableList<Upload> = mutableListOf()
    private val projectHomeOnTarget: LanguageRuntimeType.VolumeDescriptor = LanguageRuntimeType.VolumeDescriptor(
        CjCommandLineSetup::class.java.simpleName + ":projectHomeOnTarget",
        "",
        "",
        "",
        request.projectPathOnTarget
    )

    private val languageRuntime: CjLanguageRuntimeConfiguration? = request.configuration?.languageRuntime

    private fun createUploadRoot(
        volumeDescriptor: LanguageRuntimeType.VolumeDescriptor,
        localRootPath: Path
    ): TargetEnvironment.UploadRoot =
        languageRuntime?.createUploadRoot(volumeDescriptor, localRootPath)
            ?: TargetEnvironment.UploadRoot(localRootPath, TargetEnvironment.TargetPath.Temporary())

    private fun joinPath(vararg segments: String): String =
        segments.joinToString(request.targetPlatform.platform.fileSeparator.toString())

    /**
     * Request to upload a local path to the target environment
     */
    fun requestUploadIntoTarget(uploadPathString: String): TargetValue<String> {
        val uploadPath = java.io.File(uploadPathString).toPath()
        val isDir = uploadPath.toFile().isDirectory
        val localRootPath = if (isDir) uploadPath else (uploadPath.parent ?: java.nio.file.Paths.get("."))

        val (uploadRoot, pathToRoot) = request.getUploadRootForLocalPath(localRootPath)
            ?: createUploadRoot(projectHomeOnTarget, localRootPath).let { uploadRoot ->
                request.uploadVolumes += uploadRoot
                uploadRoot to "."
            }
        val result = com.intellij.execution.target.value.DeferredTargetValue(uploadPathString)
        dependingOnEnvironmentPromise += environmentPromise.then { (environment, targetProgressIndicator) ->
            if (targetProgressIndicator.isCanceled || targetProgressIndicator.isStopped) {
                result.stopProceeding()
                return@then
            }
            val volume = environment.uploadVolumes.getValue(uploadRoot)
            try {
                val relativePath = if (isDir) {
                    pathToRoot
                } else {
                    uploadPath.fileName.toString()
                        .let { if (pathToRoot == ".") it else joinPath(pathToRoot, it) }
                }
                val resolvedTargetPath = volume.resolveTargetPath(relativePath)
                uploads.add(Upload(volume, relativePath))
                result.resolve(resolvedTargetPath)
            } catch (t: Throwable) {
                LOG.warn(t)
                targetProgressIndicator.stopWithErrorMessage(
                    LangCoreBundle.message(
                        "progress.message.failed.to.resolve.0.1",
                        volume.localRoot, t.localizedMessage
                    )
                )
                result.resolveFailure(t)
            }
        }
        return result
    }

    /**
     * Provide the prepared environment to the setup
     */
    fun provideEnvironment(environment: TargetEnvironment, targetProgressIndicator: TargetProgressIndicator) {
        environmentPromise.setResult(environment to targetProgressIndicator)

        // Upload files
        uploads.asSequence()
            .sortedBy { it.relativePath.length }
            .groupBy({ it.volume }, { it.relativePath })
            .forEach { (volume, relativePaths) ->
                volume.upload(relativePaths.first(), targetProgressIndicator)
            }

        // Wait for all dependent promises
        for (promise in dependingOnEnvironmentPromise) {
            try {
                promise.blockingGet(0) // Just rethrows errors
            } catch (e: Exception) {
                LOG.warn("Failed to complete upload promise", e)
            }
        }
    }

    private class Upload(val volume: TargetEnvironment.UploadableVolume, val relativePath: String)
}

