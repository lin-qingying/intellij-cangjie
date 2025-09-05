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

package org.cangnova.cangjie.toolchain.tools

import org.cangnova.cangjie.toolchain.CjToolchainBase
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.util.io.systemIndependentPath
import java.nio.file.Path


abstract class CjTool(toolName: String, val toolchain: org.cangnova.cangjie.toolchain.CjToolchainBase) {
    open val executable: Path = toolchain.pathToExecutable(toolName)




    protected fun createBaseCommandLine(
        vararg parameters: String,
        workingDirectory: Path? = null,
        environment: Map<String, String> = emptyMap()
    ): GeneralCommandLine = createBaseCommandLine(
        parameters.toList(),
        workingDirectory = workingDirectory,
        environment = environment
    )

    protected open fun createBaseCommandLine(
        parameters: List<String>,
        workingDirectory: Path? = null,
        environment: Map<String, String> = emptyMap()
    ): GeneralCommandLine = GeneralCommandLine(executable)
        .withWorkDirectory(workingDirectory)
        .withParameters(parameters)
        .withEnvironment(environment)
        .withCharset(Charsets.UTF_8)
        .also { toolchain.patchCommandLine(it) }
}

@Suppress("FunctionName", "UnstableApiUsage")
fun GeneralCommandLine(path: Path, withSudo: Boolean = false, vararg args: String) =
    object : GeneralCommandLine(path.systemIndependentPath, *args) {
//        override fun createProcess(): Process = if (withSudo) {
//            ElevationService.getInstance().createProcess(this)
//        } else {
//            super.createProcess()
//        }
    }

fun GeneralCommandLine.withWorkDirectory(path: Path?) = withWorkDirectory(path?.systemIndependentPath)

abstract class CangJieComponent(componentName: String, toolchain: org.cangnova.cangjie.toolchain.CjToolchainBase) :
    CjTool(componentName, toolchain) {

    val executionPath: String = executable.systemIndependentPath



}
