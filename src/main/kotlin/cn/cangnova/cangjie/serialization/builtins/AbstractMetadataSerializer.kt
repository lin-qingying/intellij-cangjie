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

import cn.cangnova.cangjie.cli.messages.CompilerMessageSeverity
import cn.cangnova.cangjie.config.*
import cn.cangnova.cangjie.config.destDir
import cn.cangnova.cangjie.metadata.builtins.BuiltInsBinaryVersion
import java.io.File


abstract class AbstractMetadataSerializer<T>(
    val configuration: CompilerConfiguration,
    val environment: CangJieCoreEnvironment,
    definedMetadataVersion: BuiltInsBinaryVersion? = null

) {
    protected val metadataVersion =
        definedMetadataVersion ?: configuration.get(CommonConfigurationKeys.METADATA_VERSION) as? BuiltInsBinaryVersion
        ?: BuiltInsBinaryVersion.INSTANCE

    fun analyzeAndSerialize(): OutputInfo? {
        val destDir = environment.destDir
        if (destDir == null) {
            val configuration = environment.configuration
            val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            messageCollector.report(CompilerMessageSeverity.ERROR, "Specify destination via -d")
            return null
        }

        val analysisResult = analyze() ?: return null
//        val performanceManager = environment.configuration.getNotNull(CLIConfigurationKeys.PERF_MANAGER)


        return serialize(analysisResult, destDir)
    }

    protected abstract fun analyze(): T?

    /**
     * @return number of written bytes and files
     * The return value is optional and might be omitted in implementations
     */
    protected abstract fun serialize(analysisResult: T, destDir: File): OutputInfo?

    data class OutputInfo(val totalSize: Int, val totalFiles: Int)
}
