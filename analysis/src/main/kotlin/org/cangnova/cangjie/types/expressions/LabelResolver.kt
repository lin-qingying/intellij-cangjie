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

    fun getLabelNamesIfAny(element: PsiElement, addClassNameLabels: Boolean): List<Name> {
        val result = mutableListOf<Name>()
        when (element) {
//            is CjLabeledExpression -> result.addIfNotNull(element.getLabelNameAsName())
            // TODO: Support context receivers in function literals
            is CjFunctionLiteral -> return getLabelNamesIfAny(element.parent!!, false)
            is CjLambdaExpression -> result.addIfNotNull(getLabelForFunctionalExpression(element))
        }

        if (element is CjClass) {
            element.contextReceivers
                .mapNotNullTo(result) { it.name()?.let { s -> Name.identifier(s) } }
        }

        val functionOrProperty = when (element) {
            is CjNamedFunction -> {
                result.addIfNotNull(element.nameAsName ?: getLabelForFunctionalExpression(element))
                element
            }

            is CjPropertyAccessor -> element.property
            else -> return result
        }
        if (addClassNameLabels) {
            functionOrProperty.receiverTypeReference?.nameForReceiverLabel()?.let { result.add(Name.identifier(it)) }
            functionOrProperty.contextReceivers
                .mapNotNullTo(result) { it.name()?.let { s -> Name.identifier(s) } }
        }
        return result
    }

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
            } else if (parent is CjCallableDeclaration && typedElement == null) {
                val receiverTypeReference = parent.receiverTypeReference
                val nameForReceiverLabel = receiverTypeReference?.nameForReceiverLabel()
                if (nameForReceiverLabel == labelName.asString()) {
                    typedElement = parent
                }
            }
            parent = if (parent is CjCodeFragment) parent.context else parent.parent
        }
        return elements to typedElement
    }

    private fun getExpressionUnderLabel(labeledExpression: CjExpression): CjExpression {
        val expression = CjPsiUtil.safeDeparenthesize(labeledExpression)
        return if (expression is CjLambdaExpression) expression.functionLiteral else expression
    }

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
                    is FunctionDescriptor -> declarationDescriptor.extensionReceiverParameter
//                    is PropertyDescriptor -> declarationDescriptor.extensionReceiverParameter
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
                    val labelNameToReceiverMap = trace.bindingContext[
                        DESCRIPTOR_TO_CONTEXT_RECEIVER_MAP,
                        /* if (declarationDescriptor is PropertyAccessorDescriptor) declarationDescriptor.correspondingProperty else */declarationDescriptor
                    ]
                    val thisReceivers = labelNameToReceiverMap?.get(labelName.identifier)
                    val thisReceiver = when {
                        thisReceivers.isNullOrEmpty() ->
                            (declarationDescriptor as? FunctionDescriptor)?.extensionReceiverParameter

                        thisReceivers.size == 1 -> thisReceivers.single()
                        else -> {
                            BindingContextUtils.reportAmbiguousLabel(trace, targetLabelExpression, declarationsByLabel)
                            return LabeledReceiverResolutionResult.labelResolutionFailed()
                        }
                    }?.also {
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

    class LabeledReceiverResolutionResult private constructor(
        val code: Code,
        private val receiverParameterDescriptor: ReceiverParameterDescriptor?
    ) {
        enum class Code {
            LABEL_RESOLUTION_ERROR,
            NO_THIS,
            SUCCESS
        }

        fun success(): Boolean {
            return code == Code.SUCCESS
        }

        fun getReceiverParameterDescriptor(): ReceiverParameterDescriptor? {
            assert(success()) { "Don't try to obtain the receiver when resolution failed with $code" }
            return receiverParameterDescriptor
        }

        companion object {
            fun labelResolutionSuccess(receiverParameterDescriptor: ReceiverParameterDescriptor?): LabeledReceiverResolutionResult {
                if (receiverParameterDescriptor == null) {
                    return LabeledReceiverResolutionResult(Code.NO_THIS, null)
                }
                return LabeledReceiverResolutionResult(Code.SUCCESS, receiverParameterDescriptor)
            }

            fun labelResolutionFailed(): LabeledReceiverResolutionResult {
                return LabeledReceiverResolutionResult(Code.LABEL_RESOLUTION_ERROR, null)
            }
        }
    }
}
