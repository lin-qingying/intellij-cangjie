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

package cn.cangnova.cangjie.serialization.builtins

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import cn.cangnova.cangjie.resolve.CommonCompilerPerformanceManager
import cn.cangnova.cangjie.cli.messages.*
import cn.cangnova.cangjie.config.*
import java.io.File

object BuiltInsSerializer {
    fun analyzeAndSerialize(
        destDir: File,
        srcDirs: List<File>,
        extraClassPath: List<File>,
        dependOnOldBuiltIns: Boolean,
        project:Project? = null,
        onComplete: (totalSize: Int, totalFiles: Int) -> Unit
    ) {
        val rootDisposable =
            Disposer.newDisposable("Disposable for ${CangJieBuiltInsSerializer::class.simpleName}.analyzeAndSerialize")
        val messageCollector = createMessageCollector()
        val performanceManager = object : CommonCompilerPerformanceManager(presentableName = "test") {}

        try {
            val configuration = CompilerConfiguration().apply {
                this.messageCollector = messageCollector

                addCangJieSourceRoots(srcDirs.map { it.path })


                put(CLIConfigurationKeys.METADATA_DESTINATION_DIRECTORY, destDir)
                put(CommonConfigurationKeys.MODULE_NAME, "module for built-ins serialization")
                put(CLIConfigurationKeys.PERF_MANAGER, performanceManager)
                put(CommonConfigurationKeys.USE_LIGHT_TREE, true)
                put(
                    CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS,
                    LanguageVersionSettingsImpl(
                        LanguageVersion.LATEST_STABLE,
                        ApiVersion.LATEST_STABLE,
                        analysisFlags = mapOf(
//                            AnalysisFlags.stdlibCompilation to !dependOnOldBuiltIns,
//                            AnalysisFlags.allowKotlinPackage to true
                        )
                    )
                )
            }
            val environment = CangJieCoreEnvironment. createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.NATIVE_CONFIG_FILES,project)

            val serializer = CangJieBuiltInsSerializer(
                configuration,
                environment,
                dependOnOldBuiltIns
            )

            val (totalSize, totalFiles) = serializer.analyzeAndSerialize() ?: AbstractMetadataSerializer.OutputInfo(
                totalSize = 0,
                totalFiles = 0
            )

            onComplete(totalSize, totalFiles)
        } finally {
            messageCollector.flush()
            Disposer.dispose(rootDisposable)
        }
    }

    private fun createMessageCollector() = object : GroupingMessageCollector(
        PrintingMessageCollector(System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, false),
        /* treatWarningsAsErrors = */ false,
        /* reportAllWarnings = */ false,
    ) {
        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?
        ) {
            // Only report diagnostics without a particular location because there's plenty of errors in built-in sources
            // (functions without bodies, incorrect combination of modifiers, etc.)
            if (location == null) {
                super.report(severity, message, location)
            }
        }
    }
}
