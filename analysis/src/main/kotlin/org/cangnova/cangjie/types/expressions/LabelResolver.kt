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

import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.descriptors.ReceiverParameterDescriptor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.parents
import org.cangnova.cangjie.resolve.DescriptorResolver
import org.cangnova.cangjie.resolve.calls.context.ResolutionContext
import org.cangnova.cangjie.resolve.scopes.getDeclarationsByLabel
import org.cangnova.cangjie.utils.addIfNotNull
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.descriptors.DescriptorToSourceUtils
import org.cangnova.cangjie.diagnostics.infos.errors.UNRESOLVED_REFERENCE
import org.cangnova.cangjie.diagnostics.infos.warnings.LABEL_NAME_CLASH
import org.cangnova.cangjie.diagnostics.infos.warnings.LABEL_RESOLVE_WILL_CHANGE
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.DECLARATION_TO_DESCRIPTOR
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.DESCRIPTOR_TO_CONTEXT_RECEIVER_MAP
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.LABEL_TARGET
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.REFERENCE_TARGET
import org.cangnova.cangjie.resolve.binding.BindingContextUtils
import org.cangnova.cangjie.resolve.binding.BindingTrace

/**
 * 标签解析器
 *
 * 负责解析仓颉语言中的标签引用，主要用于处理 `this@label` 和 `super@label` 表达式。
 * 标签用于在嵌套的类、函数或 lambda 表达式中明确指定接收者。
 *
 * ## 主要功能
 *
 * ### 1. 标签名称收集
 * 收集 PSI 元素（如函数、lambda、类）可以作为标签引用的名称：
 * - 函数名称：`fun foo() { ... }` 可以用 `this@foo` 引用
 * - Lambda 调用者名称：`list.forEach { this@forEach }`
 * - 类名称：`class Foo { fun bar() { this@Foo } }`
 *
 * ### 2. 标签解析
 * 解析 `this@label` 或 `super@label` 表达式，确定标签指向的接收者：
 * - 查找作用域中匹配标签名的声明
 * - 处理标签名冲突和歧义
 * - 记录标签目标到绑定上下文
 *
 * ## 使用示例
 *
 * ```cangjie
 * class Outer {
 *     fun outer() {
 *         val inner = object {
 *             fun inner() {
 *                 // this@Outer 引用外部类实例
 *                 // this@outer 引用外部函数的扩展接收者（如果有）
 *             }
 *         }
 *     }
 * }
 *
 * fun example() {
 *     listOf(1, 2, 3).forEach {
 *         // return@forEach 返回到 forEach 调用
 *         // this@example 引用 example 函数的接收者
 *     }
 * }
 * ```
 *
 * ## 解析流程
 *
 * 1. 从标签表达式开始向上遍历 PSI 树
 * 2. 收集每个层级可用的标签名称
 * 3. 在作用域中查找匹配的声明描述符
 * 4. 验证解析结果并报告诊断信息（如歧义、未解析引用等）
 *
 * @see LabeledReceiverResolutionResult 标签解析结果
 * @see ResolutionContext 解析上下文
 */
object LabelResolver {
    /**
     * 获取函数表达式的标签名称
     *
     * 根据表达式的上下文确定可用的标签名称：
     * - 二元表达式：使用操作符名称
     * - 其他情况：使用调用者名称
     *
     * @param element 函数表达式（lambda 或匿名函数）
     * @return 标签名称，如果无法确定则返回 null
     */
    private fun getLabelForFunctionalExpression(element: CjExpression): Name? {
        return when (val parent = element.parent) {
//            is CjLabeledExpression -> getLabelNamesIfAny(parent, false).singleOrNull()
            is CjBinaryExpression -> parent.operationReference.referencedNameAsName
            else -> getCallerName(element)
        }
    }

    /**
     * 获取包含指定表达式的调用表达式
     *
     * 查找表达式所在的调用上下文，用于确定 lambda 的调用者名称。
     *
     * @param expression 要查找的表达式
     * @return 包含该表达式的调用表达式，如果不在调用上下文中则返回 null
     */
    private fun getContainingCallExpression(expression: CjExpression): CjCallExpression? {
        val parent = expression.parent
        if (parent is CjLambdaArgument) {
            // f {}
            val call = parent.parent
            if (call is CjCallExpression) {
                return call
            }
        }

        if (parent is CjValueArgument) {
            // f ({}) or f(p = {}) or f (fun () {})
            val argList = parent.parent ?: return null
            val call = argList.parent
            if (call is CjCallExpression) {
                return call
            }
        }
        return null
    }

