/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.calls.util

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.psi.CjPsiUtil
import org.cangnova.cangjie.psi.CjThisExpression
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.results.ResolutionStatus
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.resolve.calls.tower.AbstractResolvedCall
import org.cangnova.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import org.cangnova.cangjie.resolve.getOwnerForEffectiveDispatchReceiverParameter
import org.cangnova.cangjie.resolve.scopes.receivers.ClassValueReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ImplicitReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.DefinitelyNonOptionType
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.StubTypeForBuilderInference
import org.cangnova.cangjie.types.checker.CapturedType
import org.cangnova.cangjie.types.contains

fun ResolvedCall<*>.getDispatchReceiverWithSmartCast(): ReceiverValue? =
    getReceiverValueWithSmartCast(dispatchReceiver, smartCastDispatchReceiverType)

fun getReceiverValueWithSmartCast(
    receiverArgument: ReceiverValue?,
    smartCastType: CangJieType?
) = smartCastType?.let { type -> SmartCastReceiverValue(type, original = null) } ?: receiverArgument

private class SmartCastReceiverValue(override val type: CangJieType, original: SmartCastReceiverValue?) : ReceiverValue {
    override val original = original ?: this

    override fun replaceType(newType: CangJieType) = SmartCastReceiverValue(newType, original)
}

// it returns true if call has no dispatch receiver (e.g. resulting descriptor is top-level function or local variable)
// or call receiver is effectively `this` instance (explicitly or implicitly) of resulting descriptor
// class A {
//   init(other: A)
//   let x
//   let y = other.x // return false for `other.x` as it's receiver is not `this`
// }
fun ResolvedCall<*>.hasThisOrNoDispatchReceiver(
    context: BindingContext
): Boolean {
    val dispatchReceiverValue = dispatchReceiver
    if (resultingDescriptor.dispatchReceiverParameter == null || dispatchReceiverValue == null) return true

    var dispatchReceiverDescriptor: DeclarationDescriptor? = null
    when (dispatchReceiverValue) {
        is ImplicitReceiver -> // foo() -- implicit receiver
            dispatchReceiverDescriptor = dispatchReceiverValue.declarationDescriptor

        is ClassValueReceiver -> {
            dispatchReceiverDescriptor = dispatchReceiverValue.classQualifier.descriptor
        }

        is ExpressionReceiver -> {
            val expression = CjPsiUtil.deparenthesize(dispatchReceiverValue.expression)
            if (expression is CjThisExpression) {
                // this.foo() -- explicit receiver
                dispatchReceiverDescriptor = context[BindingContext.REFERENCE_TARGET, expression.instanceReference]
            }
        }
    }

    return dispatchReceiverDescriptor == resultingDescriptor.getOwnerForEffectiveDispatchReceiverParameter()
}

fun ResolvedCall<*>.isNewNotCompleted(): Boolean = if (this is AbstractResolvedCall) !isCompleted() else false
fun CallableDescriptor.isNotSimpleCall(): Boolean =
    typeParameters.isNotEmpty() ||
            (returnType?.let { type ->
                type.contains {
                    it is CapturedType ||
                            it.constructor is IntegerLiteralTypeConstructor ||
                            it is DefinitelyNonOptionType ||
                            it is StubTypeForBuilderInference
                }
            } ?: false)

/**
 * 检查解析调用是否具有推断出的返回类型
 *
 * 此函数用于确定当前解析的调用是否已经推断出了具体的返回类型它通过检查调用的结果描述符
 * 来判断，如果结果描述符的返回类型包含未推断的类型变量，则表明返回类型尚未确定
 *
 * @return 如果解析调用具有推断出的返回类型，则返回true；否则返回false
 */
fun ResolvedCall<*>.hasInferredReturnType(): Boolean {
    if (isNewNotCompleted()) return false

    val returnType = this.resultingDescriptor.returnType ?: return false
    return !returnType.contains { ErrorUtils.isUninferredTypeVariable(it) }
}

fun CandidateApplicability.toResolutionStatus(): ResolutionStatus = when (this) {
    CandidateApplicability.RESOLVED,
    CandidateApplicability.RESOLVED_LOW_PRIORITY,
    CandidateApplicability.RESOLVED_WITH_ERROR,
    CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY -> ResolutionStatus.SUCCESS

    CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER -> ResolutionStatus.RECEIVER_TYPE_ERROR
    CandidateApplicability.UNSAFE_CALL -> ResolutionStatus.UNSAFE_CALL_ERROR
    else -> ResolutionStatus.OTHER_ERROR
}
