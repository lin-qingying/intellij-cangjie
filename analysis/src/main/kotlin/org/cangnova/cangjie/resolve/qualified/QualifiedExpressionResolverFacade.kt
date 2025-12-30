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
 */

package org.cangnova.cangjie.resolve.qualified

import jakarta.inject.Inject
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.TypeResolver
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.CallExpressionElement
import org.cangnova.cangjie.resolve.module
import org.cangnova.cangjie.resolve.qualified.context.ResolutionContext
import org.cangnova.cangjie.resolve.qualified.resolvers.*
import org.cangnova.cangjie.resolve.qualified.validation.ReexportValidator
import org.cangnova.cangjie.resolve.qualified.validation.VisibilityChecker
import org.cangnova.cangjie.resolve.scopes.ImportingScope
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.receivers.QualifierReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.Receiver
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext

/**
 * 限定表达式解析器门面
 *
 * 负责协调各个专门的解析器，提供统一的名称解析接口。
 * 这是仓颉语言语义分析的核心入口之一。
 *
 * ## 架构设计
 *
 * 本类采用门面模式（Facade Pattern），将复杂的解析逻辑委托给专门的解析器：
 *
 * - [PackageHeaderResolver] - 包声明解析
 * - [ImportResolver] - 导入语句解析
 * - [TypeReferenceResolver] - 类型引用解析
 * - [ExpressionQualifierResolver] - 表达式限定符解析
 * - [QualifierPrefixResolver] - 限定符前缀解析（核心算法）
 *
 * ## 核心职责
 *
 * 1. **包声明解析**：验证包路径的有效性
 * 2. **导入解析**：将导入语句转换为导入作用域
 * 3. **类型解析**：将类型引用解析为类型描述符
 * 4. **表达式解析**：解析表达式中的限定符
 * 5. **可见性检查**：确保符号在当前上下文中可访问
 *
 * ## 解析策略
 *
 * 不同位置应用不同的解析策略：
 *
 * | 位置 | 策略 | 示例 |
 * |------|------|------|
 * | 包声明 | 只验证路径 | `package com.example` |
 * | 导入 | 解析包/类/类型别名 | `import std.core.String` |
 * | 类型 | 只解析类型 | `var x: String` |
 * | 表达式 | 值优先于类型 | `Foo.bar()` |
 *
 * @param languageVersionSettings 语言版本设置，用于控制语言特性和可见性检查
 *
 * @see QualifierPosition 解析位置枚举
 * @see ResolutionContext 解析上下文
 */
