/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.ide.run.cjpm.runconfig.buildtool

import cn.cangnova.cangjie.messages.CangJieBundle
import cn.cangnova.cangjie.cjpm.project.model.CjpmProject
import cn.cangnova.cangjie.cjpm.project.model.impl.workingDirectory
import cn.cangnova.cangjie.ide.run.cjpm.CjExecutableRunner.Companion.artifacts
import cn.cangnova.cangjie.ide.run.cjpm.CompilerArtifactMessage
import cn.cangnova.cangjie.ide.run.cjpm.runconfig.buildtool.CjpmBuildManager.showBuildNotification
import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.UserDataHolderEx
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

abstract class CjpmBuildContextBase(
    open val cjpmProject: CjpmProject,

    @NlsContexts.ProgressText val progressTitle: String,
    val isTestBuild: Boolean,
    val buildId: Any,
    val parentId: Any
) {
    //    val project: Project get() =  CangJieProjectManager.getCurrentProject()
//    val workingDirectory: Path get() = CangJieProjectManager.getCurrentProject().basePath?.let { Path.of(it) } ?: throw ProcessCanceledException()
    val project: Project get() = cjpmProject.project
    val workingDirectory: Path get() = cjpmProject.workingDirectory

    @Volatile
    var indicator: ProgressIndicator? = null

    val errors: AtomicInteger = AtomicInteger()
    val errorCodes: ConcurrentHashMap.KeySetView<String, Boolean> = ConcurrentHashMap.newKeySet()
    val warnings: AtomicInteger = AtomicInteger()

    @Volatile
    var artifacts: List<CompilerArtifactMessage> = emptyList()
}

class CjpmBuildContext(
    cjpmProject: CjpmProject,
    val environment: ExecutionEnvironment,
    @NlsContexts.ProgressTitle val taskName: String,
    @NlsContexts.ProgressText progressTitle: String,
    isTestBuild: Boolean,
    buildId: Any,
    parentId: Any
) : CjpmBuildContextBase(cjpmProject,progressTitle, isTestBuild, buildId, parentId) {
    @Volatile
    var processHandler: ProcessHandler? = null
    val result: CompletableFuture<CjpmBuildResult> = CompletableFuture()

    companion object {
        private val BUILD_SEMAPHORE_KEY: Key<Semaphore> = Key.create("BUILD_SEMAPHORE_KEY")
    }

    private val buildSemaphore: Semaphore = project.getUserData(BUILD_SEMAPHORE_KEY)
        ?: (project as UserDataHolderEx).putUserDataIfAbsent(BUILD_SEMAPHORE_KEY, Semaphore(1))
    val started: Long = System.currentTimeMillis()
    var finished: Long = started

    private val duration: Long get() = finished - started


    fun finished(isSuccess: Boolean) {
        val isCanceled = indicator?.isCanceled ?: false

        environment.artifacts = artifacts.takeIf { isSuccess && !isCanceled }

        finished = System.currentTimeMillis()
        buildSemaphore.release()

        val finishMessage: String
        val finishDetails: String?

        val errors = errors.get()
        val warnings = warnings.get()

        // We report successful builds with errors or warnings correspondingly
        val messageType = if (isCanceled) {
            finishMessage = CangJieBundle.message("system.notification.title.canceled", taskName)
            finishDetails = null
            MessageType.INFO
        } else {
            val hasWarningsOrErrors = errors > 0 || warnings > 0
            finishMessage = if (isSuccess) CangJieBundle.message(
                "system.notification.title.finished",
                taskName
            ) else CangJieBundle.message("system.notification.title.failed", taskName)
            finishDetails = if (hasWarningsOrErrors) {
                val errorsString = if (errors == 1) "error" else "errors"
                val warningsString = if (warnings == 1) "warning" else "warnings"
                CangJieBundle.message("system.notification.text.", errors, errorsString, warnings, warningsString)
            } else {
                null
            }

            when {
                !isSuccess -> MessageType.ERROR
                hasWarningsOrErrors -> MessageType.WARNING
                else -> MessageType.INFO
            }
        }

        result.complete(
            CjpmBuildResult(
                succeeded = isSuccess,
                canceled = isCanceled,
                started = started,
                duration = duration,
                errors = errors,
                warnings = warnings,
                message = finishMessage
            )
        )

        showBuildNotification(project, messageType, finishMessage, finishDetails, duration)
    }

    fun canceled() {
        finished = System.currentTimeMillis()

        result.complete(
            CjpmBuildResult(
                succeeded = false,
                canceled = true,
                started = started,
                duration = duration,
                errors = errors.get(),
                warnings = warnings.get(),
                message = "$taskName canceled"
            )
        )

        environment.notifyProcessNotStarted()
    }

    fun waitAndStart(): Boolean {
        indicator?.pushState()
        try {
            indicator?.text = CangJieBundle.message("progress.text.waiting.for.current.build.to.finish")
            indicator?.text2 = ""
            while (true) {
                indicator?.checkCanceled()
                try {
                    if (buildSemaphore.tryAcquire(100, TimeUnit.MILLISECONDS)) break
                } catch (e: InterruptedException) {
                    throw ProcessCanceledException()
                }
            }
        } catch (e: ProcessCanceledException) {
            canceled()
            return false
        } finally {
            indicator?.popState()
        }
        return true
    }
}

fun ExecutionEnvironment.notifyProcessNotStarted() =
    executionListener.processNotStarted(executor.id, this)

private val ExecutionEnvironment.executionListener: ExecutionListener
    get() = project.messageBus.syncPublisher(ExecutionManager.EXECUTION_TOPIC)

fun ExecutionEnvironment.notifyProcessStartScheduled() =
    executionListener.processStartScheduled(executor.id, this)

fun ExecutionEnvironment.notifyProcessStarting() =
    executionListener.processStarting(executor.id, this)

fun ExecutionEnvironment.notifyProcessStarted(handler: ProcessHandler) =
    executionListener.processStarted(executor.id, this, handler)

fun ExecutionEnvironment.notifyProcessTerminated(handler: ProcessHandler, exitCode: Int) =
    executionListener.processTerminated(executor.id, this, handler, exitCode)

fun ExecutionEnvironment.notifyProcessTerminating(handler: ProcessHandler) =
    executionListener.processTerminating(executor.id, this, handler)
