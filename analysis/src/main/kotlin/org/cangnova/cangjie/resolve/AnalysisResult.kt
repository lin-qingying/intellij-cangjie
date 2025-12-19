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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.types.ErrorUtils
import java.io.File

/**
 * 编译错误异常
 *
 * 当代码分析过程中检测到编译错误时抛出此异常。
 * 此异常用于区分编译级别的错误（用户代码问题）和内部错误（分析器本身的问题）。
 *
 * ## 使用场景
 *
 * - **编译失败**: 用户代码包含语法错误、类型错误等
 * - **中断编译**: 需要立即停止编译过程
 * - **错误报告**: 向用户报告编译失败的原因
 *
 * ## 与 InternalError 的区别
 *
 * - CompilationErrorException: 用户代码的问题（预期的错误）
 * - InternalError: 分析器内部的问题（意外的错误，应该修复）
 *
 * @see AnalysisResult.CompilationError
 * @see AnalysisResult.throwIfError
 */
class CompilationErrorException : RuntimeException {
    constructor() : super()
    constructor(message: String) : super(message)
}

/**
 * 委托分析结果
 *
 * 该类将分析结果的访问委托给内部的 result 对象。
 * 用于包装和转发分析结果，同时保持对原始结果的引用。
 *
 * ## 使用场景
 *
 * - **结果包装**: 在不修改原始结果的情况下添加额外功能
 * - **结果转发**: 将一个分析结果的数据转发到另一个上下文
 * - **结果追踪**: 保持对原始结果的引用以进行调试或日志记录
 *
 * ## 示例
 *
 * ```kotlin
 * val originalResult = AnalysisResult.success(bindingContext, module)
 * val delegateResult = DelegateAnalysisResult(originalResult)
 * // delegateResult 拥有与 originalResult 相同的数据
 * ```
 *
 * @param result 被委托的原始分析结果
 */
class DelegateAnalysisResult(
    val result: AnalysisResult
) : AnalysisResult(
    result.bindingContext,
    result.moduleDescriptor,
    result.shouldGenerateCode
)

/**
 * 分析结果
 *
 * 该类表示仓颉代码分析的结果，包含了分析过程生成的所有信息。
 * 分析结果可能是成功的、包含编译错误的，或者遇到了内部错误。
 *
 * ## 核心组件
 *
 * 1. **BindingContext**: 绑定上下文，包含所有符号解析和类型推导的结果
 * 2. **ModuleDescriptor**: 模块描述符，表示被分析的模块
 * 3. **shouldGenerateCode**: 是否应该生成代码（编译成功且无致命错误）
 *
 * ## 结果类型
 *
 * - **成功** (AnalysisResult): 分析成功完成
 * - **编译错误** (CompilationError): 用户代码包含错误
 * - **内部错误** (InternalError): 分析器内部出现异常
 * - **需要重试** (RetryWithAdditionalRoots): 需要添加额外的根目录后重新分析
 *
 * ## 使用流程
 *
 * ```
 * 执行代码分析
 *   ↓
 * 返回 AnalysisResult
 *   ↓
 * 检查 isError()
 *   ↓
 * 如果有错误，调用 throwIfError()
 *   ↓
 * 否则，使用 bindingContext 获取分析信息
 * ```
 *
 * ## 使用示例
 *
 * ```kotlin
 * val result = analyzeFile(file)
 *
 * // 检查是否有错误
 * if (result.isError()) {
 *     try {
 *         result.throwIfError()
 *     } catch (e: CompilationErrorException) {
 *         // 处理编译错误
 *         println("Compilation failed")
 *     } catch (e: IllegalStateException) {
 *         // 处理内部错误
 *         println("Internal error: ${e.message}")
 *     }
 * } else {
 *     // 使用分析结果
 *     val context = result.bindingContext
 *     val module = result.moduleDescriptor
 * }
 * ```
 *
 * @param bindingContext 绑定上下文，包含符号解析和类型信息
 * @param moduleDescriptor 模块描述符
 * @param shouldGenerateCode 是否应该生成代码
 *
 * @see BindingContext
 * @see ModuleDescriptor
 * @see CompilationError
 * @see InternalError
 * @see RetryWithAdditionalRoots
 */
