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
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

abstract class ProcessBuilder {
    var isPty: Boolean = false
    var isColored: Boolean = false
    var isElevated: Boolean = false
    var isShortLived: Boolean = false
    var isSplitToLines: Boolean = false
    var isCapturedOutput: Boolean = true
    var useExternalConsole: Boolean = false
    var setupRunDebugEnv: Boolean = false

    @Nullable
    var progressIndicator: ProgressIndicator? = null
    var isEmulateTerminal: Boolean = false

    @NotNull
    fun withPty(value: Boolean): ProcessBuilder {
        this.isPty = value
        return this
    }

    @NotNull
    fun withColoredOutput(value: Boolean): ProcessBuilder {
        isColored = value
        return this
    }

    @NotNull
    fun withElevated(value: Boolean): ProcessBuilder {
        isElevated = value
        return this
    }

    @NotNull
    fun withShortLived(value: Boolean): ProcessBuilder {
        isShortLived = value
        return this
    }

    @NotNull
    fun withSplitToLines(value: Boolean): ProcessBuilder {
        isSplitToLines = value
        return this
    }

    @NotNull
    fun withCapturedOutput(value: Boolean): ProcessBuilder {
        isCapturedOutput = value
        return this
    }

    @NotNull
    fun withExternalConsole(value: Boolean): ProcessBuilder {
        useExternalConsole = value
        return this
    }

    @NotNull
    fun withRunDebugEnvSetup(value: Boolean): ProcessBuilder {
        setupRunDebugEnv = value
        return this
    }

    @NotNull
    fun withProgressIndicator(pi: ProgressIndicator?): ProcessBuilder {
        progressIndicator = pi
        return this
    }

    @NotNull
    fun withEmulateTerminal(value: Boolean): ProcessBuilder {
        isEmulateTerminal = value
        return this
    }

    @NotNull
    @Throws(ExecutionException::class)
    fun build(cmd: GeneralCommandLine): BaseProcessHandler<*> {
        return this.build(
            cmd, Parameters(
                isPty,
                isColored,
                isElevated,
                isShortLived,
                isSplitToLines,
                isCapturedOutput,
                useExternalConsole,
                setupRunDebugEnv,
                progressIndicator,
                isEmulateTerminal
            )
        )

    }

    protected abstract fun build(cmd: GeneralCommandLine, parameters: Parameters): BaseProcessHandler<*>

    class Parameters(
        val isPty: Boolean,
        val isColored: Boolean,
        val isElevated: Boolean,
        val isShortLived: Boolean,
        val isSplitToLines: Boolean,
        val isCapturedOutput: Boolean,
        val useExternalConsole: Boolean,
        val setupRunDebugEnv: Boolean,
        val progressIndicator: ProgressIndicator?,
        val isEmulateTerminal: Boolean
    )
}
