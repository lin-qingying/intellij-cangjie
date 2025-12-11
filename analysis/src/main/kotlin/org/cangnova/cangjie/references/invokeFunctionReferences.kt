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

package org.cangnova.cangjie.references

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.MultiRangeReference
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.psi.Call
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.calls.model.VariableAsFunctionResolvedCall
import org.cangnova.cangjie.resolve.calls.util.getCall
import org.cangnova.cangjie.resolve.calls.util.getResolvedCall

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
        val resolvedCall = call?.getResolvedCall(context)
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
