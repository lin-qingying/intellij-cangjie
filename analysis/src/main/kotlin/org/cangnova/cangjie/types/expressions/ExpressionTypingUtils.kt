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

package org.cangnova.cangjie.types.expressions

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.impl.AnonymousFunctionDescriptor
import org.cangnova.cangjie.descriptors.impl.FunctionExpressionDescriptor
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.OverloadChecker
import org.cangnova.cangjie.resolve.scopes.*
import org.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver
import org.cangnova.cangjie.types.CangJieType
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.diagnostics.infos.warnings.NAME_SHADOWING
import org.cangnova.cangjie.name.OperatorConventions
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.PROCESSED
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo

/**
 * 表达式类型检查工具类
 *
 * 提供表达式类型检查过程中常用的工具方法。
 *
 * ## 主要功能
 *
 * ### 1. 表达式判断
 * - [isExclExclExpression] - 判断是否为 !! 表达式
 * - [dependsOnExpectedType] - 判断表达式是否依赖期望类型
 * - [isBinaryExpressionDependentOnExpectedType] - 判断二元表达式是否依赖期望类型
 *
 * ### 2. 接收者创建
 * - [getExpressionReceiver] - 获取表达式接收者
 * - [safeGetExpressionReceiver] - 安全获取表达式接收者
 *
 * ### 3. 类型信息处理
 * - [getTypeInfoOrNullType] - 获取类型信息或空类型
 * - [safeGetType] - 安全获取类型
 *
 * ### 4. 作用域管理
 * - [newWritableScopeImpl] - 创建新的可写作用域
 * - [checkVariableShadowing] - 检查变量遮蔽
 *
 * ### 5. 声明判断
 * - [isLocal] - 判断是否为局部声明
 * - [isFunctionExpression] - 判断是否为函数表达式
 * - [isFunctionLiteral] - 判断是否为函数字面量
 *
 * ### 6. 测试辅助
 * - [createFakeExpressionOfType] - 创建指定类型的假表达式
 *
 * ## 使用示例
 *
 * ### 判断表达式依赖性
 *
 * ```kotlin
 * val expression: CjExpression = // ...
 * if (ExpressionTypingUtils.dependsOnExpectedType(expression)) {
 *     // 需要在有期望类型的上下文中处理
 * }
 * ```
 *
 * ### 创建表达式接收者
 *
 * ```kotlin
 * val receiver = ExpressionTypingUtils.getExpressionReceiver(
 *     facade,
 *     expression,
 *     context
 * )
 * if (receiver != null) {
 *     // 使用接收者进行调用解析
 * }
 * ```
 *
 * ### 检查变量遮蔽
 *
 * ```kotlin
 * ExpressionTypingUtils.checkVariableShadowing(
 *     scope,
 *     trace,
 *     variableDescriptor
 * )
 * ```
 *
 * ### 判断局部声明
 *
 * ```kotlin
 * if (ExpressionTypingUtils.isLocal(currentLocality, candidate)) {
 *     // 局部扩展函数优先于成员函数
 * }
 * ```
 */
object ExpressionTypingUtils {

    /**
     * 判断表达式是否为 !! 表达式
     *
     * !! 操作符用于强制解包可选类型，如果值为 null 则抛出异常。
     *
     * ## 示例
     *
     * ```kotlin
     * val x: Int? = getValue()
     * val y = x!!  // 如果 x 为 null，抛出 NullPointerException
     * ```
     *
     * @param expression 要检查的表达式
     * @return 如果是 !! 表达式则返回 true
     */
    
    fun isExclExclExpression(expression: CjExpression?): Boolean {
        return expression is CjUnaryExpression
        // TODO: 需要检查操作符类型是否为 EXCLEXCL
        // && expression.operationReference.referencedNameElementType == CjTokens.EXCLEXCL
    }

    /**
     * 获取表达式接收者
     *
     * 从表达式创建 [ExpressionReceiver]，用于调用解析。
     *
     * ## 返回值
     *
     * - 如果表达式类型为 null，返回 null
     * - 否则创建包含表达式和类型的接收者
     *
     * ## 使用场景
     *
     * 在解析成员调用或扩展调用时，需要将表达式转换为接收者：
     * ```kotlin
     * receiver.foo()  // receiver 是表达式接收者
     * ```
     *
     * @param facade 表达式类型检查门面
     * @param expression 表达式
     * @param context 类型检查上下文
     * @return 表达式接收者，如果表达式类型为 null 则返回 null
     */
    