open class AnalysisResult protected constructor(
    val bindingContext: BindingContext,
    val moduleDescriptor: ModuleDescriptor,
    val shouldGenerateCode: Boolean = true
) {
    /**
     * 检查分析是否出错
     *
     * @return true 如果是内部错误或编译错误，否则返回 false
     */
    fun isError(): Boolean = this is InternalError || this is CompilationError

    /**
     * 编译错误结果
     *
     * 表示用户代码包含编译错误（如语法错误、类型错误等）。
     * 这是一个预期的错误类型，用于处理用户代码中的问题。
     *
     * ## 特征
     *
     * - moduleDescriptor 设置为 ErrorUtils.errorModule
     * - bindingContext 包含部分分析结果（到错误点为止）
     * - shouldGenerateCode 默认为 true（继承自父类）
     *
     * @param bindingContext 包含错误信息的绑定上下文
     */
    private class CompilationError(bindingContext: BindingContext) :
        AnalysisResult(bindingContext, ErrorUtils.errorModule)

    /**
     * 如果分析结果包含错误，则抛出相应的异常
     *
     * ## 抛出的异常
     *
     * - **IllegalStateException**: 当结果是内部错误时
     * - **CompilationErrorException**: 当结果是编译错误时
     * - 无异常: 当结果成功时
     *
     * ## 使用场景
     *
     * - **编译器流程**: 在代码生成前检查是否可以继续
     * - **测试**: 验证分析是否成功
     * - **错误处理**: 将错误转换为异常以便统一处理
     *
     * @throws IllegalStateException 内部错误
     * @throws CompilationErrorException 编译错误
     */
    fun throwIfError() {
        when (this) {
            is InternalError -> throw IllegalStateException("failed to analyze: $error", error)
            is CompilationError -> throw CompilationErrorException()
        }
    }

    /**
     * 需要重试的分析结果
     *
     * 表示分析过程需要添加额外的根目录后重新执行。
     * 这通常发生在增量编译或动态发现依赖的场景中。
     *
     * ## 使用场景
     *
     * - **增量编译**: 发现新的源文件需要加入编译
     * - **依赖发现**: 动态发现缺失的依赖库
     * - **多轮分析**: 需要多次迭代才能完成的复杂分析
     *
     * ## 示例
     *
     * ```kotlin
     * val result = analyze(sources)
     * if (result is AnalysisResult.RetryWithAdditionalRoots) {
     *     // 添加新发现的根目录
     *     val newSources = sources + result.additionalCangJieRoots
     *     val newClassPath = classPath + result.additionalClassPathRoots
     *
     *     // 重新分析
     *     val finalResult = analyze(newSources, newClassPath)
     * }
     * ```
     *
     * @param bindingContext 当前的绑定上下文
     * @param moduleDescriptor 当前的模块描述符
     * @param additionalCangJieRoots 需要添加的仓颉源码根目录
     * @param additionalClassPathRoots 需要添加的类路径根目录
     * @param addToEnvironment 是否应该将新根目录添加到环境中
     */
    class RetryWithAdditionalRoots(
        bindingContext: BindingContext,
        moduleDescriptor: ModuleDescriptor,
        /**
         * 需要添加的仓颉源码根目录列表
         */
        val additionalCangJieRoots: List<File>,
        /**
         * 需要添加的类路径根目录列表（默认为空）
         */
        val additionalClassPathRoots: List<File> = emptyList(),
        /**
         * 是否应该将新根目录添加到分析环境中（默认为 true）
         */
        val addToEnvironment: Boolean = true
    ) : AnalysisResult(bindingContext, moduleDescriptor)

    /**
     * 内部错误结果
     *
     * 表示分析器内部出现了意外的异常。
     * 这是一个不应该发生的错误，通常表示分析器本身的 bug。
     *
     * ## 特征
     *
     * - moduleDescriptor 设置为 ErrorUtils.errorModule
     * - 保存了原始异常以便调试
     * - 应该被报告为 bug
     *
     * ## 示例
     *
     * ```kotlin
     * try {
     *     // 执行分析
     * } catch (e: Exception) {
     *     return AnalysisResult.internalError(bindingContext, e)
     * }
     * ```
     *
     * @param bindingContext 错误发生时的绑定上下文
     * @param exception 导致错误的异常
     */
    private class InternalError(
        bindingContext: BindingContext,
        val exception: Throwable
    ) : AnalysisResult(bindingContext, ErrorUtils.errorModule)

    /**
     * 获取错误异常
     *
     * 只有在结果是 [InternalError] 时才能调用此属性。
     *
     * @return 导致内部错误的异常
     * @throws IllegalStateException 如果结果不是内部错误
     */
    val error: Throwable
        get() = if (this is InternalError) this.exception else throw IllegalStateException("Should only be called for error analysis result")

    companion object {
        /**
         * 空的分析结果
         *
         * 用于初始化或占位的空结果。
         * 包含空的绑定上下文和错误模块。
         */
        val EMPTY: AnalysisResult = success(BindingContext.EMPTY, ErrorUtils.errorModule)

        /**
         * 创建成功的分析结果
         *
         * ## 使用场景
         *
         * - 分析成功完成
         * - 无编译错误
         * - 准备好进行代码生成
         *
         * @param bindingContext 分析生成的绑定上下文
         * @param module 被分析的模块
         * @return 成功的分析结果
         */
        @JvmStatic
        fun success(bindingContext: BindingContext, module: ModuleDescriptor): AnalysisResult {
            return AnalysisResult(bindingContext, module)
        }

        /**
         * 创建成功的分析结果，并指定是否生成代码
         *
         * ## 使用场景
         *
         * - 分析成功，但可能包含警告
         * - 需要控制是否生成代码
         * - 语法检查模式（只分析不生成代码）
         *
         * @param bindingContext 分析生成的绑定上下文
         * @param module 被分析的模块
         * @param shouldGenerateCode 是否应该生成代码
         * @return 成功的分析结果
         */
        @JvmStatic
        fun success(
            bindingContext: BindingContext,
            module: ModuleDescriptor,
            shouldGenerateCode: Boolean
        ): AnalysisResult {
            return AnalysisResult(bindingContext, module, shouldGenerateCode)
        }

        /**
         * 创建内部错误结果
         *
         * 当分析器内部发生意外异常时使用。
         * 这种错误应该被视为 bug 并报告。
         *
         * ## 使用场景
         *
         * - 分析器代码抛出了未预期的异常
         * - 断言失败
         * - 内部状态不一致
         *
         * @param bindingContext 错误发生时的绑定上下文
         * @param error 导致错误的异常
         * @return 内部错误结果
         */
        @JvmStatic
        fun internalError(bindingContext: BindingContext, error: Throwable): AnalysisResult {
            return InternalError(bindingContext, error)
        }
    }
}
