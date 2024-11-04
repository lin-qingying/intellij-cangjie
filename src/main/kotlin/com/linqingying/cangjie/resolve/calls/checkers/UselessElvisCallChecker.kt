package com.linqingying.cangjie.resolve.calls.checkers

import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.diagnostics.reportDiagnosticOnce
import com.linqingying.cangjie.psi.CjBinaryExpression
import com.linqingying.cangjie.resolve.calls.inference.model.TypeVariableTypeConstructor
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall
import com.linqingying.cangjie.types.expressions.ControlStructureTypingUtils
import com.linqingying.cangjie.types.isError
import com.linqingying.cangjie.types.util.TypeUtils
import com.linqingying.cangjie.types.util.contains
import com.intellij.psi.PsiElement
import com.linqingying.cangjie.resolve.calls.smartcasts.Nullability

class UselessElvisCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (resolvedCall.resultingDescriptor.name != ControlStructureTypingUtils.ResolveConstruct.ELVIS.specialFunctionName) return

        val elvisBinaryExpression = resolvedCall.call.callElement as? CjBinaryExpression ?: return
        val left = elvisBinaryExpression.left ?: return
        val right = elvisBinaryExpression.right ?: return

        val leftType = context.trace.getType(left) ?: return

        // if type contains not fixed `TypeVariable` it means that call wasn't completed, we should wait for its completion first
        if (leftType.isError || leftType.contains { it.constructor is TypeVariableTypeConstructor }) return

        if (!TypeUtils.isNullableType(leftType)) {
            context.trace.reportDiagnosticOnce(Errors.USELESS_ELVIS.on(elvisBinaryExpression, leftType))
            return
        }

        val dataFlowValue = context.dataFlowValueFactory.createDataFlowValue(left, leftType, context.resolutionContext)
        if (context.dataFlowInfo.getStableNullability(dataFlowValue) == Nullability.NOT_NULL) {
            context.trace.reportDiagnosticOnce(Errors.USELESS_ELVIS.on(elvisBinaryExpression, leftType))
            return
        }

//        if (CjPsiUtil.isOptionConstant(right) && !leftType.isNullabilityFlexible()) {
//            context.trace.reportDiagnosticOnce(Errors.USELESS_ELVIS_RIGHT_IS_NULL.on(elvisBinaryExpression))
//        }
    }
}