    fun getExpressionReceiver(
        facade: ExpressionTypingFacade,
        expression: CjExpression,
        context: ExpressionTypingContext
    ): ExpressionReceiver? {
        val type = facade.getTypeInfo(expression, context).type ?: return null
        return ExpressionReceiver.create(expression, type, context.trace.bindingContext)
    }

    /**
     * 创建指定类型的假表达式
     *
     * 用于测试或内部类型检查，创建一个具有指定名称和类型的假表达式。
     *
     * ## 使用场景
     *
     * - 单元测试中创建测试数据
     * - 内部类型推导时需要占位表达式
     * - 错误恢复时创建默认表达式
     *
     * ## 副作用
     *
     * - 将表达式的类型记录到 trace 中
     * - 将表达式标记为已处理（PROCESSED）
     *
     * @param project 项目实例
     * @param trace 绑定跟踪器
     * @param argumentName 表达式的名称
     * @param argumentType 表达式的类型
     * @return 创建的假表达式
     */
    
    fun createFakeExpressionOfType(
        project: Project,
        trace: BindingTrace,
        argumentName: String,
        argumentType: CangJieType
    ): CjExpression {
        val fakeExpression = CjPsiFactory(project, markGenerated = false).createExpression(argumentName)
        trace.recordType(fakeExpression, argumentType)
        trace.record(PROCESSED, fakeExpression)
        return fakeExpression
    }

    /**
     * 获取类型信息或空类型
     *
     * 如果表达式为 null，返回包含空数据流信息的类型信息。
     *
     * ## 行为
     *
     * - 表达式非 null：返回表达式的实际类型信息
     * - 表达式为 null：返回 [noTypeInfo]，包含当前上下文的数据流信息
     *
     * ## 使用场景
     *
     * 处理可选表达式时的默认值：
     * ```kotlin
     * val typeInfo = getTypeInfoOrNullType(
     *     expression?.condition,
     *     context,
     *     facade
     * )
     * ```
     *
     * @param expression 表达式，可以为 null
     * @param context 类型检查上下文
     * @param facade 表达式类型检查内部接口
     * @return 类型信息，永远不为 null
     */
    
    fun getTypeInfoOrNullType(
        expression: CjExpression?,
        context: ExpressionTypingContext,
        facade: ExpressionTypingInternals
    ): CangJieTypeInfo {
        return expression?.let { facade.getTypeInfo(it, context) } ?: noTypeInfo(context)
    }

    /**
     * 创建新的可写作用域
     *
     * 基于当前上下文创建新的 [LexicalWritableScope]，用于声明新的变量或函数。
     *
     * ## 作用域配置
     *
     * - **父作用域**: 使用上下文的当前作用域
     * - **所有者**: 使用父作用域的所有者
     * - **继承**: 不继承父作用域的声明（`false`）
     * - **重声明检查**: 使用基于 trace 的检查器
     * - **作用域类型**: 由调用者指定
     *
     * ## 使用场景
     *
     * - 函数体作用域
     * - 代码块作用域
     * - 循环作用域
     * - 模式匹配分支作用域
     *
     * ## 示例
     *
     * ```kotlin
     * val blockScope = ExpressionTypingUtils.newWritableScopeImpl(
     *     context,
     *     LexicalScopeKind.CODE_BLOCK,
     *     overloadChecker
     * )
     *
     * // 在新作用域中声明变量
     * blockScope.addVariableDescriptor(variableDescriptor)
     * ```
     *
     * @param context 表达式类型检查上下文
     * @param scopeKind 作用域类型（函数、代码块等）
     * @param overloadChecker 重载检查器，用于检测重声明
     * @return 新创建的可写作用域
     */
    
    fun newWritableScopeImpl(
        context: ExpressionTypingContext,
        scopeKind: LexicalScopeKind,
        overloadChecker: OverloadChecker
    ): LexicalWritableScope {
        return LexicalWritableScope(
            parent = context.scope,
            ownerDescriptor = context.scope.ownerDescriptor,
            isOwnerDescriptorAccessibleByLabel = false,
            redeclarationChecker = TraceBasedLocalRedeclarationChecker(context.trace, overloadChecker),
            kind = scopeKind
        )
    }

