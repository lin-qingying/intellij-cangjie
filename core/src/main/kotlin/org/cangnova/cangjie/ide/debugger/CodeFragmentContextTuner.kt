/*
 * Copyright 2026 LinQingYing. and contributors.
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