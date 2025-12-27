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

package org.cangnova.cangjie.utils

import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.DescriptorToSourceUtils
import org.cangnova.cangjie.descriptors.ReceiverParameterDescriptor
import org.cangnova.cangjie.diagnostics.rendering.IdeDescriptorRenderers
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.psiUtil.findLabelAndCall
import org.cangnova.cangjie.renderer.render
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.getImplicitReceiversHierarchy

/**
 * 获取词法作用域中的所有隐式接收器（带实例）
 *
 * 隐式接收器是指在当前作用域中可以不使用限定符直接访问成员的对象。
 * 仓颉语言中的隐式接收器包括：
 * - 类成员函数中的 `this`
 * - extend 块中的 `this`
 * - lambda 表达式中的 `this`（如果 lambda 有接收器）
 *
 * @receiver LexicalScope 词法作用域
 * @return Collection<ReceiverParameterDescriptor> 所有隐式接收器的描述符列表
 */
fun LexicalScope.getImplicitReceiversWithInstance(): Collection<ReceiverParameterDescriptor> {
    return getImplicitReceiversWithInstanceToExpression().keys
}


/**
 * 接收器表达式工厂接口
 *
 * 用于创建访问隐式接收器的表达式。
 * 例如，创建 `this`、`this@ClassName` 等表达式。
 */
interface ReceiverExpressionFactory {
    /**
     * 是否为直接的 `this` 表达式
     *
     * true 表示可以使用简短的 `this`，false 表示需要使用带标签的 `this@Label`
     */
    val isImmediate: Boolean

    /**
     * 表达式的文本形式
     *
     * 例如："this"、"this@ClassName"、"this@lambda"
     */
    val expressionText: String

    /**
     * 创建接收器表达式的 PSI 元素
     *
     * @param psiFactory PSI 工厂，用于创建表达式元素
     * @param shortThis 是否使用简短的 `this`（仅当 [isImmediate] 为 true 时有效）
     * @return CjExpression 创建的表达式 PSI 元素
     */
    fun createExpression(psiFactory: CjPsiFactory, shortThis: Boolean = true): CjExpression
}

/**
 * 获取词法作用域中的隐式接收器及其对应的表达式工厂
 *
 * 此函数返回一个映射，键为接收器描述符，值为创建访问该接收器表达式的工厂。
 * 用于代码补全、自动导入等 IDE 功能。
 *
 * 示例：
 * ```kotlin
 * class Foo {
 *     fun bar() {
 *         // 在这里，隐式接收器包括：
 *         // - this@Foo -> 表达式为 "this"
 *     }
 * }
 * ```
 *
 * @receiver LexicalScope 词法作用域
 * @return Map<ReceiverParameterDescriptor, ReceiverExpressionFactory?> 接收器到表达式工厂的映射
 * - 键：接收器描述符
 * - 值：表达式工厂（如果接收器可以通过表达式访问），或 null（如果无法直接访问）
 */
fun LexicalScope.getImplicitReceiversWithInstanceToExpression(

): Map<ReceiverParameterDescriptor, ReceiverExpressionFactory?> {
    val allReceivers = getImplicitReceiversHierarchy()

    val receivers = allReceivers
    val outerDeclarationsWithInstance = LinkedHashSet<DeclarationDescriptor>()
    var current: DeclarationDescriptor? = ownerDescriptor
    while (current != null) {
        // 注释：仓颉没有属性访问器的概念，不需要处理 PropertyAccessorDescriptor
        // if (current is PropertyAccessorDescriptor) {
        //     current = current.correspondingProperty
        // }
        outerDeclarationsWithInstance.add(current)

        val classDescriptor = current as? ClassDescriptor
        // 遇到非本地类时停止，因为外部类的实例不在当前作用域中
        if (classDescriptor != null && !DescriptorUtils.isLocal(classDescriptor)) break

        current = current.containingDeclaration
    }

    val result = LinkedHashMap<ReceiverParameterDescriptor, ReceiverExpressionFactory?>()
    for ((index, receiver) in receivers.withIndex()) {
        val owner = receiver.containingDeclaration


        val (expressionText, isImmediateThis) = when {
            owner in outerDeclarationsWithInstance -> {
                val thisWithLabel = getThisQualifierName(receiver)?.let { "this@${it.render()}" } //extended
                when (index) {
                    0 -> (thisWithLabel ?: "this") to true  // 最内层的接收器可以使用简短的 this
                    else -> thisWithLabel to false           // 外层接收器需要使用带标签的 this
                }

            }



            else -> continue  // 跳过无法访问的接收器
        }

        result[receiver] =
            if (expressionText != null)
                createReceiverExpressionFactory(expressionText, isImmediateThis)
            else null
    }

    return result
}

