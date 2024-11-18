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

package com.linqingying.cangjie.ide.parameterInfo

import com.intellij.psi.PsiWhiteSpace
import com.linqingying.cangjie.ide.codeinsight.hints.InlayInfoDetails
import com.linqingying.cangjie.ide.codeinsight.hints.PsiInlayInfoDetail
import com.linqingying.cangjie.ide.codeinsight.hints.TextInlayInfoDetail
import com.linqingying.cangjie.ide.codeinsight.hints.declarative.SHOW_RETURN_EXPRESSIONS
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.endOffset
import com.linqingying.cangjie.psi.psiUtil.getParentOfType
import com.linqingying.cangjie.psi.psiUtil.getStrictParentOfType
import com.linqingying.cangjie.psi.psiUtil.isOneLiner
import com.linqingying.cangjie.resolve.BindingContext.USED_AS_RESULT_OF_LAMBDA
import com.linqingying.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode

import com.intellij.codeInsight.hints.InlayInfo
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.utils.safeAs

private fun CjLambdaExpression.getNameOfFunctionThatTakesLambda(): String? {
    val lambda = this
    val callExpression = this.getStrictParentOfType<CjCallExpression>() ?: return null
    return if (callExpression.lambdaArguments.any { it.getLambdaExpression() == lambda }) {
        val parent = lambda.parent

        callExpression.calleeExpression.safeAs<CjNameReferenceExpression>()?.getReferencedName()

    } else null
}

fun provideLambdaReturnValueHints(expression: CjExpression): InlayInfoDetails? {
    if (!expression.isLambdaReturnValueHintsApplicable()) {
        return null
    }

    val bindingContext = expression.safeAnalyzeNonSourceRootCode(BodyResolveMode.PARTIAL_WITH_CFA)
    return if (bindingContext[USED_AS_RESULT_OF_LAMBDA, expression] == true) {
        val functionLiteral = expression.getStrictParentOfType<CjFunctionLiteral>() ?: return null
        val lambdaExpression = functionLiteral.getStrictParentOfType<CjLambdaExpression>() ?: return null

        val lambdaName = lambdaExpression.getNameOfFunctionThatTakesLambda() ?: "lambda"
        val inlayInfo = InlayInfo("", expression.endOffset)
        InlayInfoDetails(
            inlayInfo,
            listOf(TextInlayInfoDetail("^"), PsiInlayInfoDetail(lambdaName, lambdaExpression)),
            option = SHOW_RETURN_EXPRESSIONS
        )
    } else null
}

fun provideLambdaReturnTypeHints(expression: CjExpression): InlayInfoDetails? {
    if (!expression.isLambdaReturnValueHintsApplicable(allowOneLiner = true)) {
        return null
    }

    val bindingContext = expression.safeAnalyzeNonSourceRootCode(BodyResolveMode.PARTIAL_WITH_CFA)
    return if (bindingContext[USED_AS_RESULT_OF_LAMBDA, expression] == true) {
        val functionLiteral = expression.getStrictParentOfType<CjFunctionLiteral>() ?: return null
        val type = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, functionLiteral).safeAs<FunctionDescriptor>()?.returnType ?: return null
        val inlayInfo = InlayInfo("", expression.endOffset)
        val infoDetails = buildList {
            add(TextInlayInfoDetail(": "))
            addAll(HintsTypeRenderer.getInlayHintsTypeRenderer(bindingContext, expression).renderTypeIntoInlayInfo(type))
        }
        InlayInfoDetails(inlayInfo, infoDetails)
    } else null
}
fun CjExpression.isLambdaReturnValueHintsApplicable(allowOneLiner: Boolean = false): Boolean {
    //if (allowOneLiner && this.isOneLiner()) {
    //    val literalWithBody = this is CjBlockExpression && isFunctionalLiteralWithBody()
    //    return literalWithBody
    //}

    if (this is CjMatchExpression) {
        return false
    }

    if (this is CjBlockExpression) {
        if (allowOneLiner && this.isOneLiner()) {
            return isFunctionalLiteralWithBody()
        }
        return false
    }

    if (this is CjIfExpression && !this.isOneLiner()) {
        return false
    }

    if (this.getParentOfType<CjIfExpression>(true)?.isOneLiner() == true) {
        return false
    }

    if (!CjPsiUtil.isStatement(this)) {
        if (!allowLabelOnExpressionPart(this)) {
            return false
        }
    } else if (forceLabelOnExpressionPart(this)) {
        return false
    }
    return isFunctionalLiteralWithBody()
}

private fun CjExpression.isFunctionalLiteralWithBody(): Boolean {
    val functionLiteral = this.getParentOfType<CjFunctionLiteral>(true)
    val body = functionLiteral?.bodyExpression ?: return false
    return !(body.statements.size == 1 && body.statements[0] == this)
}

private fun forceLabelOnExpressionPart(expression: CjExpression): Boolean {
    return expressionStatementPart(expression) != null
}

private fun expressionStatementPart(expression: CjExpression): CjExpression? {
    val splitPart: CjExpression = when (expression) {
//        is CjAnnotatedExpression -> expression.baseExpression
//        is CjLabeledExpression -> expression.baseExpression
        else -> null
    } ?: return null

    if (!isNewLineBeforeExpression(splitPart)) {
        return null
    }

    return splitPart
}

private fun isNewLineBeforeExpression(expression: CjExpression): Boolean {
    val whiteSpace = expression.node.treePrev?.psi as? PsiWhiteSpace ?: return false
    return whiteSpace.text.contains("\n")
}

private fun allowLabelOnExpressionPart(expression: CjExpression): Boolean {
    val parent = expression.parent as? CjExpression ?: return false
    return expression == expressionStatementPart(parent)
}
