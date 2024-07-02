package com.huawei.cangjie.resolve.scopes.receivers

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.types.CangJieType

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
