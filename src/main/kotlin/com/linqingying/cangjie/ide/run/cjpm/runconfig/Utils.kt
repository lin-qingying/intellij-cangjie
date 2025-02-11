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

@file:Suppress("UnstableApiUsage")
package com.linqingying.cangjie.ide.run.cjpm.runconfig

import com.linqingying.cangjie.CangJieBundle

import com.linqingying.cangjie.ide.run.cjpm.isUnitTestMode
import com.linqingying.cangjie.ide.run.cjpm.languageRuntime
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.target.*
import com.intellij.execution.target.value.TargetValue
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.text.nullize
private val LOG: Logger = Logger.getInstance("com.linqingying.cangjie.ide.run.cjpm.runconfig.Utils")





/**
 * 在一个带有取消进度的项目中计算给定的任务
 *
 * 此函数用于在可能需要长时间运行的任务中显示一个带有取消功能的进度条
 * 它根据环境是否是单元测试模式来决定任务的执行方式
 *
 * @param title 进度条的标题，用于向用户显示当前任务的简短描述
 * @param supplier 一个提供任务结果的lambda表达式，它会在一个带有进度条的环境中执行
 * @return 任务的结果，类型由调用者指定
 *
 * 注意：此函数使用了`ProgressManager`来管理进度条的显示和取消逻辑，这是JetBrains IDEA和其他IntelliJ平台产品中的一个组件
 * 在单元测试模式下，进度条将被跳过，任务将直接执行，以避免在测试环境中显示UI元素
 */
fun <T> Project.computeWithCancelableProgress(
    @Suppress("UnstableApiUsage") @NlsContexts.ProgressTitle title: String,
    supplier: () -> T
): T {
    // 如果是单元测试模式，直接执行任务并返回结果，不显示进度条
    if (isUnitTestMode) {
        return supplier()
    }
    // 在非单元测试模式下，使用ProgressManager运行任务，并显示带有取消功能的进度条
    return ProgressManager.getInstance().runProcessWithProgressSynchronously<T, Exception>(supplier, title, true, this)
}


fun GeneralCommandLine.startProcess(
    project: Project,
    config: TargetEnvironmentConfiguration?,
    processColors: Boolean,
    uploadExecutable: Boolean
): ProcessHandler {
    if (config == null) {
        val handler = CjProcessHandler(this)
        ProcessTerminatedListener.attach(handler)
        return handler
    }

    val request = config.createEnvironmentRequest(project)
    val setup = CjCommandLineSetup(request)
    val targetCommandLine = toTargeted(setup, uploadExecutable)
    val progressIndicator = ProgressManager.getInstance().progressIndicator ?: EmptyProgressIndicator()
    val environment =
        project.computeWithCancelableProgress(CangJieBundle.message("progress.title.preparing.remote.environment")) {
            request.prepareEnvironment(setup, progressIndicator)
        }
    val process = environment.createProcess(targetCommandLine, progressIndicator)

    val commandRepresentation = targetCommandLine.getCommandPresentation(environment)
   LOG.debug("Executing command: `$commandRepresentation`")

    val handler = CjProcessHandler(process, commandRepresentation, targetCommandLine.charset, processColors)
    ProcessTerminatedListener.attach(handler)
    return handler
}


private fun GeneralCommandLine.toTargeted(
    setup: CjCommandLineSetup,
    uploadExecutable: Boolean
): TargetedCommandLine {
    val commandLineBuilder = TargetedCommandLineBuilder(setup.request)
    commandLineBuilder.charset = charset

    val targetedExePath = if (uploadExecutable) setup.requestUploadIntoTarget(exePath) else TargetValue.fixed(exePath)
    commandLineBuilder.exePath = targetedExePath

    val workDirectory = workDirectory
    if (workDirectory != null) {
        val targetWorkingDirectory = setup.requestUploadIntoTarget(workDirectory.absolutePath)
        commandLineBuilder.setWorkingDirectory(targetWorkingDirectory)
    }

    val inputFile = inputFile
    if (inputFile != null) {
        val targetInput = setup.requestUploadIntoTarget(inputFile.absolutePath)
        commandLineBuilder.setInputFile(targetInput)
    }

    commandLineBuilder.addParameters(parametersList.parameters)

    for ((key, value) in environment.entries) {
        commandLineBuilder.addEnvironmentVariable(key, value)
    }
//    val runtime = setup.request.configuration?.languageRuntime
//    commandLineBuilder.addEnvironmentVariable("CJC", runtime?.cjcPath?.nullize(true))
//    commandLineBuilder.addEnvironmentVariable("CJPM", runtime?.cjpmPath?.nullize(true))


    return commandLineBuilder.build()
}

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
            CangJieBundle.message(
                "dialog.message.failed.to.prepare.remote.environment",
                e.localizedMessage
            ), e
        )
    }
}
