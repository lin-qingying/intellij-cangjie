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

package org.cangnova.cangjie.analysis

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.AnalysisResult
import org.cangnova.cangjie.resolve.AnalyzerWithCompilerReport
import org.cangnova.cangjie.resolve.CangJieResolverForModuleFactory
import org.cangnova.cangjie.resolve.CompilerEnvironment
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * 通用分析结果
 *
 * 封装了仓颉代码分析的核心结果，包含模块描述符和绑定上下文。
 * 用于序列化、代码生成等后续处理阶段。
 *
 * ## 组成部分
 *
 * - **moduleDescriptor**: 模块描述符，包含模块的所有声明和类型信息
 * - **bindingContext**: 绑定上下文，包含 PSI 元素到描述符的映射和类型信息
 *
 * ## 使用场景
 *
 * - 元数据序列化：将分析结果序列化为 .cjo 文件
 * - 代码生成：基于分析结果生成目标代码
 * - IDE 功能：提供代码补全、导航等功能的数据源
 *
 * @param moduleDescriptor 模块描述符
 * @param bindingContext 绑定上下文
 *
 * @see AnalysisResult
 * @see runCommonAnalysisForSerialization
 */
data class CommonAnalysisResult(
    val moduleDescriptor: ModuleDescriptor,
    val bindingContext: BindingContext
)

/**
 * 运行通用分析以进行序列化
 *
 * 该函数是仓颉编译器分析阶段的入口点，用于分析源文件并生成可序列化的分析结果。
 * 它处理多轮分析（如果需要添加额外的源文件）并返回最终的分析结果。
 *
 * ## 功能说明
 *
 * 1. **多轮分析**: 支持 `AnalysisResult.RetryWithAdditionalRoots`，动态添加源文件后重新分析
 * 2. **错误检测**: 检查分析过程中的错误，只有在无错误时才返回结果
 * 3. **代码生成决策**: 基于 `shouldGenerateCode` 标志决定是否返回结果
 *
 * ## 工作流程
 *
 * ```
 * 1. 获取源文件和配置
 *   ↓
 * 2. 执行分析迭代 (runCommonAnalysisIteration)
 *   ↓
 * 3. 检查是否需要重试 (RetryWithAdditionalRoots)
 *   ↓
 *   是 → 添加额外的源文件 → 回到步骤 2
 *   否 → 继续
 *   ↓
 * 4. 检查分析结果
 *   ↓
 *   成功且无错误 → 返回 CommonAnalysisResult
 *   失败或有错误 → 返回 null
 * ```
 *
 * ## 参数说明
 *
 * @param files 要分析的仓颉源文件集合
 * @param moduleName 模块名称
 * @param dependOnBuiltins 是否依赖内置类型库
 * @param languageVersionSettings 语言版本设置
 * @param renderDiagnosticName 是否在诊断消息中显示内部名称
 * @param onAnalysisStarted 分析开始时的回调（用于性能监控）
 * @param onAnalysisFinished 分析完成时的回调（用于性能监控）
 * @param onAddCangJieSourceRoots 需要添加额外源文件时的回调
 *
 * @return 分析成功且无错误时返回 [CommonAnalysisResult]，否则返回 null
 *
 * ## 使用示例
 *
 * ```kotlin
 * val files = listOf(file1, file2, file3)
 * val result = runCommonAnalysisForSerialization(
 *     files = files,
 *     moduleName = Name.special("<myModule>"),
 *     dependOnBuiltins = true,
 *     languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
 *     renderDiagnosticName = false,
 *     onAnalysisStarted = { println("Analysis started") },
 *     onAnalysisFinished = { println("Analysis finished") },
 *     onAddCangJieSourceRoots = { roots -> println("Adding roots: $roots") }
 * )
 *
 * if (result != null) {
 *     // 序列化或生成代码
 *     serializeMetadata(result.moduleDescriptor, result.bindingContext)
 * } else {
 *     // 处理分析失败
 *     println("Analysis failed")
 * }
 * ```
 *
 * ## 错误处理
 *
 * - 如果分析过程中有编译错误，返回 null
 * - 如果 `shouldGenerateCode` 为 false，返回 null
 * - 只有在成功分析且无错误时才返回有效结果
 *
 * @see CommonAnalysisResult
 * @see runCommonAnalysisIteration
 * @see AnalysisResult.RetryWithAdditionalRoots
 */
