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

package org.cangnova.cangjie.completion

import org.cangnova.cangjie.completion.handlers.indexOfSkippingSpace
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.*
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.ui.RowIcon
import org.cangnova.cangjie.quickfix.overrideImplement.moveCaretIntoGeneratedElement
import javax.swing.Icon

class OverridesCompletionLookupElementDecorator(
    lookupElement: LookupElement,
    private val declaration: CjCallableDeclaration?,
    private val text: String,
    private val isImplement: Boolean,
    private val icon: RowIcon,
    private val baseClassName: String?,
    private val baseClassIcon: Icon?,
    private val isConstructorParameter: Boolean,

    private val generateMember: () -> CjCallableDeclaration,
    private val shortenReferences: (CjElement) -> Unit,
) : LookupElementDecorator<LookupElement>(lookupElement) {
    override fun getLookupString() =
        if (declaration == null) "override" else delegate.lookupString // don't use "override" as lookup string when already in the name of declaration

    override fun getAllLookupStrings() = setOf(lookupString, delegate.lookupString)

    override fun renderElement(presentation: LookupElementPresentation) {
        super.renderElement(presentation)

        presentation.itemText = text
        presentation.isItemTextBold = isImplement
        presentation.icon = icon
        presentation.clearTail()
        presentation.setTypeText(baseClassName, baseClassIcon)
    }

    override fun getDelegateInsertHandler(): InsertHandler<LookupElement> = InsertHandler { context, _ ->
        val dummyMemberHead = when {
            declaration != null -> ""
            isConstructorParameter -> "override prop "
            else -> "override func "
        }
        val dummyMemberTail = when {
            isConstructorParameter || declaration is CjProperty -> "dummy: Dummy ,@"
            else -> "dummy() {}"
        }
        val dummyMemberText = dummyMemberHead + dummyMemberTail
        val override = CjTokens.OVERRIDE_KEYWORD.value

        tailrec fun calcStartOffset(startOffset: Int, diff: Int = 0): Int {
            return when {
                context.document.text[startOffset - 1].isWhitespace() -> calcStartOffset(startOffset - 1, diff + 1)
                context.document.text.substring(startOffset - override.length, startOffset) == override -> {
                    startOffset - override.length
                }

                else -> diff + startOffset
            }
        }

        val startOffset = calcStartOffset(context.startOffset)
        val tailOffset = context.tailOffset
        context.document.replaceString(startOffset, tailOffset, dummyMemberText)

        val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
        psiDocumentManager.commitDocument(context.document)

        val dummyMember = context.file.findElementAt(startOffset)!!.getStrictParentOfType<CjNamedDeclaration>()!!

        // keep original modifiers
        val psiFactory = CjPsiFactory(context.project)
        val modifierList = psiFactory.createModifierList(dummyMember.modifierList!!.text)

        fun isCommentOrWhiteSpace(e: PsiElement) = e is PsiComment || e is PsiWhiteSpace
        fun createCommentOrWhiteSpace(e: PsiElement) =
            if (e is PsiComment) psiFactory.createComment(e.text) else psiFactory.createWhiteSpace(e.text)

        val dummyMemberChildren = dummyMember.allChildren
        val headComments =
            dummyMemberChildren.takeWhile(::isCommentOrWhiteSpace).map(::createCommentOrWhiteSpace).toList()
        val tailComments =
            dummyMemberChildren.toList().takeLastWhile(::isCommentOrWhiteSpace).map(::createCommentOrWhiteSpace)

        val prototype = generateMember()
//        prototype.modifierList!!.replace(modifierList)
        val insertedMember = dummyMember.replaced(prototype)

        val insertedMemberParent = insertedMember.parent
        headComments.forEach { insertedMemberParent.addBefore(it, insertedMember) }
        tailComments.reversed().forEach { insertedMemberParent.addAfter(it, insertedMember) }

        shortenReferences(insertedMember)

        if (isConstructorParameter) {
            psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.document)

            val offset = insertedMember.endOffset
            val chars = context.document.charsSequence
            val commaOffset = chars.indexOfSkippingSpace(',', offset)!!
            val atCharOffset = chars.indexOfSkippingSpace('@', commaOffset + 1)!!
            context.document.deleteString(offset, atCharOffset + 1)

            context.editor.moveCaret(offset)
        } else {
            moveCaretIntoGeneratedElement(context.editor, insertedMember)
        }
    }
}
