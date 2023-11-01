package com.huawei.cangjie.highlighter

import com.huawei.cangjie.doc.parser.CDocKnownTag
import com.huawei.cangjie.doc.psi.impl.CDocLink
import com.huawei.cangjie.doc.psi.impl.CDocTag
import com.huawei.cangjie.highlighter.visitor.AbstractHighlightingVisitor
import com.huawei.cangjie.lang.CangJieLanguage
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.*
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
            typeElement.referenceExpression?.let { highlightName(it, CangJieHighlightInfoTypeSemanticNames.CONSTRUCTOR_CALL) }
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
