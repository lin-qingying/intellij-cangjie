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

package org.cangnova.cangjie.dapDebugger

import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.fileTypes.FileType
import com.redhat.devtools.lsp4ij.dap.definitions.DebugAdapterServerDefinition
import com.redhat.devtools.lsp4ij.dap.descriptors.DebugAdapterDescriptor
import com.redhat.devtools.lsp4ij.dap.descriptors.DebugAdapterDescriptorFactory
import org.cangnova.cangjie.lang.CangJieFileType

class CangJieDebugAdapterDescriptorFactory : DebugAdapterDescriptorFactory() {

    override fun createDebugAdapterDescriptor(
        options: RunConfigurationOptions,
        environment: ExecutionEnvironment
    ): DebugAdapterDescriptor {

        return CangJieDebugAdapterDescriptor(options, environment, getServerDefinition());
    }
}

class CangJieDebugAdapterDescriptor(
    options: RunConfigurationOptions,
    environment: ExecutionEnvironment,
    serverDefinition: DebugAdapterServerDefinition
) : DebugAdapterDescriptor(
    options, environment, serverDefinition
) {
    override fun startServer(): ProcessHandler {
        val runParameters = RunParameters(

            CangJieDebuggerServerManager.getCommandLine(environment.project),
            CangJieDebuggerServerManager.DEBUGPORT,
//            runExecutable.commandLineString
            ""

        )
        return DapProcessHandler(runParameters.command).apply {
            startNotify()
        }

    }

    override fun getDapParameters(): Map<String?, Any?> {
        return emptyMap()
    }

    override fun getFileType(): FileType {
        return CangJieFileType.INSTANCE
    }
}