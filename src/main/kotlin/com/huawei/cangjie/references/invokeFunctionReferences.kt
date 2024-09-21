package com.huawei.cangjie.references

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.Call
import com.huawei.cangjie.psi.CjCallExpression
import com.huawei.cangjie.psi.CjConstructor
import com.huawei.cangjie.psi.CjImportAlias
import com.huawei.cangjie.psi.psiUtil.containingTypeStatement
import com.huawei.cangjie.references.util.unwrappedTargets
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.calls.model.VariableAsFunctionResolvedCall
import com.huawei.cangjie.resolve.calls.util.getCall
import com.huawei.cangjie.resolve.calls.util.getResolvedCall
import com.huawei.cangjie.utils.OperatorNameConventions
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.MultiRangeReference

abstract class CangJieAbstractInvokeFunctionReference(expression: CjCallExpression) :
    CjSimpleReference<CjCallExpression>(expression),
    MultiRangeReference {

    override val resolvesByNames: Collection<Name> get() = NAMES

    override fun getRangeInElement(): TextRange {
        return element.textRange.shiftRight(-element.textOffset)
    }



    override fun getRanges(): List<TextRange> {
        val list = ArrayList<TextRange>()
        val valueArgumentList = expression.valueArgumentList
        if (valueArgumentList != null) {
            if (valueArgumentList.arguments.isNotEmpty()) {
                val valueArgumentListNode = valueArgumentList.node
                val lPar = valueArgumentListNode.findChildByType(CjTokens.LPAR)
                if (lPar != null) {
                    list.add(getRange(lPar))
                }

                val rPar = valueArgumentListNode.findChildByType(CjTokens.RPAR)
                if (rPar != null) {
                    list.add(getRange(rPar))
                }
            } else {
                list.add(getRange(valueArgumentList.node))
            }
        }

        val functionLiteralArguments = expression.lambdaArguments
        for (functionLiteralArgument in functionLiteralArguments) {
            val functionLiteralExpression = functionLiteralArgument.getLambdaExpression() ?: continue
            list.add(getRange(functionLiteralExpression.leftCurlyBrace))
            val rightCurlyBrace = functionLiteralExpression.rightCurlyBrace
            if (rightCurlyBrace != null) {
                list.add(getRange(rightCurlyBrace))
            }
        }

        return list
    }

    private fun getRange(node: ASTNode): TextRange {
        val textRange = node.textRange
        return textRange.shiftRight(-expression.textOffset)
    }

    override fun canRename(): Boolean = true

    companion object {
        private val NAMES = listOf(OperatorNameConventions.INVOKE)
    }
}

class CjInvokeFunctionReference(expression: CjCallExpression) : CangJieAbstractInvokeFunctionReference(expression) {

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val call = element.getCall(context)
        val resolvedCall = call.getResolvedCall(context)
        return when {
            resolvedCall is VariableAsFunctionResolvedCall ->
                setOf<DeclarationDescriptor>((resolvedCall as VariableAsFunctionResolvedCall).functionCall.candidateDescriptor)

            call != null && resolvedCall != null && call.callType == Call.CallType.INVOKE ->
                setOf<DeclarationDescriptor>(resolvedCall.candidateDescriptor)

            else ->
                emptyList()
        }
    }



}
