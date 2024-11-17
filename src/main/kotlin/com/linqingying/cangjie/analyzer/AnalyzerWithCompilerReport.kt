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

package com.linqingying.cangjie.analyzer


import com.linqingying.cangjie.cli.messages.MessageCollector
import com.linqingying.cangjie.config.*
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.resolve.CompilerEnvironment
import com.linqingying.cangjie.resolve.TargetEnvironment
import com.linqingying.cangjie.serialization.builtins.languageVersionSettings


interface AbstractAnalyzerWithCompilerReport {
    val targetEnvironment: TargetEnvironment

    val analysisResult: AnalysisResult

    fun analyzeAndReport(files: Collection<CjFile>, analyze: () -> AnalysisResult)

    fun hasErrors(): Boolean
}
class AnalyzerWithCompilerReport(
    private val messageCollector: MessageCollector,
    private val languageVersionSettings: LanguageVersionSettings,
    private val renderDiagnosticName: Boolean
) : AbstractAnalyzerWithCompilerReport
{
    override val targetEnvironment: TargetEnvironment
        get() = CompilerEnvironment
    override lateinit var analysisResult: AnalysisResult

    override fun analyzeAndReport(files: Collection<CjFile>, analyze: () -> AnalysisResult) {
        analysisResult = analyze()
//        if (!analysisResult.isError()) {
//            OptInUsageChecker.checkCompilerArguments(
//                analysisResult.moduleDescriptor, languageVersionSettings,
//                reportError = { message -> messageCollector.report(ERROR, message) },
//                reportWarning = { message -> messageCollector.report(WARNING, message) }
//            )
//        }
//        reportSyntaxErrors(files)
//        reportDiagnostics(analysisResult.bindingContext.diagnostics, messageCollector, renderDiagnosticName)
//        reportIncompleteHierarchies()
//        reportAlternativeSignatureErrors()
    }
    constructor(configuration: CompilerConfiguration) : this(
        configuration.messageCollector,
        configuration.languageVersionSettings,
        configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    )
    override fun hasErrors(): Boolean =
        messageCollector.hasErrors()


}
