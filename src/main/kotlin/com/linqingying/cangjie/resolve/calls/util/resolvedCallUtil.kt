package com.linqingying.cangjie.resolve.calls.util

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.psi.CjPsiUtil
import com.linqingying.cangjie.psi.CjThisExpression
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall
import com.linqingying.cangjie.resolve.calls.results.ResolutionStatus
import com.linqingying.cangjie.resolve.calls.tower.CandidateApplicability
import com.linqingying.cangjie.resolve.calls.tower.NewAbstractResolvedCall
import com.linqingying.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import com.linqingying.cangjie.resolve.getOwnerForEffectiveDispatchReceiverParameter
import com.linqingying.cangjie.resolve.scopes.receivers.ClassValueReceiver
import com.linqingying.cangjie.resolve.scopes.receivers.ExpressionReceiver
import com.linqingying.cangjie.resolve.scopes.receivers.ImplicitReceiver
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.DefinitelyNotNullType
import com.linqingying.cangjie.types.ErrorUtils
import com.linqingying.cangjie.types.StubTypeForBuilderInference
import com.linqingying.cangjie.types.checker.NewCapturedType
import com.linqingying.cangjie.types.util.contains

fun ResolvedCall<*>.getDispatchReceiverWithSmartCast(): ReceiverValue? =
    getReceiverValueWithSmartCast(dispatchReceiver, smartCastDispatchReceiverType)

fun getReceiverValueWithSmartCast(
    receiverArgument: ReceiverValue?,
    smartCastType: CangJieType?
) = smartCastType?.let { type -> SmartCastReceiverValue(type, original = null) } ?: receiverArgument

private class SmartCastReceiverValue(private val type: CangJieType, original: SmartCastReceiverValue?) : ReceiverValue {
    private val original = original ?: this

    override fun getType() = type
    override fun replaceType(newType: CangJieType) = SmartCastReceiverValue(newType, original)
    override fun getOriginal() = original
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
                dispatchReceiverDescriptor = context.get(BindingContext.REFERENCE_TARGET, expression.instanceReference)
            }
        }
    }

    return dispatchReceiverDescriptor == resultingDescriptor.getOwnerForEffectiveDispatchReceiverParameter()
}

fun ResolvedCall<*>.isNewNotCompleted(): Boolean = if (this is NewAbstractResolvedCall) !isCompleted() else false
fun CallableDescriptor.isNotSimpleCall(): Boolean =
    typeParameters.isNotEmpty() ||
            (returnType?.let { type ->
                type.contains {
                    it is NewCapturedType ||
                            it.constructor is IntegerLiteralTypeConstructor ||
                            it is DefinitelyNotNullType ||
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