/**
 * 获取接收器的 `this` 限定符名称
 *
 * 用于生成带标签的 `this` 表达式，例如 `this@ClassName`、`this@lambda`。
 *
 * @param receiver 接收器参数描述符
 * @return Name? 限定符名称，如果无法确定则返回 null
 * - 对于命名的声明（类、函数等），返回其名称
 * - 对于匿名函数（lambda），查找其标签
 */
private fun getThisQualifierName(receiver: ReceiverParameterDescriptor): Name? {
    val descriptor = receiver.containingDeclaration
    val name = descriptor.name
    if (!name.isSpecial) {
        return name
    }

    // 对于 lambda 表达式，尝试查找其标签
    val functionLiteral = DescriptorToSourceUtils.descriptorToDeclaration(descriptor) as? CjFunctionLiteral
    return functionLiteral?.findLabelAndCall()?.first
}

/**
 * 获取被 DSL 标记遮蔽的接收器参数
 *
 * 仓颉语言的 DSL 标记机制：
 * - 标记相同 DSL 作用域的多个接收器
 * - 在每个 DSL 作用域中，只有最内层的接收器可以直接访问，其他接收器被遮蔽
 * - 被遮蔽的接收器必须使用显式的 `this@Label` 访问
 *
 * 注意：当前实现已注释掉，因为仓颉语言可能尚未完全实现 DSL 标记功能。
 *
 * @param receiverParameters 接收器参数列表（从内到外排序）
 * @return Set<ReceiverParameterDescriptor> 被遮蔽的接收器集合
 */
private fun getParametersShadowedByDslMarkers(receiverParameters: List<ReceiverParameterDescriptor>): Set<ReceiverParameterDescriptor> {
    val typesByDslScopes = mutableMapOf<FqName, MutableList<ReceiverParameterDescriptor>>()

    // 仓颉语言的 DSL 标记支持尚未实现
//    for (receiverParameter in receiverParameters) {
//        val dslMarkers = DslMarkerUtils.extractDslMarkerFqNames(receiverParameter.value).all()
//        for (marker in dslMarkers) {
//            typesByDslScopes.getOrPut(marker) { mutableListOf() } += receiverParameter
//        }
//    }

    // 对于每个 DSL 标记，除了最内层的接收器，其他接收器都被遮蔽
    return typesByDslScopes.values.flatMapTo(mutableSetOf()) { it.drop(1) }
}

/**
 * 创建接收器表达式工厂
 *
 * 工厂方法，用于创建 [ReceiverExpressionFactory] 实例。
 *
 * @param expressionText 表达式的文本形式（例如 "this" 或 "this@ClassName"）
 * @param isImmediateThis 是否为直接的 `this` 表达式
 * @return ReceiverExpressionFactory 接收器表达式工厂实例
 */
private fun createReceiverExpressionFactory(
    expressionText: String,
    isImmediateThis: Boolean
): ReceiverExpressionFactory {
    return object : ReceiverExpressionFactory {
        override val isImmediate = isImmediateThis
        override val expressionText: String get() = expressionText
        override fun createExpression(psiFactory: CjPsiFactory, shortThis: Boolean): CjExpression {
            return psiFactory.createExpression(if (shortThis && isImmediateThis) "this" else expressionText)
        }
    }
}
