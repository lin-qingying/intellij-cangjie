//package com.huawei.cangjie.resolve.calls.util
//
//import com.huawei.cangjie.descriptors.CallableDescriptor
//import com.huawei.cangjie.psi.*
//import com.huawei.cangjie.psi.psiUtil.getCalleeExpressionIfAny
//import com.huawei.cangjie.resolve.BindingContext
//import com.huawei.cangjie.resolve.BindingContext.CALL
//import com.huawei.cangjie.resolve.BindingContext.RESOLVED_CALL
//
//import com.huawei.cangjie.resolve.calls.model.ResolvedCall
//
//fun CjElement?.getResolvedCall(context: BindingContext): ResolvedCall<out CallableDescriptor>? {
//    return this?.getCall(context)?.getResolvedCall(context)
//}
//fun Call?.getResolvedCall(context: BindingContext): ResolvedCall<out CallableDescriptor>? {
//    return context[RESOLVED_CALL, this]
//}
//fun CjElement.getCall(context: BindingContext): Call? {
//    val element = if (this is CjExpression) CjPsiUtil.deparenthesize(this) else this
//    if (element == null) return null
//
//    // Do not use Call bound to outer call expression (if any) to prevent stack overflow during analysis
//    if (element is CjCallElement && element.calleeExpression == null) return null
//
//    if (element is CjMatchExpression) {
//        val subjectVariable = element.subjectVariable
//        if (subjectVariable != null) {
//            return subjectVariable.getCall(context) ?: context[CALL, element]
//        }
//    }
//
//    val reference: CjExpression? = when (val parent = element.parent) {
//        is CjInstanceExpressionWithLabel -> parent
//        is CjUserType -> parent.parent.parent as? CjConstructorCalleeExpression
//        else -> element.getCalleeExpressionIfAny()
//    }
//    if (reference != null) {
//        return context[CALL, reference]
//    }
//    return context[CALL, element]
//}