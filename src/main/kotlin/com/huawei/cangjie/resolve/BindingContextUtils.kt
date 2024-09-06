package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjPureElement
import com.huawei.cangjie.psi.CjReferenceExpression
import com.huawei.cangjie.psi.psiUtil.parentsWithSelf
import com.huawei.cangjie.resolve.BindingContext.*
import com.huawei.cangjie.resolve.calls.context.ResolutionContext
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.scopes.takeSnapshot
import com.huawei.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import com.intellij.psi.PsiElement

fun BindingTrace.recordScope(scope: LexicalScope, element: CjElement?) {
    if (element != null) {
//        TODO()
        record(LEXICAL_SCOPE, element, scope.takeSnapshot() as LexicalScope)
    }
}

fun CjExpression.getReferenceTargets(context: BindingContext): Collection<DeclarationDescriptor> {
    val targetDescriptor = if (this is CjReferenceExpression) context[REFERENCE_TARGET, this] else null
    return targetDescriptor?.let { listOf(it) } ?: context[AMBIGUOUS_REFERENCE_TARGET, this].orEmpty()
}
fun <C : ResolutionContext<C>> ResolutionContext<C>.recordDataFlowInfo(expression: CjExpression?) {
    if (expression == null) return

    val typeInfo = trace.get(EXPRESSION_TYPE_INFO, expression)
    if (typeInfo != null) {
        trace.record(EXPRESSION_TYPE_INFO, expression, typeInfo.replaceDataFlowInfo(dataFlowInfo))
    } else if (dataFlowInfo != DataFlowInfo.EMPTY) {
        // Don't store anything in BindingTrace if it's simply an empty DataFlowInfo
        trace.record(EXPRESSION_TYPE_INFO, expression, noTypeInfo(dataFlowInfo))
    }
}
fun BindingContext.getDataFlowInfoBefore(position: PsiElement): DataFlowInfo {
    for (element in position.parentsWithSelf) {
        (element as? CjExpression)
            ?.let { this[DATA_FLOW_INFO_BEFORE, it] }
            ?.let { return it }
    }
    return DataFlowInfo.EMPTY
}

fun CjExpression.isUsedAsStatement(context: BindingContext): Boolean = !isUsedAsExpression(context)

fun CjElement.isUsedAsExpression(context: BindingContext): Boolean =
    context[USED_AS_EXPRESSION, this] ?: false

fun CjPureElement.findClassDescriptor(bindingContext: BindingContext): ClassDescriptor = when (this) {
    is PsiElement -> BindingContextUtils.getNotNull(bindingContext, CLASS, this)
//    is SyntheticClassOrObjectDescriptor.SyntheticDeclaration -> descriptor()
    else -> throw IllegalArgumentException("$this shall be PsiElement or SyntheticClassOrObjectDescriptor.SyntheticDeclaration")
}