    /**
     * 获取调用者名称
     *
     * 从调用表达式中提取被调用函数的名称，用作 lambda 的隐式标签。
     *
     * @param expression 位于调用参数中的表达式
     * @return 调用者名称，如果无法确定则返回 null
     */
    private fun getCallerName(expression: CjExpression): Name? {
        val callExpression = getContainingCallExpression(expression) ?: return null
        val calleeExpression = callExpression.calleeExpression as? CjSimpleNameExpression
        return calleeExpression?.referencedNameAsName

    }

    /**
     * 获取 PSI 元素的所有可用标签名称
     *
     * 根据元素类型收集可以作为标签引用的名称列表：
     * - 函数字面量：递归获取父元素的标签
     * - Lambda 表达式：使用调用者名称
     * - 类：包含上下文接收者名称
     * - 命名函数：使用函数名，可选包含扩展接收者类型名
     * - 属性访问器：使用属性名
     *
     * @param element 要获取标签名称的 PSI 元素
     * @param addClassNameLabels 是否添加类名相关的标签（扩展接收者、上下文接收者）
     * @return 可用标签名称列表
     */
    fun getLabelNamesIfAny(element: PsiElement, addClassNameLabels: Boolean): List<Name> {
        val result = mutableListOf<Name>()
        when (element) {
//            is CjLabeledExpression -> result.addIfNotNull(element.getLabelNameAsName())
            // TODO: Support context receivers in function literals
            is CjFunctionLiteral -> return getLabelNamesIfAny(element.parent!!, false)
            is CjLambdaExpression -> result.addIfNotNull(getLabelForFunctionalExpression(element))
        }

        val functionOrProperty = when (element) {
            is CjNamedFunction -> {
                result.addIfNotNull(element.nameAsName ?: getLabelForFunctionalExpression(element))
                element
            }

            is CjPropertyAccessor -> element.property
            else -> return result
        }
        return result
    }

    /**
     * 根据标签名称获取匹配的 PSI 元素
     *
     * 从标签表达式向上遍历 PSI 树，收集所有匹配指定标签名的元素。
     *
     * @param labelName 要查找的标签名称
     * @param labelExpression 标签表达式
     * @param classNameLabelsEnabled 是否启用类名标签
     * @return 匹配元素集合和类型化元素（如果有扩展接收者匹配）
     */
    private fun getElementsByLabelName(
        labelName: Name,
        labelExpression: CjSimpleNameExpression,
        classNameLabelsEnabled: Boolean
    ): Pair<LinkedHashSet<CjElement>, CjCallableDeclaration?> {
        val elements = linkedSetOf<CjElement>()
        var typedElement: CjCallableDeclaration? = null
        var parent: PsiElement? = labelExpression.parent
        while (parent != null) {
            val names = getLabelNamesIfAny(parent, classNameLabelsEnabled)
            if (names.contains(labelName)) {
                elements.add(getExpressionUnderLabel(parent as CjExpression))
            }
            parent = if (parent is CjCodeFragment) parent.context else parent.parent
        }
        return elements to typedElement
    }

    /**
     * 获取标签下的实际表达式
     *
     * 去除括号包装，如果是 lambda 表达式则返回函数字面量。
     *
     * @param labeledExpression 带标签的表达式
     * @return 实际的表达式
     */
    private fun getExpressionUnderLabel(labeledExpression: CjExpression): CjExpression {
        val expression = CjPsiUtil.safeDeparenthesize(labeledExpression)
        return if (expression is CjLambdaExpression) expression.functionLiteral else expression
    }

