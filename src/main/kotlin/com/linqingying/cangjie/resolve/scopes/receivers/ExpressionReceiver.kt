/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.resolve.scopes.receivers

import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.types.CangJieType

interface ExpressionReceiver : ReceiverValue {
    val expression: CjExpression

    companion object {
        private open class ExpressionReceiverImpl(
            override val expression: CjExpression, type: CangJieType, original: ReceiverValue?
        ) : AbstractReceiverValue(type, original), ExpressionReceiver {
            override fun replaceType(newType: CangJieType) = ExpressionReceiverImpl(expression, newType, original)

            override fun toString() = "$type {$expression: ${expression.text}}"
        }

        private class ThisExpressionClassReceiver(
            override val classDescriptor: ClassDescriptor,
            expression: CjExpression,
            type: CangJieType,
            original: ReceiverValue?
        ) : ExpressionReceiverImpl(expression, type, original), ThisClassReceiver {
            override fun replaceType(newType: CangJieType) = ThisExpressionClassReceiver(classDescriptor, expression, newType, original)
        }

        private class SuperExpressionReceiver(
            override val thisType: CangJieType,
            expression: CjExpression,
            type: CangJieType,
            original: ReceiverValue?
        ) : ExpressionReceiverImpl(expression, type, original), SuperCallReceiverValue {
            override fun replaceType(newType: CangJieType) = SuperExpressionReceiver(thisType, expression, newType, original)
        }

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
                val descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, referenceExpression)
                if (descriptor is ClassDescriptor && !referenceExpression.isContextClassReceiverReference(bindingContext)) {
                    return ThisExpressionClassReceiver(descriptor.original, expression, type, original = null)
                }
            } else if (expression is CjSuperExpression) {
                // if there is no THIS_TYPE_FOR_SUPER_EXPRESSION in binding context, we fall through into more restrictive option
                // i.e. just return common ExpressionReceiverImpl
                bindingContext[BindingContext.THIS_TYPE_FOR_SUPER_EXPRESSION, expression]?.let { thisType ->
                    return SuperExpressionReceiver(thisType, expression, type, original = null)
                }
            }

            return ExpressionReceiverImpl(expression, type, original = null)
        }

        private fun CjReferenceExpression.isContextClassReceiverReference(bindingContext: BindingContext): Boolean =
            bindingContext[BindingContext.THIS_REFERENCE_TARGET, this]?.value is ContextClassReceiver
    }
}