    /**
     * 判断表达式是否依赖期望类型
     *
     * 某些表达式的类型推导依赖于上下文的期望类型。
     *
     * ## 依赖期望类型的表达式
     *
     * ### 二元表达式
     * - 标识符操作符（中缀调用）
     * - 操作符重载表达式
     * - 空合并操作符 `??`
     *
     * ### 不依赖期望类型的表达式
     * - 类型转换表达式 `as`、`is`
     *
     * ## 默认行为
     *
     * 对于未明确处理的表达式类型，保守地返回 true（假设依赖期望类型）。
     *
     * ## 示例
     *
     * ```kotlin
     * val x = if (condition) 1 else "str"  // 依赖期望类型判断结果类型
     * val y = 1 + 2                       // 不依赖期望类型，结果总是 Int
     * val z = value as String             // 不依赖期望类型，结果总是 String
     * ```
     *
     * @param expression 要检查的表达式
     * @return 如果表达式依赖期望类型则返回 true
     */
    
    fun dependsOnExpectedType(expression: CjExpression?): Boolean {
        val expr = CjPsiUtil.deparenthesize(expression) ?: return false

        // 类型转换表达式不依赖期望类型
        if (expr is CjBinaryExpressionWithTypeRHS) {
            return false
        }

        // 二元表达式可能依赖期望类型
        if (expr is CjBinaryExpression) {
            return isBinaryExpressionDependentOnExpectedType(expr)
        }

        // TODO: 处理一元表达式
        // if (expr is CjUnaryExpression) {
        //     return isUnaryExpressionDependentOnExpectedType(expr)
        // }

        // 默认假设依赖期望类型
        return true
    }

    /**
     * 判断二元表达式是否依赖期望类型
     *
     * ## 依赖期望类型的二元操作符
     *
     * 1. **标识符操作符**：中缀调用，如 `a foo b`
     * 2. **二元操作符重载**：算术、比较、逻辑操作符
     * 3. **空合并操作符**：`??` 操作符
     *
     * ## 不依赖期望类型的操作符
     *
     * - 赋值操作符 `=`
     * - 范围操作符 `..`
     * - Elvis 操作符 `?:`
     *
     * @param expression 二元表达式
     * @return 如果依赖期望类型则返回 true
     */
    
    @Suppress("DEPRECATION")
    fun isBinaryExpressionDependentOnExpectedType(expression: CjBinaryExpression): Boolean {
        val operationType = expression.operationReference.referencedNameElementType
        return (operationType == CjTokens.IDENTIFIER ||
                OperatorConventions.BINARY_OPERATION_NAMES.containsKey(operationType) ||
                operationType == CjTokens.COALESCING)
    }

    /**
     * 安全获取类型
     *
     * 从 [CangJieTypeInfo] 中获取类型，断言类型不为 null。
     *
     * ## 使用前提
     *
     * 此方法只能用于确保类型存在的场景，如：
     * - 使用 [ExpressionTypingFacade.safeGetTypeInfo] 获取的类型信息
     * - 已知表达式必然有类型的情况
     *
     * ## 错误处理
     *
     * 如果类型为 null，会触发断言错误，表明调用上下文有误。
     *
     * @param typeInfo 类型信息
     * @return 非 null 的类型
     * @throws AssertionError 如果类型为 null
     */
    
    fun safeGetType(typeInfo: CangJieTypeInfo): CangJieType {
        val type = typeInfo.type
        check(type != null) {
            "safeGetType should be invoked on safe CangJieTypeInfo; safeGetTypeInfo should return non-null type"
        }
        return type
    }

    /**
     * 安全获取表达式接收者
     *
     * 从表达式创建接收者，断言表达式有有效类型。
     *
     * ## 与 [getExpressionReceiver] 的区别
     *
     * - [getExpressionReceiver]: 类型为 null 时返回 null
     * - [safeGetExpressionReceiver]: 类型为 null 时抛出异常
     *
     * ## 使用场景
     *
     * 确保表达式必然有类型的情况：
     * ```kotlin
     * val receiver = safeGetExpressionReceiver(facade, expression, context)
     * // receiver 永远不为 null
     * ```
     *
     * @param facade 表达式类型检查门面
     * @param expression 表达式
     * @param context 类型检查上下文
     * @return 非 null 的表达式接收者
     * @throws AssertionError 如果表达式类型为 null
     */
    