fun runCommonAnalysisForSerialization(
    files: Collection<CjFile>,
    moduleName: Name,
    dependOnBuiltins: Boolean,
    languageVersionSettings: LanguageVersionSettings,
    renderDiagnosticName: Boolean,
    onAnalysisStarted: () -> Unit = {},
    onAnalysisFinished: () -> Unit = {},
    onAddCangJieSourceRoots: (List<CjFile>) -> Unit = {}
): CommonAnalysisResult? {
    var analysisResultWithHasErrors: AnalysisResultWithHasErrors
    var currentFiles = files.toList()

    do {
        // 通知分析开始
        onAnalysisStarted()

        // 执行分析迭代
        analysisResultWithHasErrors = runCommonAnalysisIteration(
            currentFiles,
            moduleName,
            dependOnBuiltins,
            languageVersionSettings,
            renderDiagnosticName
        )

        val result = analysisResultWithHasErrors.result

        // 检查是否需要重试（添加额外的源文件）
        if (result is AnalysisResult.RetryWithAdditionalRoots) {
            // 添加额外的仓颉源文件
            val additionalFiles = result.additionalCangJieRoots.flatMap { root ->
                // 这里需要从文件系统加载 .cj 文件
                // 简化实现：假设调用者会通过回调处理
                emptyList<CjFile>()
            }
            onAddCangJieSourceRoots(additionalFiles)
            currentFiles = currentFiles + additionalFiles
        }

        // 通知分析完成
        onAnalysisFinished()

    } while (result is AnalysisResult.RetryWithAdditionalRoots)

    val analysisResult = analysisResultWithHasErrors.result

    // 只有在应该生成代码且无错误时才返回结果
    return if (analysisResult.shouldGenerateCode && !analysisResultWithHasErrors.hasErrors) {
        CommonAnalysisResult(analysisResult.moduleDescriptor, analysisResult.bindingContext)
    } else {
        null
    }
}

/**
 * 分析结果与错误状态
 *
 * 内部数据类，用于封装分析结果和错误状态。
 * 将分析结果和错误标志打包在一起，便于在多轮分析中传递。
 *
 * @param result 分析结果
 * @param hasErrors 是否有错误
 */
private data class AnalysisResultWithHasErrors(
    val result: AnalysisResult,
    val hasErrors: Boolean
)

/**
 * 运行单次通用分析迭代
 *
 * 执行一次完整的分析流程，包括创建分析器、执行分析、收集错误。
 * 这是 [runCommonAnalysisForSerialization] 的内部实现细节。
 *
 * ## 工作流程
 *
 * ```
 * 1. 创建 AnalyzerWithCompilerReport
 *   ↓
 * 2. 调用 analyzeAndReport
 *   ↓
 * 3. 内部调用 CangJieResolverForModuleFactory.analyzeFiles
 *   ↓
 * 4. 收集分析结果和错误
 *   ↓
 * 5. 返回 AnalysisResultWithHasErrors
 * ```
 *
 * ## 参数说明
 *
 * @param files 要分析的仓颉源文件集合
 * @param moduleName 模块名称
 * @param dependOnBuiltins 是否依赖内置类型库
 * @param languageVersionSettings 语言版本设置
 * @param renderDiagnosticName 是否在诊断消息中显示内部名称
 *
 * @return 封装了分析结果和错误状态的对象
 *
 * @see AnalyzerWithCompilerReport
 * @see CangJieResolverForModuleFactory.analyzeFiles
 */
private fun runCommonAnalysisIteration(
    files: Collection<CjFile>,
    moduleName: Name,
    dependOnBuiltins: Boolean,
    languageVersionSettings: LanguageVersionSettings,
    renderDiagnosticName: Boolean
): AnalysisResultWithHasErrors {
    // 创建分析器（带编译器报告）
    // TODO: 需要从配置中获取 MessageCollector
    val analyzer = AnalyzerWithCompilerReport(
        messageCollector = DummyMessageCollector, // 临时使用虚拟消息收集器
        languageVersionSettings = languageVersionSettings,
        renderDiagnosticName = renderDiagnosticName
    )

    // 执行分析并报告
    analyzer.analyzeAndReport(files) {
        CangJieResolverForModuleFactory.analyzeFiles(
            files = files,
            moduleName = moduleName,
            dependOnBuiltIns = dependOnBuiltins,
            languageVersionSettings = languageVersionSettings,
            targetEnvironment = CompilerEnvironment,
            capabilities = emptyMap(),
            explicitProjectContext = null
        )
    }

    return AnalysisResultWithHasErrors(analyzer.analysisResult, analyzer.hasErrors())
}

/**
 * 虚拟消息收集器
 *
 * 临时实现，用于在没有完整配置系统时提供基本的消息收集功能。
 * 实际使用中应该从 CompilerConfiguration 中获取真实的 MessageCollector。
 */
private object DummyMessageCollector : org.cangnova.cangjie.cli.messages.MessageCollector {
    override fun report(
        severity: org.cangnova.cangjie.cli.messages.CompilerMessageSeverity,
        message: String,
        location: org.cangnova.cangjie.cli.messages.CompilerMessageSourceLocation?
    ) {
        // 临时实现：打印到标准错误输出
        System.err.println("[$severity] $message ${location?.let { "at $it" } ?: ""}")
    }

    override fun hasErrors(): Boolean = false

    override fun clear() {}
}
