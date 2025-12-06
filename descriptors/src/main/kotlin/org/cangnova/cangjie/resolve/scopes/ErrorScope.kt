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

package org.cangnova.cangjie.resolve.scopes

import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.DescriptorWithDeprecation
import org.cangnova.cangjie.descriptors.PropertyDescriptor
import org.cangnova.cangjie.descriptors.SimpleFunctionDescriptor
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.descriptors.macro.ErrorMacroDescriptor
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.error.ErrorClassDescriptor
import org.cangnova.cangjie.types.error.ErrorEntity
import org.cangnova.cangjie.types.error.ErrorFunctionDescriptor
import org.cangnova.cangjie.types.error.ErrorScopeKind
import org.cangnova.cangjie.utils.Printer

/**
 * 错误恢复作用域 - 用于错误恢复和增强错误报告的特殊作用域
 *
 * ErrorScope 是一个特殊的作用域实现，用于在符号解析失败时提供错误恢复能力。
 * 当正常的作用域无法解析符号时，ErrorScope 返回错误描述符，使编译器能够继续分析，
 * 同时提供有意义的错误信息，提升 IDE 的健壮性。
 *
 * ## 核心功能
 *
 * ### 错误描述符生成
 *
 * 所有查找方法都返回预定义的错误描述符：
 * - [getContributedClassifier]: 返回 [ErrorClassDescriptor]
 * - [getContributedFunctions]: 返回 [ErrorFunctionDescriptor]
 * - [getContributedVariables]: 返回错误变量组
 * - [getContributedPropertys]: 返回错误属性组
 * - [getContributedMacros]: 返回 [ErrorMacroDescriptor]
 *
 * ### 错误类型
 *
 * 通过 [ErrorScopeKind] 区分不同的错误场景：
 * - 缺失作用域
 * - 未解析的引用
 * - 循环依赖
 * - 类型错误
 * - 其他语义错误
 *
 * ## 设计哲学
 *
 * ### 错误恢复（Error Recovery）
 *
 * 编译器在遇到错误时不应立即终止，而应：
 * 1. 报告当前错误
 * 2. 创建错误占位符
 * 3. 继续后续分析
 * 4. 尽可能多地发现错误
 *
 * ErrorScope 使得编译器能够在符号解析失败后继续工作，避免级联错误。
 *
 * ### IDE 友好性
 *
 * 在 IDE 中，用户代码经常处于不完整状态：
 * - 正在输入的代码
 * - 未保存的更改
 * - 临时的语法错误
 *
 * ErrorScope 确保 IDE 功能（补全、高亮、导航等）在这些情况下依然可用。
 *
 * ## 使用场景
 *
 * ### 1. 未解析的引用
 *
 * 当解析到未知符号时，返回 ErrorScope 而非 null：
 *
 * ```kotlin
 * // 源代码: unknownVar.method()
 * // unknownVar 无法解析
 *
 * val varType = resolveVariable("unknownVar")
 *     ?: ErrorUtils.createErrorType()
 *
 * // 不是返回 null，而是返回 ErrorScope
 * val memberScope = varType.memberScope
 * // memberScope 是 ErrorScope 实例
 *
 * // 继续解析 method 调用，返回 ErrorFunctionDescriptor
 * val method = memberScope.getContributedFunctions(
 *     Name.identifier("method"),
 *     location
 * ).firstOrNull() // 不会是 null
 * ```
 *
 * ### 2. 类型检查失败后的继续分析
 *
 * ```kotlin
 * // class MyClass {}
 * // fun test(obj: MyClass) {
 * //     obj.nonExistentMethod() // 方法不存在
 * //     obj.anotherBadCall()   // 仍然可以分析
 * // }
 *
 * // 第一个错误后，继续分析第二个调用
 * ```
 *
 * ### 3. IDE 中的不完整代码
 *
 * ```kotlin
 * // 用户正在输入：
 * // let x = someUnknownFunction().
 * //                               ^ 光标位置，请求补全
 *
 * // someUnknownFunction 不存在，但应该提供基本补全
 * val resultType = resolveCall("someUnknownFunction")
 *     ?: ErrorUtils.createErrorType()
 *
 * // resultType.memberScope 是 ErrorScope
 * // 补全系统仍能工作，提供通用建议
 * ```
 *
 * ## 实现细节
 *
 * ### 错误消息格式化
 *
 * 构造时传入 [ErrorScopeKind] 和格式参数：
 *
 * ```kotlin
 * val scope = ErrorScope(
 *     ErrorScopeKind.MISSING_DEPENDENCY,
 *     "com.example.missing.Module"
 * )
 * // debugMessage = "Missing dependency: com.example.missing.Module"
 * ```
 *
 * ### 默认错误描述符
 *
 * 使用 [ErrorUtils] 提供的单例错误描述符：
 * - `ErrorUtils.errorClass`: 通用错误类
 * - `ErrorUtils.errorVariableGroup`: 错误变量集合
 * - `ErrorUtils.errorPropertyGroup`: 错误属性集合
 *
 * ### 名称集合语义
 *
 * - [functionNames]: 返回空集（无已知函数）
 * - [variableNames]: 返回空集
 * - [classifierNames]: 返回空集
 * - [propertyNames]: 返回空集
 * - [definitelyDoesNotContainName]: 总是返回 false（任何查找都可能返回错误描述符）
 *
 * ## 错误描述符的特性
 *
 * ### 类型传播
 *
 * 错误描述符的类型也是错误类型，错误会传播：
 *
 * ```kotlin
 * val errorFunction = errorScope.getContributedFunctions(name, location).first()
 * val returnType = errorFunction.returnType // 也是 ErrorType
 * val memberScope = returnType.memberScope // 也是 ErrorScope
 * ```
 *
 * ### 错误抑制
 *
 * 一旦进入错误状态，后续错误可以被抑制，避免级联报告：
 *
 * ```kotlin
 * if (descriptor.isError) {
 *     // 不报告后续错误，因为根本原因已知
 *     return
 * }
 * ```
 *
 * ## 与正常作用域的区别
 *
 * | 特性 | ErrorScope | 正常作用域 |
 * |------|-----------|----------|
 * | 查找结果 | 总是返回错误描述符 | 返回实际符号或 null |
 * | 名称集合 | 返回空集 | 返回实际名称 |
 * | 调试信息 | 包含错误消息 | 包含作用域描述 |
 * | 用途 | 错误恢复 | 正常解析 |
 *
 * ## 注意事项
 *
 * 1. **不应直接创建**: 通常通过 [ErrorUtils] 工厂方法创建
 * 2. **不可用于类型推导**: ErrorScope 的结果不应参与类型推导
 * 3. **仅用于诊断**: 错误描述符主要用于报告和恢复，不应生成代码
 * 4. **弃用处理**: `getContributedClassifierIncludeDeprecated` 返回 null，因为错误无弃用状态
 *
 * ## 错误类型层次
 *
 * ```
 * ErrorScope (作用域层面)
 *   ↓ 返回
 * ErrorDescriptor (符号层面)
 *   ↓ 具有
 * ErrorType (类型层面)
 *   ↓ 其成员作用域又是
 * ErrorScope (循环)
 * ```
 *
 * ## 典型使用模式
 *
 * ### 模式 1: 安全的符号解析
 *
 * ```kotlin
 * fun resolveType(name: String): CangJieType {
 *     val classifier = scope.getContributedClassifier(
 *         Name.identifier(name),
 *         location
 *     )
 *     return when {
 *         classifier == null -> {
 *             // 报告错误
 *             reportError("Unresolved reference: $name")
 *             // 返回错误类型（其 memberScope 是 ErrorScope）
 *             ErrorUtils.createErrorType()
 *         }
 *         classifier is ErrorClassDescriptor -> {
 *             // 已经是错误，不重复报告
 *             ErrorUtils.createErrorType()
 *         }
 *         else -> classifier.defaultType
 *     }
 * }
 * ```
 *
 * ### 模式 2: IDE 中的容错补全
 *
 * ```kotlin
 * fun provideCompletion(receiverType: CangJieType): List<CompletionItem> {
 *     val memberScope = receiverType.memberScope
 *
 *     // 即使是 ErrorScope 也尝试提供补全
 *     val members = memberScope.getContributedDescriptors()
 *
 *     return if (memberScope is ErrorScope) {
 *         // 提供通用建议或基于上下文的猜测
 *         provideGenericCompletions()
 *     } else {
 *         // 正常补全
 *         members.map { createCompletionItem(it) }
 *     }
 * }
 * ```
 *
 * ## 示例：完整的错误恢复流程
 *
 * ```kotlin
 * // 源代码（有错误）:
 * // let x = unknownFunction()
 * // let y = x.someMethod()
 *
 * // 1. 尝试解析 unknownFunction
 * val functionDescriptor = scope.getContributedFunctions(
 *     Name.identifier("unknownFunction"),
 *     location
 * ).firstOrNull()
 *
 * if (functionDescriptor == null) {
 *     // 2. 报告错误
 *     report("Unresolved reference: unknownFunction")
 *
 *     // 3. 创建错误类型
 *     val errorType = ErrorUtils.createErrorType()
 *
 *     // 4. errorType.memberScope 是 ErrorScope
 *     // 继续分析 x.someMethod()
 *     val memberScope = errorType.memberScope // ErrorScope 实例
 *
 *     // 5. someMethod 查找返回 ErrorFunctionDescriptor
 *     val someMethod = memberScope.getContributedFunctions(
 *         Name.identifier("someMethod"),
 *         location
 *     ).firstOrNull() // 返回 ErrorFunctionDescriptor，不是 null
 *
 *     // 6. 不再报告 y 的类型错误，因为 x 已经是错误
 *     // 编译继续，用户看到一个明确的错误而非级联错误
 * }
 * ```
 *
 * @property kind 错误作用域的类型，定义错误场景
 * @property debugMessage 格式化的调试消息，用于诊断输出
 *
 * @see ErrorUtils 错误工具类，提供错误描述符和类型
 * @see ErrorScopeKind 错误场景枚举
 * @see ErrorClassDescriptor 错误类描述符
 * @see ErrorFunctionDescriptor 错误函数描述符
 * @see MemberScope 父接口
 */