    fun safeGetExpressionReceiver(
        facade: ExpressionTypingFacade,
        expression: CjExpression,
        context: ExpressionTypingContext
    ): ExpressionReceiver {
        val type = safeGetType(facade.safeGetTypeInfo(expression, context))
        return ExpressionReceiver.create(expression, type, context.trace.bindingContext)
    }

    // TODO: 未来实现
    // /**
    //  * 判断一元表达式是否依赖期望类型
    //  *
    //  * @param expression 一元表达式
    //  * @return 如果依赖期望类型则返回 true
    //  */
    // 
    // fun isUnaryExpressionDependentOnExpectedType(expression: CjUnaryExpression): Boolean {
    //     return expression.operationReference.referencedNameElementType == CjTokens.EXCLEXCL
    // }

    /**
     * 判断候选声明是否为当前位置的局部声明
     *
     * ## 局部扩展优先级规则
     *
     * 在仓颉语言中，局部声明具有更高的优先级：
     *
     * 1. **局部扩展** > **成员函数** > **非局部扩展**
     *
     * ## 动机
     *
     * 考虑以下场景：
     *
     * ```kotlin
     * class X {
     *     fun x.foo() { ... }  // 局部扩展函数
     *
     *     fun test(x: X) {
     *         x.foo()  // 应该调用局部扩展
     *     }
     * }
     *
     * // 后来 X 类添加了成员函数 foo
     * class X {
     *     fun foo() { ... }  // 新添加的成员函数
     * }
     * ```
     *
     * 为了保持向后兼容性，原有代码中的 `x.foo()` 仍然调用局部扩展函数，
     * 而不是新添加的成员函数。
     *
     * ## 局部性判断
     *
     * 候选声明被认为是局部的，当且仅当：
     * 1. 候选是值参数（总是局部的）
     * 2. 候选的容器是当前位置的某个祖先函数
     *
     * ## 示例
     *
     * ```kotlin
     * fun outer() {
     *     fun middle() {
     *         fun inner() {
     *             // 在 inner 中：
     *             // - outer 的参数和局部变量：局部的
     *             // - middle 的参数和局部变量：局部的
     *             // - inner 的参数和局部变量：局部的
     *             // - 顶层函数：非局部的
     *         }
     *     }
     * }
     * ```
     *
     * @param containerOfTheCurrentLocality 当前位置的容器（函数或类）
     * @param candidate 候选声明
     * @return 如果候选是局部声明则返回 true
     */
    
    fun isLocal(containerOfTheCurrentLocality: DeclarationDescriptor, candidate: DeclarationDescriptor): Boolean {
        // 值参数总是局部的
        if (candidate is ValueParameterDescriptor) {
            return true
        }

        // 检查候选的容器是否是函数
        val parent = candidate.containingDeclaration
        if (parent !is FunctionDescriptor) {
            return false
        }

        // 向上遍历容器链，检查是否找到候选的容器函数
        var current: DeclarationDescriptor? = containerOfTheCurrentLocality
        while (current != null) {
            if (current == parent) {
                return true
            }
            current = current.containingDeclaration
        }

        return false
    }

