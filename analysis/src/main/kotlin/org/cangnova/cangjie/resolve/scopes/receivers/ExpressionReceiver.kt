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

package org.cangnova.cangjie.resolve.scopes.receivers

import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.types.CangJieType

/**
 * 表达式接收器
 *
 * ExpressionReceiver 表示由一个具体的表达式计算得出的接收器值。
 * 与隐式接收器不同，表达式接收器对应源代码中的一个显式表达式。
 *
 * 例如在以下代码中：
 * ```
 * val obj = MyClass()
 * obj.foo()  // obj 是表达式接收器
 * ```
 *
 * @property expression 产生此接收器的表达式
 *
 * @see ReceiverValue
 * @see ImplicitReceiver
 */
interface ExpressionReceiver : ReceiverValue {
    /** 产生此接收器的表达式 */
    val expression: CjExpression

    companion object {
        /**
         * 基础的表达式接收器实现
         *
         * @param expression 产生接收器的表达式
         * @param type 接收器的类型
         * @param original 原始的接收器值
         */
        private open class ExpressionReceiverImpl(
            override val expression: CjExpression, type: CangJieType, original: ReceiverValue?
        ) : AbstractReceiverValue(type, original), ExpressionReceiver {
            override fun replaceType(newType: CangJieType) = ExpressionReceiverImpl(expression, newType, original)

            override fun toString() = "$type {$expression: ${expression.text}}"
        }

        /**
         * This 表达式的类接收器
         *
         * 专门用于 this 表达式的类接收器实现。
         *
         * @param classDescriptor 类的描述符
         * @param expression this 表达式
         * @param type 接收器的类型
         * @param original 原始的接收器值
         */
        private class ThisExpressionClassReceiver(
            override val classDescriptor: ClassDescriptor,
            expression: CjExpression,
            type: CangJieType,
            original: ReceiverValue?
        ) : ExpressionReceiverImpl(expression, type, original), ThisClassReceiver {
            override fun replaceType(newType: CangJieType) =
                ThisExpressionClassReceiver(classDescriptor, expression, newType, original)
        }

        /**
         * Super 表达式的接收器
         *
         * 专门用于 super 表达式的接收器实现。
         *
         * @param thisType 实际的接收器类型（this 的类型）
         * @param expression super 表达式
         * @param type 父类型
         * @param original 原始的接收器值
         */
        private class SuperExpressionReceiver(
            override val thisType: CangJieType,
            expression: CjExpression,
            type: CangJieType,
            original: ReceiverValue?
        ) : ExpressionReceiverImpl(expression, type, original), SuperCallReceiverValue {
            override fun replaceType(newType: CangJieType) =
                SuperExpressionReceiver(thisType, expression, newType, original)
        }

        /**
         * 创建表达式接收器
         *
         * 根据表达式的类型创建相应的表达式接收器实例：
         * - 对于 this 表达式，创建 [ThisExpressionClassReceiver]
         * - 对于 super 表达式，创建 [SuperExpressionReceiver]
         * - 对于其他表达式，创建普通的 [ExpressionReceiverImpl]
         *
         * @param expression 产生接收器的表达式
         * @param type 接收器的类型
         * @param bindingContext 绑定上下文，用于解析表达式引用
         * @return 对应的表达式接收器实例
         */
        fun create(
            expression: CjExpression,
            type: CangJieType,
            bindingContext: BindingContext
        ): ExpressionReceiver {
            var referenceExpression: CjReferenceExpression? = null
            if (expression is CjThisExpression) {
                referenceExpression = expression.instanceReference
            } else if (expression is CjConstructorDelegationReferenceExpression) { // todo check this
                referenceExpression = expression
            }

            if (referenceExpression != null) {
                val descriptor = bindingContext[BindingContext.REFERENCE_TARGET, referenceExpression]
                if (descriptor is ClassDescriptor) {
                    return ThisExpressionClassReceiver(descriptor.original, expression, type, original = null)
                }
            } else if (expression is CjSuperExpression) {
                // 如果 binding context 中没有 THIS_TYPE_FOR_SUPER_EXPRESSION，则使用更严格的选项
                // 即返回普通的 ExpressionReceiverImpl
                bindingContext[BindingContext.THIS_TYPE_FOR_SUPER_EXPRESSION, expression]?.let { thisType ->
                    return SuperExpressionReceiver(thisType, expression, type, original = null)
                }
            }

            return ExpressionReceiverImpl(expression, type, original = null)
        }
    }
}
