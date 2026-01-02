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

package org.cangnova.cangjie.resolve.qualified

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.infos.errors.EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE
import org.cangnova.cangjie.diagnostics.infos.errors.EXPRESSION_EXPECTED_PACKAGE_FOUND
import org.cangnova.cangjie.diagnostics.infos.errors.TYPE_PARAMETER_IS_NOT_AN_EXPRESSION
import org.cangnova.cangjie.diagnostics.infos.errors.TYPE_PARAMETER_ON_LHS_OF_DOT
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.scopes.receivers.*
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext



/**
 * 将限定符解析为表达式中的接收器
 *
 * 在限定表达式（如 `a.b.c`）中，将限定符部分（`a.b`）解析为接收器，
 * 用于后续的成员访问或方法调用。
 *
 * 特殊处理：
 * - 类型参数限定符：报告错误，类型参数不能出现在点号左侧
 *
 * @param qualifier 限定符接收器（包、类、类型别名等）
 * @param selector 选择器描述符（成员、构造器等），可能为 null
 * @param context 表达式类型推导上下文
 * @return 解析后的声明描述符
 */
fun resolveQualifierAsReceiverInExpression(
    qualifier: Qualifier, selector: DeclarationDescriptor?, context: ExpressionTypingContext
): DeclarationDescriptor {
    val referenceTarget = resolveQualifierReferenceTarget(qualifier, selector, context)

    // 类型参数不能作为接收器（如 T.foo() 是非法的）
    if (referenceTarget is TypeParameterDescriptor) {
        context.trace.report(TYPE_PARAMETER_ON_LHS_OF_DOT.on(qualifier.referenceExpression, referenceTarget))
    }

    return referenceTarget
}

/**
 * 将限定符解析为独立表达式
 *
 * 当限定符本身作为一个完整的表达式出现时（如 `MyClass`），
 * 需要检查其是否可以作为值使用。
 *
 * 错误检查：
 * - 类型别名：如果不是对象类型，报告需要成员或构造器
 * - 类型参数：报告类型参数不能作为表达式
 * - 普通类：如果没有类值描述符（非对象/枚举），报告错误
 * - 包：报告期望表达式但找到了包
 *
 * @param qualifier 限定符接收器
 * @param context 表达式类型推导上下文
 * @return 解析后的声明描述符
 */
fun resolveQualifierAsStandaloneExpression(
    qualifier: QualifierReceiver, context: ExpressionTypingContext
): DeclarationDescriptor {
    val referenceTarget = resolveQualifierReferenceTarget(qualifier, null, context)

    when (referenceTarget) {
        // 类型别名作为独立表达式
        is TypeAliasDescriptor -> {
            referenceTarget.classDescriptor?.let { classDescriptor ->
                    context.trace.report(
                        EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE.on(
                            qualifier.expression,
                            referenceTarget
                        )
                    )
            }
        }

        // 类型参数不能作为表达式
        is TypeParameterDescriptor -> {
            context.trace.report(TYPE_PARAMETER_IS_NOT_AN_EXPRESSION.on(qualifier.expression, referenceTarget))
        }

        // 普通类作为独立表达式
        is ClassDescriptor -> {
            // 如果不是枚举类型，报告错误（仓颉语言不支持单例对象）
            if (!context.config.isDotEnumGetType && !referenceTarget.isEnum) {
                context.trace.report(
                    EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE.on(
                        qualifier.expression,
                        referenceTarget
                    )
                )
            }
        }

        // 包不能作为表达式
        is PackageViewDescriptor -> {
            context.trace.report(EXPRESSION_EXPECTED_PACKAGE_FOUND.on(qualifier.expression))
        }
    }

    return referenceTarget
}

/**
 * 扩展属性：检查类是否为枚举类型
 *
 * 枚举类型的构造器可以作为值直接访问（如 Color.Red）。
 * 这是仓颉语言中唯一支持"类值访问"的场景。
 */
