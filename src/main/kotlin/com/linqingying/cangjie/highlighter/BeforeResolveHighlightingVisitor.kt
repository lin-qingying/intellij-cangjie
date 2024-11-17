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

package com.linqingying.cangjie.highlighter

import com.linqingying.cangjie.doc.parser.CDocKnownTag
import com.linqingying.cangjie.doc.psi.impl.CDocLink
import com.linqingying.cangjie.doc.psi.impl.CDocTag
import com.linqingying.cangjie.highlighter.visitor.AbstractHighlightingVisitor
import com.linqingying.cangjie.lang.CangJieLanguage
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.*
import com.intellij.codeHighlighting.RainbowHighlighter
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.psi.PsiElement


class BeforeResolveHighlightingVisitor(holder: HighlightInfoHolder) : AbstractHighlightingVisitor(holder) {
    override fun visitElement(element: PsiElement) {
        val elementType = element.node.elementType
        val attributes = when {
            element is CDocLink && !willApplyRainbowHighlight(element) -> CangJieHighlightInfoTypeSemanticNames.CDOC_LINK

            elementType in CjTokens.SOFT_KEYWORDS -> {
                when (elementType) {
                    in CjTokens.MODIFIER_KEYWORDS -> CangJieHighlightInfoTypeSemanticNames.BUILTIN_ANNOTATION
                    in CjTokens.BASICTYPES -> CangJieHighlightInfoTypeSemanticNames.BUILTIN_ANNOTATION
                    else -> CangJieHighlightInfoTypeSemanticNames.KEYWORD
                }
            }

            else -> return
        }

        highlightName(element, attributes)
    }

    private fun willApplyRainbowHighlight(element: CDocLink): Boolean {
        if (!RainbowHighlighter.isRainbowEnabledWithInheritance(EditorColorsManager.getInstance().globalScheme, CangJieLanguage)) {
            return false
        }

        return (element.parent as? CDocTag)?.knownTag == CDocKnownTag.PARAM
    }




    override fun visitExpressionWithLabel(expression: CjExpressionWithLabel) {
        val targetLabel = expression.getTargetLabel()
        if (targetLabel != null) {
            highlightName(targetLabel, CangJieHighlightInfoTypeSemanticNames.LABEL)
        }
    }

    override fun visitSuperTypeCallEntry(call: CjSuperTypeCallEntry) {
        val calleeExpression = call.calleeExpression
        val typeElement = calleeExpression.typeReference?.typeElement
        if (typeElement is CjUserType) {
            typeElement.referenceExpression?.let { highlightName(it,
                CangJieHighlightInfoTypeSemanticNames.CONSTRUCTOR_CALL
            ) }
        }
        super.visitSuperTypeCallEntry(call)
    }


    override fun visitTypeParameter(parameter: CjTypeParameter) {
        parameter.nameIdentifier?.let { highlightName(it, CangJieHighlightInfoTypeSemanticNames.TYPE_PARAMETER) }
        super.visitTypeParameter(parameter)
    }

    override fun visitNamedFunction(function: CjNamedFunction) {
        highlightNamedDeclaration(function, CangJieHighlightInfoTypeSemanticNames.FUNCTION_DECLARATION)
        super.visitNamedFunction(function)
    }
}