class QualifiedExpressionResolverFacade(
    val languageVersionSettings: LanguageVersionSettings
) {
    @set:Inject
      lateinit var typeResolver: TypeResolver

    companion object {
        /**
         * IDE 解析模式的根前缀
         *
         * 用户不应直接使用此前缀。
         * 在 IDE 中作为非根路径的前缀，避免解析时的冲突。
         */
        const val ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE = "_root_ide_package_"
        const val ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE_WITH_DOT = "$ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE."
    }

    // 验证器
    private val visibilityChecker = VisibilityChecker(languageVersionSettings)
    private val reexportValidator = ReexportValidator()

    // 核心解析器
    private val prefixResolver by lazy {
        QualifierPrefixResolver(visibilityChecker = visibilityChecker)
    }

    // 专门解析器
    private val packageHeaderResolver by lazy {
        PackageHeaderResolver()
    }

    private val importResolver by lazy {
        ImportResolver(languageVersionSettings, prefixResolver, visibilityChecker, reexportValidator)
    }

    private val typeReferenceResolver by lazy {
        TypeReferenceResolver(prefixResolver, visibilityChecker)
    }

    private val expressionQualifierResolver by lazy {
        ExpressionQualifierResolver(typeResolver, prefixResolver,languageVersionSettings)
    }

    // ==================== 包声明解析 ====================

    /**
     * 解析包声明（package directive）
     *
     * 验证并记录包声明中的每个名称部分，确保包路径的每一级都有效。
     *
     * @param packageDirective 包声明 PSI 节点
     * @param module 模块描述符
     * @param trace 用于记录绑定信息
     */
    fun resolvePackageHeader(
        packageDirective: CjPackageDirective,
        module: ModuleDescriptor,
        trace: BindingTrace
    ) {
        packageHeaderResolver.resolve(packageDirective, module, trace)
    }

    // ==================== 导入解析 ====================

    /**
     * 处理导入引用
     *
     * 将导入语句解析为导入作用域，用于后续的名称解析。
     *
     * @param importDirective 导入指令
     * @param moduleDescriptor 模块描述符
     * @param trace 用于记录绑定信息
     * @param excludedImportNames 排除的导入名称（避免循环导入）
     * @param packageFragmentForVisibilityCheck 用于可见性检查的包片段
     * @return 导入作用域
     */
    fun processImportReference(
        importDirective: CjImportInfo,
        moduleDescriptor: ModuleDescriptor,
        trace: BindingTrace,
        excludedImportNames: Collection<FqName>,
        packageFragmentForVisibilityCheck: PackageFragmentDescriptor?
    ): ImportingScope? {
        val context = ResolutionContext.fromModule(
            trace = trace,
            module = moduleDescriptor,
            position = QualifierPosition.IMPORT,
            languageVersionSettings = languageVersionSettings,
            visibleFrom = packageFragmentForVisibilityCheck
        )

        return importResolver.processImport(importDirective, context, excludedImportNames)
    }

    // ==================== 类型解析 ====================

    /**
     * 解析用户类型的描述符
     *
     * 将源码中的类型引用解析为对应的类型描述符。
     *
     * @param userType PSI 中的用户类型节点
     * @param scope 解析的词法作用域
     * @param trace 用于记录绑定信息和诊断
     * @param isDebuggerContext 是否在调试器上下文中
     * @return 类型解析结果
     */
    fun resolveDescriptorForType(
        userType: CjUserType,
        scope: LexicalScope,
        trace: BindingTrace,
        isDebuggerContext: Boolean
    ): TypeQualifierResolutionResult {
        val context = ResolutionContext.fromScope(
            trace = trace,
            scope = scope,
            position = QualifierPosition.TYPE,
            languageVersionSettings = languageVersionSettings,
            isDebuggerContext = isDebuggerContext
        )

        return typeReferenceResolver.resolve(userType, context)
    }

    /**
     * 解析双冒号左侧的描述符
     *
     * 用于成员引用表达式（如 `String::length`）的左侧类型解析。
     *
     * @param expression 表达式
     * @param scope 词法作用域
     * @param trace 绑定追踪器
     * @param isDebuggerContext 是否在调试器上下文中
     * @return 类型解析结果
     */
    fun resolveDescriptorForDoubleColonLHS(
        expression: CjExpression,
        scope: LexicalScope,
        trace: BindingTrace,
        isDebuggerContext: Boolean
    ): TypeQualifierResolutionResult {
        val context = ResolutionContext.fromScope(
            trace = trace,
            scope = scope,
            position = QualifierPosition.TYPE,
            languageVersionSettings = languageVersionSettings,
            isDebuggerContext = isDebuggerContext
        )

        return typeReferenceResolver.resolveForDoubleColonLHS(expression, context)
    }

    // ==================== 表达式解析 ====================

    /**
     * 解析限定表达式中的类或包
     *
     * 从左到右解析限定表达式，尽可能多地识别出类或包的前缀部分。
     *
     * @param expression 要解析的限定表达式
     * @param scope 解析的词法作用域
     * @param context 绑定上下文
     * @return 解析结果
     */
    fun resolveClassOrPackageInQualifiedExpression(
        expression: CjQualifiedExpression,
        scope: LexicalScope,
        context: BindingContext
    ): QualifiedExpressionResolveResult {
        return expressionQualifierResolver.resolveClassOrPackage(
            expression,
            scope,
            context,
            scope.ownerDescriptor.module
        )
    }

    /**
     * 解析表达式中的限定符并展开调用链
     *
     * 在表达式位置解析限定名称，识别哪部分是类型限定符，哪部分是成员调用链。
     *
     * @param expression 限定表达式
     * @param context 表达式类型推导上下文
     * @param isValue 判断简单名称表达式是否表示值的谓词
     * @return 调用表达式元素列表
     */
    fun resolveQualifierInExpressionAndUnroll(
        expression: CjQualifiedExpression,
        context: ExpressionTypingContext,
        isValue: (CjSimpleNameExpression) -> Boolean
    ): List<CallExpressionElement> {
        return expressionQualifierResolver.resolveAndUnroll(expression, context, isValue)
    }

    /**
     * 为诊断目的解析简单名称表达式为限定符接收器
     *
     * 在表达式位置尝试将简单名称解析为限定符（包、类、枚举类等），
     * 用于提供代码补全、导航等 IDE 功能。
     *
     * @param expression 简单名称表达式
     * @param receiver 接收器（如果有）
     * @param context 表达式类型推导上下文
     * @return 限定符接收器，如果无法解析为限定符则返回 null
     */
    fun resolveNameExpressionAsQualifierForDiagnostics(
        expression: CjSimpleNameExpression,
        receiver: Receiver?,
        context: ExpressionTypingContext
    ): QualifierReceiver? {
        return expressionQualifierResolver.resolveNameExpressionAsQualifier(expression, receiver, context)
    }
}
