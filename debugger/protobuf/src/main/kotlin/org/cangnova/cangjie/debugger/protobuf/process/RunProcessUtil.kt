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

package org.cangnova.cangjie.debugger.protobuf.process


import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.util.containers.ContainerUtil


object RunProcessUtil {
    const val READ_VERSION_TIMEOUT = 10000

    @JvmStatic
    fun runProcess(handler: BaseProcessHandler<*>, indicator: ProgressIndicator?, timeout: Int): ProcessOutput {
        return runProcess(handler as ProcessHandler, indicator, timeout)
    }

    @JvmStatic
    fun runProcess(handler: ProcessHandler, indicator: ProgressIndicator?, timeout: Int): ProcessOutput {
        val processOutput = if (indicator == null) {
            CapturingProcessRunner(handler).runProcess(timeout)
        } else {
            CapturingProcessRunner(handler).runProcess(indicator, timeout)
        }
        return processOutput
    }

    @JvmStatic
    fun getProgressIndicator(): ProgressIndicator? {
        return ProgressIndicatorProvider.getGlobalProgressIndicator()
    }

    @JvmStatic
    fun runWithProgress(handler: CapturingProcessHandler): ProcessOutput {
        return runWithProgress(handler, 0)
    }

    @JvmStatic
    fun runWithProgress(handler: BaseProcessHandler<*>, timeout: Int): ProcessOutput {
        return runWithProgress(handler as ProcessHandler, timeout)
    }

    @JvmStatic
    fun runWithProgress(handler: ProcessHandler, timeout: Int): ProcessOutput {
        val indicator = getProgressIndicator()
        val result = runProcess(handler, indicator, timeout)
        if (result.isCancelled) {
            throw ProcessCanceledException()
        }
        return result
    }

    @JvmStatic
    fun readOneLineWithProgress(commandLine: GeneralCommandLine, timeout: Int): String? {
        return try {
            val output = runWithProgress(CapturingProcessHandler(commandLine), timeout)
            ContainerUtil.getFirstItem(output.stdout.lines())
        } catch (e: ExecutionException) {
            null
        }
    }

    @JvmStatic
    fun setHasPty(processHandler: OSProcessHandler, usePty: Boolean) {
        if (usePty) {
            processHandler.setHasPty(true)
            processHandler.setShouldDestroyProcessRecursively(false)
        }
    }
}
