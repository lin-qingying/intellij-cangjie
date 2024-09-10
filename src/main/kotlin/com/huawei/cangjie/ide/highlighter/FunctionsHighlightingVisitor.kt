package com.huawei.cangjie.ide.highlighter

import com.huawei.cangjie.builtins.isFunctionTypeOrSubtype
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.ConstructorDescriptor
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.descriptors.annotations.fqNameOrNull
import com.huawei.cangjie.highlighter.CangJieHighlightInfoTypeSemanticNames
import com.huawei.cangjie.psi.CjBinaryExpression
import com.huawei.cangjie.psi.CjCallExpression
import com.huawei.cangjie.psi.CjReferenceExpression
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.calls.model.ResolvedCall
import com.huawei.cangjie.resolve.calls.model.VariableAsFunctionResolvedCall
import com.huawei.cangjie.resolve.calls.tasks.isDynamic
import com.huawei.cangjie.resolve.calls.util.getResolvedCall
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.psi.PsiElement

internal class FunctionsHighlightingVisitor(holder: HighlightInfoHolder, bindingContext: BindingContext) :
    AfterAnalysisHighlightingVisitor(holder, bindingContext) {

    override fun visitBinaryExpression(expression: CjBinaryExpression) {
        if (expression.operationReference.getIdentifier() != null) {
            expression.getResolvedCall(bindingContext)?.let { resolvedCall ->
                highlightCall(expression.operationReference, resolvedCall)
            }
        }
        super.visitBinaryExpression(expression)
    }

    override fun visitCallExpression(expression: CjCallExpression) {
        val callee = expression.calleeExpression
        val resolvedCall = expression.getResolvedCall(bindingContext)
        if (callee is CjReferenceExpression && callee !is CjCallExpression && resolvedCall != null) {
            highlightCall(callee, resolvedCall)
        }

        super.visitCallExpression(expression)
    }

    private fun highlightCall(callee: PsiElement, resolvedCall: ResolvedCall<out CallableDescriptor>) {
        val calleeDescriptor = resolvedCall.resultingDescriptor

        val extensions = CangJieHighlightingVisitorExtension.EP_NAME.extensionList

        val attributesKey = extensions.firstNotNullOfOrNull { extension ->
            extension.highlightCall(callee, resolvedCall)
        } ?:
        when {
//            calleeDescriptor.fqNameOrNull() == CANGJIE_SUSPEND_BUILT_IN_FUNCTION_FQ_NAME -> CangJieHighlightInfoTypeSemanticNames.KEYWORD
            calleeDescriptor.isDynamic() -> CangJieHighlightInfoTypeSemanticNames.DYNAMIC_FUNCTION_CALL
//            calleeDescriptor is FunctionDescriptor && calleeDescriptor.isSuspend -> CangJieHighlightInfoTypeSemanticNames.SUSPEND_FUNCTION_CALL
            resolvedCall is VariableAsFunctionResolvedCall -> {
                val container = calleeDescriptor.containingDeclaration
                val containedInFunctionClassOrSubclass = container is ClassDescriptor && container.defaultType.isFunctionTypeOrSubtype
                if (containedInFunctionClassOrSubclass)
                    CangJieHighlightInfoTypeSemanticNames.VARIABLE_AS_FUNCTION_CALL
                else
                    CangJieHighlightInfoTypeSemanticNames.VARIABLE_AS_FUNCTION_LIKE_CALL
            }

            calleeDescriptor is ConstructorDescriptor -> CangJieHighlightInfoTypeSemanticNames.CONSTRUCTOR_CALL
            calleeDescriptor !is FunctionDescriptor -> null
         calleeDescriptor is FunctionDescriptor && calleeDescriptor.isExtend -> CangJieHighlightInfoTypeSemanticNames.EXTENSION_FUNCTION_CALL
            calleeDescriptor.extensionReceiverParameter != null -> CangJieHighlightInfoTypeSemanticNames.EXTENSION_FUNCTION_CALL
            DescriptorUtils.isTopLevelDeclaration(calleeDescriptor) -> CangJieHighlightInfoTypeSemanticNames.PACKAGE_FUNCTION_CALL
            else -> CangJieHighlightInfoTypeSemanticNames.FUNCTION_CALL
        }
        attributesKey?.let { key ->
            highlightName(callee, key)
        }
    }
}