open class ErrorScope(val kind: ErrorScopeKind, vararg formatParams: String) : MemberScope {
    protected val debugMessage = kind.debugMessage.format(*formatParams)

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor =
        ErrorClassDescriptor(Name.Companion.special(ErrorEntity.ERROR_CLASS.debugText.format(name)))


    override fun getContributedClassifierIncludeDeprecated(
        name: Name, location: LookupLocation
    ): DescriptorWithDeprecation<ClassifierDescriptor>? = null

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<@JvmWildcard VariableDescriptor> =
        ErrorUtils.errorVariableGroup

    override fun getContributedPropertys (name: Name, location: LookupLocation): Set<PropertyDescriptor> =
        ErrorUtils.errorPropertyGroup

    override fun getContributedFunctions(name: Name, location: LookupLocation): Set<SimpleFunctionDescriptor> =
        setOf(ErrorFunctionDescriptor(ErrorUtils.errorClass))



    override fun getContributedMacros(name: Name, location: LookupLocation): Collection<MacroDescriptor> =
    setOf(ErrorMacroDescriptor(ErrorUtils.errorClass))
    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter, nameFilter: Function1<Name, Boolean>
    ): Collection<DeclarationDescriptor> = emptyList()

    override val functionNames: Set<Name> = emptySet()
    override val variableNames: Set<Name> = emptySet()
    override val classifierNames: Set<Name>? = emptySet()
    override val propertyNames: Set<Name> = emptySet()
    override fun recordLookup(name: Name, location: LookupLocation) {}
    override fun definitelyDoesNotContainName(name: Name): Boolean = false

    override fun toString(): String = "ErrorScope{$debugMessage}"

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.simpleName, ": ", debugMessage)
    }
}