    /**
     * 解析 this@label 或 super@label 表达式
     *
     * 这是标签解析的核心方法，负责：
     * 1. 在作用域中查找匹配标签名的声明
     * 2. 确定接收者参数描述符
     * 3. 记录绑定信息到 trace
     * 4. 报告诊断信息（歧义、未解析引用等）
     *
     * ## 解析逻辑
     *
     * - **找到唯一匹配**：返回成功结果，记录标签目标和引用目标
     * - **未找到匹配**：尝试从元素标签中解析，处理上下文接收者
     * - **找到多个匹配**：报告歧义错误
     *
     * @param expression this 或 super 表达式
     * @param context 解析上下文
     * @param labelName 标签名称
     * @return 标签解析结果，包含成功/失败状态和接收者描述符
     */
    fun resolveThisOrSuperLabel(
        expression: CjInstanceExpressionWithLabel,
        context: ResolutionContext<*>,
        labelName: Name
    ): LabeledReceiverResolutionResult {
        val referenceExpression = expression.instanceReference
        val targetLabelExpression = expression.getTargetLabel() ?: error(expression)

        val scope = context.scope
        val declarationsByLabel = scope.getDeclarationsByLabel(labelName)
        val (elementsByLabel, typedElement) = getElementsByLabelName(
            labelName, targetLabelExpression,
            classNameLabelsEnabled = false
        )
        val trace = context.trace
        when (declarationsByLabel.size) {
            1 -> {
                val declarationDescriptor = declarationsByLabel.single()
                val thisReceiver = when (declarationDescriptor) {
                    is ClassDescriptor -> declarationDescriptor.thisAsReceiverParameter
                    is FunctionDescriptor -> declarationDescriptor.dispatchReceiverParameter
//                    is PropertyDescriptor -> declarationDescriptor.dispatchReceiverParameter
                    else -> throw UnsupportedOperationException("Unsupported descriptor: $declarationDescriptor") // TODO
                }

                val declarationElement = DescriptorToSourceUtils.descriptorToDeclaration(declarationDescriptor)
                    ?: error("No PSI element for descriptor: $declarationDescriptor")
                trace.record(LABEL_TARGET, targetLabelExpression, declarationElement)
                trace.record(REFERENCE_TARGET, referenceExpression, declarationDescriptor)
                val closestElement = elementsByLabel.firstOrNull()
                if (closestElement != null && declarationElement in closestElement.parents) {
                    reportLabelResolveWillChange(
                        trace, targetLabelExpression, declarationElement, closestElement, isForExtensionReceiver = false
                    )
                } else if (typedElement != null && declarationElement in typedElement.parents) {
                    reportLabelResolveWillChange(
                        trace, targetLabelExpression, declarationElement, typedElement, isForExtensionReceiver = true
                    )
                }

                if (declarationDescriptor is ClassDescriptor) {
                    if (!DescriptorResolver.checkHasOuterClassInstance(
                            scope, trace, targetLabelExpression, declarationDescriptor
                        )
                    ) {
                        return LabeledReceiverResolutionResult.labelResolutionFailed()
                    }
                }

                return LabeledReceiverResolutionResult.labelResolutionSuccess(thisReceiver)
            }

            0 -> {
                if (elementsByLabel.size > 1) {
                    trace.report(LABEL_NAME_CLASH.on(targetLabelExpression))
                }
                val element = elementsByLabel.firstOrNull()?.also {
                    trace.record(LABEL_TARGET, targetLabelExpression, it)
                }
                val declarationDescriptor =  element?.let { trace.bindingContext[DECLARATION_TO_DESCRIPTOR, element] }
                if (declarationDescriptor is FunctionDescriptor || declarationDescriptor is ClassDescriptor) {
                    val thisReceiver = (declarationDescriptor as? FunctionDescriptor)?.dispatchReceiverParameter?.also {
                        trace.record(LABEL_TARGET, targetLabelExpression, element)
                        trace.record(REFERENCE_TARGET, referenceExpression, declarationDescriptor)
                    }
                    return LabeledReceiverResolutionResult.labelResolutionSuccess(thisReceiver)
                } else {
                    trace.report(UNRESOLVED_REFERENCE.on(targetLabelExpression, targetLabelExpression))
                }
            }

            else -> BindingContextUtils.reportAmbiguousLabel(trace, targetLabelExpression, declarationsByLabel)
        }
        return LabeledReceiverResolutionResult.labelResolutionFailed()
    }

