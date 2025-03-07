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

package cn.cangnova.cangjie.ide.parameterInfo

import cn.cangnova.cangjie.psi.CjLambdaExpression

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiComment
import com.intellij.psi.TokenType
import cn.cangnova.cangjie.ide.codeinsight.hints.InlayInfoDetails
import cn.cangnova.cangjie.ide.codeinsight.hints.TextInlayInfoDetail
import cn.cangnova.cangjie.ide.codeinsight.hints.declarative.SHOW_IMPLICIT_RECEIVERS_AND_PARAMS
import cn.cangnova.cangjie.psi.psiUtil.siblings
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import cn.cangnova.cangjie.resolve.lazy.BodyResolveMode
import cn.cangnova.cangjie.types.util.isUnit

fun provideLambdaImplicitHints(lambda: CjLambdaExpression): List<InlayInfoDetails>? {
    val lbrace = lambda.leftCurlyBrace
    if (!lbrace.isFollowedByNewLine()) {
        return null
    }
    val bindingContext = lambda.safeAnalyzeNonSourceRootCode(BodyResolveMode.PARTIAL)
    val functionDescriptor = bindingContext[BindingContext.FUNCTION, lambda.functionLiteral] ?: return null

    val implicitReceiverHint = functionDescriptor.extensionReceiverParameter?.let { implicitReceiver ->
        val type = implicitReceiver.type
        val renderedType = HintsTypeRenderer.getInlayHintsTypeRenderer(bindingContext, lambda).renderTypeIntoInlayInfo(type)
        InlayInfoDetails(
            InlayInfo("", lbrace.psi.textRange.endOffset),
            listOf(TextInlayInfoDetail("this: ")) + renderedType,
            option = SHOW_IMPLICIT_RECEIVERS_AND_PARAMS
        )
    }

    val singleParameter = functionDescriptor.valueParameters.singleOrNull()
    val singleParameterHint = if (singleParameter != null && bindingContext[BindingContext.AUTO_CREATED_IT, singleParameter] == true) {
        val type = singleParameter.type
        if (type.isUnit()) null else {
            val renderedType = HintsTypeRenderer.getInlayHintsTypeRenderer(bindingContext, lambda).renderTypeIntoInlayInfo(type)
            InlayInfoDetails(
                InlayInfo("", lbrace.textRange.endOffset),
                listOf(TextInlayInfoDetail("it: ")) + renderedType,
                option = SHOW_IMPLICIT_RECEIVERS_AND_PARAMS
            )
        }
    } else null

    return listOfNotNull(implicitReceiverHint, singleParameterHint)
}

internal fun ASTNode.isFollowedByNewLine(): Boolean {
    for (sibling in siblings()) {
        if (sibling.elementType != TokenType.WHITE_SPACE && sibling.psi !is PsiComment) {
            continue
        }
        if (sibling.elementType == TokenType.WHITE_SPACE && sibling.textContains('\n')) {
            return true
        }
    }
    return false
}
