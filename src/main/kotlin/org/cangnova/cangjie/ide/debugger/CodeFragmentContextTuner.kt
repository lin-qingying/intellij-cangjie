package org.cangnova.cangjie.ide.debugger

import com.intellij.openapi.application.ApplicationManager

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement

import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjAbstractClassBody
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjTypeStatement

interface CodeFragmentContextTuner {
    fun tuneContextElement(element: PsiElement?): PsiElement?

    companion object {
        fun getInstance(): CodeFragmentContextTuner {
            return ApplicationManager.getApplication().getService(CodeFragmentContextTuner::class.java)
        }
    }
}


internal class CangJieCodeFragmentContextTuner : CodeFragmentContextTuner {
    override fun tuneContextElement(element: PsiElement?): PsiElement? {
        if (element == null) return null


        val containingFile = element.containingFile

        if (containingFile !is CjFile) {
            return null
        }

        val accurateElement = getAccurateContextElement(element, containingFile)
        if (accurateElement != null) {
            return accurateElement
        }

        return containingFile
    }

    private fun getAccurateContextElement(elementAt: PsiElement, containingFile: CjFile): PsiElement? {
        // elementAt can be PsiWhiteSpace when codeFragment is created from line start offset (in case of first opening EE window)
        val elementAtSkippingWhitespaces = getElementSkippingWhitespaces(elementAt)

        if (elementAtSkippingWhitespaces is LeafPsiElement && elementAtSkippingWhitespaces.elementType == CjTokens.RBRACE) {
            val classBody = elementAtSkippingWhitespaces.parent as? CjAbstractClassBody
            val classOrObject = classBody?.parent as? CjTypeStatement
            var declarationParent = classOrObject?.parent


            if (declarationParent != null) {
                return getAccurateContextElement(declarationParent, containingFile)
            }
        }

        val lineStartOffset = elementAtSkippingWhitespaces.textOffset

        val targetExpression =
            PsiTreeUtil.findElementOfClassAtOffset(containingFile, lineStartOffset, CjExpression::class.java, false)

//        val editorTextProvider = CangJieEditorTextProvider.instance

//        if (targetExpression != null) {
//            if (editorTextProvider.isAcceptedAsCodeFragmentContext(targetExpression)) {
//                return targetExpression
//            }
//
//            editorTextProvider.findEvaluationTarget(elementAt, true)?.let { return it }
//
//            targetExpression.parents(withSelf = false)
//                .firstOrNull { editorTextProvider.isAcceptedAsCodeFragmentContext(it) }
//                ?.let { return it }
//        }

        return targetExpression
    }

    private fun getElementSkippingWhitespaces(elementAt: PsiElement): PsiElement {
        if (elementAt is PsiWhiteSpace || elementAt is PsiComment) {
            val newElement =
                PsiTreeUtil.skipSiblingsForward(elementAt, PsiWhiteSpace::class.java, PsiComment::class.java)
            if (newElement != null) {
                return newElement
            }
        }

        return elementAt
    }
}