    /**
     * 报告标签解析将要改变的警告
     *
     * 当标签解析结果可能在未来版本中改变时，报告警告信息。
     * 这通常发生在存在更近的同名标签时。
     *
     * @param trace 绑定追踪器
     * @param target 标签表达式
     * @param declarationElement 当前解析到的声明元素
     * @param closestElement 更近的同名元素
     * @param isForExtensionReceiver 是否是扩展接收者
     */
    private fun reportLabelResolveWillChange(
        trace: BindingTrace,
        target: CjSimpleNameExpression,
        declarationElement: PsiElement,
        closestElement: CjElement,
        isForExtensionReceiver: Boolean
    ) {
        fun suffix() = if (isForExtensionReceiver) "extension receiver" else "context receiver"

        val closestDescription = when (closestElement) {
            is CjFunctionLiteral -> "anonymous function"
            is CjNamedFunction -> "function ${closestElement.name} ${suffix()}"
            is CjPropertyAccessor -> "property ${closestElement.property.name} ${suffix()}"
            else -> "???"
        }
        val declarationDescription = when (declarationElement) {
            is CjClass -> "class ${declarationElement.name}"
            is CjNamedFunction -> "function ${declarationElement.name}"
            is CjProperty -> "property ${declarationElement.name}"
            is CjNamedDeclaration -> "declaration with name ${declarationElement.name}"
            else -> "unknown declaration"
        }
        trace.report(LABEL_RESOLVE_WILL_CHANGE.on(target, declarationDescription, closestDescription))
    }

    /**
     * 标签接收者解析结果
     *
     * 封装标签解析的结果，包含解析状态码和可能的接收者参数描述符。
     *
     * ## 结果状态
     *
     * - [Code.SUCCESS]：解析成功，找到了有效的接收者
     * - [Code.NO_THIS]：解析成功但没有 this 接收者（如静态上下文）
     * - [Code.LABEL_RESOLUTION_ERROR]：解析失败（未找到、歧义等）
     *
     * @property code 解析结果状态码
     * @property receiverParameterDescriptor 接收者参数描述符（仅在成功时有效）
     */
    class LabeledReceiverResolutionResult private constructor(
        val code: Code,
        private val receiverParameterDescriptor: ReceiverParameterDescriptor?
    ) {
        /**
         * 解析结果状态码
         */
        enum class Code {
            /** 标签解析错误（未找到、歧义等） */
            LABEL_RESOLUTION_ERROR,
            /** 解析成功但没有 this 接收者 */
            NO_THIS,
            /** 解析成功，找到有效接收者 */
            SUCCESS
        }

        /**
         * 检查解析是否成功
         *
         * @return 如果解析成功返回 true
         */
        fun success(): Boolean {
            return code == Code.SUCCESS
        }

        /**
         * 获取接收者参数描述符
         *
         * 仅在解析成功时调用，否则会抛出断言错误。
         *
         * @return 接收者参数描述符，如果是 NO_THIS 状态则返回 null
         * @throws AssertionError 如果解析未成功
         */
        fun getReceiverParameterDescriptor(): ReceiverParameterDescriptor? {
            assert(success()) { "Don't try to obtain the receiver when resolution failed with $code" }
            return receiverParameterDescriptor
        }

        companion object {
            /**
             * 创建成功的解析结果
             *
             * @param receiverParameterDescriptor 接收者参数描述符，如果为 null 则返回 NO_THIS 状态
             * @return 解析结果
             */
            fun labelResolutionSuccess(receiverParameterDescriptor: ReceiverParameterDescriptor?): LabeledReceiverResolutionResult {
                if (receiverParameterDescriptor == null) {
                    return LabeledReceiverResolutionResult(Code.NO_THIS, null)
                }
                return LabeledReceiverResolutionResult(Code.SUCCESS, receiverParameterDescriptor)
            }

            /**
             * 创建失败的解析结果
             *
             * @return 表示解析失败的结果
             */
            fun labelResolutionFailed(): LabeledReceiverResolutionResult {
                return LabeledReceiverResolutionResult(Code.LABEL_RESOLUTION_ERROR, null)
            }
        }
    }
}
