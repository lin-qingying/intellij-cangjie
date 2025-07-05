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

package cn.cangnova.cangjie.toolchain.tools


import cn.cangnova.cangjie.ide.module.CjProcessResult
import cn.cangnova.cangjie.ide.run.cjpm.runconfig.CjCapturingProcessHandler
import cn.cangnova.cangjie.ide.run.cjpm.runconfig.CjProcessExecutionException
import cn.cangnova.cangjie.ide.run.cjpm.runconfig.unwrapOrElse
import cn.cangnova.cangjie.toolchain.impl.CangJieVersion.Companion.getInfo
import cn.cangnova.cangjie.utils.isUnitTestMode
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import java.nio.file.Path


class Cjc(toolchain: cn.cangnova.cangjie.toolchain.CjToolchainBase) : CangJieComponent(NAME, toolchain) {

    val version: cn.cangnova.cangjie.toolchain.impl.CangJieVersion?
        get() = getInfo()

    init {
        toolchain.cjc = this
    }

    fun queryVersion(workingDirectory: Path? = null): cn.cangnova.cangjie.toolchain.impl.CangJieVersion? {
        try {
            if (!isUnitTestMode) {
                checkIsBackgroundThread()
            }
            val lines = createBaseCommandLine(
                "-v",
                workingDirectory = workingDirectory
            ).execute(toolchain.executionTimeoutInMilliseconds)?.stdoutLines

            return lines?.let { cn.cangnova.cangjie.toolchain.impl.parseCjcVersion(it) }
        } catch (e: CjProcessExecutionException) {

            return null
        }
    }

    fun queryVersion(
        workingDirectory: Path, owner: Disposable, listener: ProcessListener
    ): CjProcessResult<cn.cangnova.cangjie.toolchain.impl.CangJieVersion?> {
        if (!isUnitTestMode) {
            checkIsBackgroundThread()
        }
        return createBaseCommandLine("-v", workingDirectory = workingDirectory).execute(owner, listener = listener)
            .map {
                cn.cangnova.cangjie.toolchain.impl.parseCjcVersion(it.stdoutLines)

//            version
            }
    }


    fun getSysroot(projectDirectory: Path): String? {
        if (!isUnitTestMode) {
            checkIsBackgroundThread()
        }
        val timeoutMs = 10000
        val output = createBaseCommandLine(
            "--print", "sysroot", workingDirectory = projectDirectory
        ).execute(timeoutMs)

        if (output?.isSuccess != true) return null
        return toolchain.toLocalPath(output.stdout.trim())
    }


    companion object {
        const val NAME: String = "bin/cjc"
    }
}

fun checkIsBackgroundThread() {
    check(!ApplicationManager.getApplication().isDispatchThread) {
        "Long running operation invoked on UI thread"
    }
}


private val LOG: Logger = Logger.getInstance("CommandLineExt")

fun CapturingProcessHandler.runProcessWithGlobalProgress(timeoutInMilliseconds: Int? = null): ProcessOutput {
    return runProcess(ProgressManager.getGlobalProgressIndicator(), timeoutInMilliseconds)

}

fun CapturingProcessHandler.runProcess(
    indicator: ProgressIndicator?, timeoutInMilliseconds: Int? = null
): ProcessOutput {
    return when {
        indicator != null && timeoutInMilliseconds != null -> runProcessWithProgressIndicator(
            indicator,
            timeoutInMilliseconds
        )

        indicator != null -> runProcessWithProgressIndicator(indicator)
        timeoutInMilliseconds != null -> runProcess(timeoutInMilliseconds)
        else -> runProcess()
    }

}

val ProcessOutput.isSuccess: Boolean get() = !isTimeout && !isCancelled && exitCode == 0

fun GeneralCommandLine.execute(timeoutInMilliseconds: Int?): ProcessOutput? {
    LOG.info("Executing `$commandLineString`")
    val handler = CjCapturingProcessHandler.startProcess(this).unwrapOrElse {
        LOG.warn("Failed to run executable", it)
        return null
    }
    val output = handler.runProcessWithGlobalProgress(timeoutInMilliseconds)

    if (!output.isSuccess) {
        LOG.warn(CjProcessExecutionException.errorMessage(commandLineString, output))
    }

    return output
}