val ClassAndEnumDescriptor.isEnum: Boolean
    get() = kind == ClassKind.ENUM

/**
 * 解析限定符的引用目标
 *
 * 根据限定符类型和选择器，确定限定符最终引用的描述符。
 * 这是限定表达式解析的核心逻辑。
 *
 * 解析逻辑：
 * 1. 类型参数限定符：直接返回类型参数描述符
 * 2. 包限定符 + 包成员：返回包描述符
 * 3. 类限定符 + 可调用成员：
 *    - 如果类有可调用接收器描述符（对象/伴生对象）
 *    - 记录隐式引用并返回类值类型描述符
 * 4. 其他情况：返回限定符自身的描述符
 *
 * @param qualifier 限定符接收器
 * @param selector 选择器描述符，null 表示限定符本身是完整表达式
 * @param context 表达式类型推导上下文
 * @return 引用目标描述符
 */
private fun resolveQualifierReferenceTarget(
    qualifier: QualifierReceiver,
    selector: DeclarationDescriptor?,
    context: ExpressionTypingContext
): DeclarationDescriptor {
    // 类型参数限定符直接返回
    if (qualifier is TypeParameterQualifier) {
        return qualifier.descriptor
    }

    // 确定选择器的容器（构造器的容器需要向上查找两级）
    val selectorContainer = when (selector) {
        is ConstructorDescriptor ->
            selector.containingDeclaration.containingDeclaration

        else ->
            selector?.containingDeclaration
    }

    // 如果是包限定符且选择器也在同一个包中，返回包描述符
    if (qualifier is PackageQualifier &&
        (selectorContainer is PackageFragmentDescriptor || selectorContainer is PackageViewDescriptor) &&
        DescriptorUtils.getFqName(qualifier.descriptor) == DescriptorUtils.getFqName(selectorContainer)
    ) {
        return qualifier.descriptor
    }

    // TODO 在其他地方决定伴生对象的短引用
    if (qualifier is ClassifierQualifier) {
        val classifier = qualifier.descriptor
        // 检查选择器是否为可调用的成员（有接收器参数）
        val selectorIsCallable = selector is CallableDescriptor &&
                (selector.dispatchReceiverParameter != null  )

        // TODO 简化此代码
        // 当类限定符出现在表达式位置时，
        // 应该提供正确的 REFERENCE_TARGET（带类型），
        // 并在枚举类型的情况下记录类型信息。
        val receiverClassifierDescriptor = classifier.getCallableReceiverDescriptorRetainingTypeAliasReference()
        if (selectorIsCallable && receiverClassifierDescriptor != null) {
            // 记录引用目标和类型
            context.trace.record(
                BindingContext.REFERENCE_TARGET,
                qualifier.referenceExpression,
                receiverClassifierDescriptor
            )
            context.trace.recordType(qualifier.expression, receiverClassifierDescriptor.defaultType)
            return receiverClassifierDescriptor
        }
    }

    return qualifier.descriptor
}

/**
 * 获取保留类型别名引用的可调用接收器描述符
 *
 * 对于类型别名，在某些情况下需要保留类型别名的引用而不是直接展开到底层类型。
 * 这对于错误消息和 IDE 功能很重要。
 *
 * 对于枚举类型，返回类本身作为可调用接收器（用于访问枚举构造器）。
 *
 * @return 可调用接收器描述符，如果不适用则返回 null
 */
private fun ClassifierDescriptor.getCallableReceiverDescriptorRetainingTypeAliasReference(): ClassDescriptor? =
    when (this) {
        // 枚举类：返回类本身作为可调用接收器
        is ClassDescriptor -> if (isEnum) this else null

        // 类型别名：如果指向枚举类型，返回底层枚举类
        is TypeAliasDescriptor ->
            classDescriptor?.takeIf { it.isEnum }

        else -> null
    }