    /**
     * 检查变量遮蔽
     *
     * 检测新声明的变量是否遮蔽（shadow）了外层作用域的同名变量。
     *
     * ## 变量遮蔽规则
     *
     * ### 报告遮蔽的情况
     *
     * 1. 外层作用域有同名局部变量
     * 2. 新变量在同一个局部上下文中声明
     *
     * ### 不报告遮蔽的情况
     *
     * 1. **非局部变量**: 外层变量不是局部变量
     * 2. **函数参数**: 新变量是普通函数的参数（非 lambda）
     * 3. **不同上下文**: Lambda 参数与外层变量不在同一容器中
     * 4. **重声明**: 解构声明的参数重复时，只报告重声明错误
     *
     * ## 示例
     *
     * ### 报告遮蔽
     *
     * ```kotlin
     * fun test() {
     *     val x = 1
     *     if (condition) {
     *         val x = 2  // 警告: NAME_SHADOWING
     *     }
     * }
     * ```
     *
     * ### Lambda 参数遮蔽
     *
     * ```kotlin
     * fun test() {
     *     val x = 1
     *     list.forEach { x ->  // 警告: NAME_SHADOWING
     *         println(x)
     *     }
     * }
     * ```
     *
     * ### 不报告的情况
     *
     * ```kotlin
     * val x = 1  // 顶层变量
     *
     * fun test(x: Int) {  // 不报告：函数参数不遮蔽顶层变量
     *     val y = x
     * }
     *
     * fun outer() {
     *     val x = 1
     *     fun inner(x: Int) {  // 不报告：嵌套函数的参数
     *         println(x)
     *     }
     * }
     * ```
     *
     * @param scope 当前词法作用域
     * @param trace 绑定跟踪器，用于报告诊断信息
     * @param variableDescriptor 新声明的变量描述符
     */
    
    @Suppress("DEPRECATION")
    fun checkVariableShadowing(
        scope: LexicalScope,
        trace: BindingTrace,
        variableDescriptor: VariableDescriptor
    ) {
        // 查找外层作用域中的同名变量
        val oldDescriptor = scope.findLocalVariable( variableDescriptor.name) ?: return

        // 检查外层变量是否为局部变量
        val variableContainingDeclaration = variableDescriptor.containingDeclaration
        if (!isLocal(variableContainingDeclaration, oldDescriptor)) return

        // 特殊处理函数参数
        if (variableDescriptor is ParameterDescriptor) {
            // 非 lambda 的函数参数不报告遮蔽
            if (!isFunctionLiteral(variableContainingDeclaration)) {
                return
            }

            // Lambda 参数：检查是否在同一容器中
            if (variableContainingDeclaration.containingDeclaration != oldDescriptor.containingDeclaration) {
                return
            }
        }

        // 获取声明的 PSI 元素
        val declaration = DescriptorToSourceUtils.descriptorToDeclaration(variableDescriptor) ?: return

        // 特殊处理解构声明
        if (declaration is CjDestructuringDeclarationEntry &&
            declaration.parent?.parent is CjParameter
        ) {
            // 解构参数中的重复名称：foo { a, (a, b) -> }
            // 对第二个 'a' 不报告 NAME_SHADOWING，因为应该报告 REDECLARATION
            val oldElement = DescriptorToSourceUtils.descriptorToDeclaration(oldDescriptor)

            if (oldElement != null && oldElement.parent == declaration.parent?.parent?.parent) {
                return
            }
        }

        // 报告变量遮蔽警告
        trace.report( NAME_SHADOWING.on(declaration, variableDescriptor.name.asString()))
    }

    /**
     * 判断描述符是否为函数表达式
     *
     * 函数表达式是具有名称的匿名函数。
     *
     * ## 函数表达式示例
     *
     * ```kotlin
     * val f = fun add(x: Int, y: Int): Int {
     *     return x + y
     * }
     * ```
     *
     * @param descriptor 声明描述符
     * @return 如果是函数表达式则返回 true
     */
    
    fun isFunctionExpression(descriptor: DeclarationDescriptor?): Boolean {
        return descriptor is FunctionExpressionDescriptor
    }

    /**
     * 判断描述符是否为函数字面量
     *
     * 函数字面量是 lambda 表达式。
     *
     * ## 函数字面量示例
     *
     * ```kotlin
     * val f = { x: Int, y: Int -> x + y }
     *
     * list.map { it * 2 }
     *
     * list.filter { value ->
     *     value > 0
     * }
     * ```
     *
     * ## 与函数表达式的区别
     *
     * - **函数字面量**：Lambda 语法，无 `fun` 关键字
     * - **函数表达式**：使用 `fun` 关键字的匿名函数
     *
     * @param descriptor 声明描述符
     * @return 如果是函数字面量则返回 true
     */
    
    fun isFunctionLiteral(descriptor: DeclarationDescriptor?): Boolean {
        return descriptor is AnonymousFunctionDescriptor
    }
}
