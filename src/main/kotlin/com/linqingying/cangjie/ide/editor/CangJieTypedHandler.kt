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

package com.linqingying.cangjie.ide.editor

import com.linqingying.cangjie.CjNodeTypes
import com.linqingying.cangjie.doc.lexer.CDocTokens
import com.linqingying.cangjie.ide.formatter.adjustLineIndent
import com.linqingying.cangjie.ide.project.tools.projectWizard.core.safeAs
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.CjDeclarationModifierList
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.CjFunctionLiteral
import com.linqingying.cangjie.psi.CjSimpleNameStringTemplateEntry
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.TypedHandler
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtilEx
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.startOffset
import com.intellij.util.DocumentUtil


internal object CangJieTypedHandlerHelper {
    internal val PREVIOUS_IN_STRING_DOLLAR_TYPED_OFFSET_KEY =
        Key.create<Int>("PREVIOUS_IN_STRING_DOLLAR_TYPED_OFFSET_KEY")

    internal fun autoPopupParameterInfo(project: Project, editor: Editor) {
        val offset = editor.caretModel.offset
        if (offset == 0) return
        val iterator = editor.highlighter.createIterator(offset - 1)
        val tokenType = iterator.tokenType
        if (CjTokens.COMMENTS.contains(tokenType) ||
            tokenType === CjTokens.REGULAR_STRING_PART ||
            tokenType === CjTokens.OPEN_QUOTE ||
            tokenType === CjTokens.RUNE_LITERAL
        ) {
            return
        }

        AutoPopupController.getInstance(project).autoPopupParameterInfo(editor, null)
    }

    internal fun autoPopupMemberLookup(project: Project, editor: Editor): Unit =
        AutoPopupController.getInstance(project).autoPopupMemberLookup(editor, fun(file: PsiFile): Boolean {
            val offset = editor.caretModel.offset
            val lastToken = file.findElementAt(offset - 1) ?: return false
            val elementType = lastToken.node.elementType
            if (elementType === CjTokens.DOT || elementType === CjTokens.SAFE_ACCESS) return true
            if (elementType === CjTokens.REGULAR_STRING_PART && lastToken.textRange.startOffset == offset - 1) {
                val prevSibling = lastToken.parent.prevSibling
                return prevSibling is CjSimpleNameStringTemplateEntry
            }
            return false
        })

    private fun isLabelCompletion(chars: CharSequence, offset: Int): Boolean {
        return endsWith(chars, offset, "this@")
                || endsWith(chars, offset, "return@")
                || endsWith(chars, offset, "break@")
                || endsWith(chars, offset, "continue@")
    }

    /**
     * @param atElement [PsiElement] corresponding to typed `@`
     */
    private fun isAnnotationCompletion(atElement: PsiElement): Boolean {
        val errorElement = atElement.parent as? PsiErrorElement
        return errorElement?.parent.let { it is CjDeclarationModifierList }
    }

    internal fun autoPopupAt(project: Project, editor: Editor) {
        AutoPopupController.getInstance(project).autoPopupMemberLookup(editor) { file: PsiFile ->
            val offset = editor.caretModel.offset
            val chars = editor.document.charsSequence
            val elementAtCaret = file.findElementAt(offset - 1)
            val lastNodeType = elementAtCaret?.node?.elementType ?: return@autoPopupMemberLookup false

            lastNodeType === CDocTokens.TEXT ||
                    lastNodeType === CjTokens.AT && (isLabelCompletion(chars, offset) || isAnnotationCompletion(
                elementAtCaret
            ))
        }
    }

    internal fun autoPopupColon(project: Project, editor: Editor): Unit =
        AutoPopupController.getInstance(project).autoPopupMemberLookup(editor) { file: PsiFile ->
            val offset = editor.caretModel.offset
            val lastElement = file.findElementAt(offset - 1) ?: return@autoPopupMemberLookup false
            isAnnotationAfterUseSiteTargetCompletion(lastElement)
        }

    /**
     * Check whether the [element] is `:` in annotation with specified use-site target. For example, `:` in `@file:`.
     */
    private fun isAnnotationAfterUseSiteTargetCompletion(element: PsiElement): Boolean {
        val colonElement = element.takeIf { it.node.elementType == CjTokens.COLON }
        val identifierElement = colonElement?.prevSibling?.takeIf { it.node.elementType == CjTokens.IDENTIFIER }
        val atElement = identifierElement?.prevSibling?.takeIf { it.node.elementType == CjTokens.AT } ?: return false

        return isAnnotationCompletion(atElement)
    }

    private fun endsWith(chars: CharSequence, offset: Int, text: String): Boolean =
        if (offset < text.length) false else chars.subSequence(offset - text.length, offset).toString() == text


    internal fun autoIndentCase(
        editor: Editor,
        project: Project,
        file: PsiFile,
        klass: Class<*>,
        forFirstElement: Boolean = true,
    ): Boolean {
        val offset = editor.caretModel.offset
        PsiDocumentManager.getInstance(project).commitDocument(editor.document)
        val currElement = file.findElementAt(offset - 1)
        if (currElement != null) {
            // Should be applied only if there's nothing but the whitespace in line before the element
            val prevLeaf = PsiTreeUtil.prevLeaf(currElement)
            if (forFirstElement && !(prevLeaf is PsiWhiteSpace && prevLeaf.textContains('\n'))) {
                return false
            }

            val parent = currElement.parent
            if (klass.isInstance(parent)) {
                val curElementLength = currElement.text.length
                if (offset < curElementLength) return false
                if (forFirstElement) {
                    CodeStyleManager.getInstance(project).adjustLineIndent(file, offset - curElementLength)
                } else {
                    val document = PsiDocumentManager.getInstance(project).getDocument(file)
                    if (document != null) {
                        CodeStyleManager.getInstance(project)
                            .adjustLineIndent(document, DocumentUtil.getLineStartOffset(offset, document))
                    }
                }

                return true
            }
        }

        return false
    }
}

class CangJieTypedHandler : TypedHandlerDelegate() {

    private var cangjieLTTyped = false


    private var isGlobalPreviousDollarInString = false

    override fun beforeCharTyped(
        c: Char,
        project: Project,
        editor: Editor,
        file: PsiFile,
        fileType: FileType
    ): Result {

        if (file !is CjFile) return Result.CONTINUE
        when (c) {




            '<' -> {
                cangjieLTTyped = CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET &&
                        LtGtTypingUtils.shouldAutoCloseAngleBracket(editor.caretModel.offset, editor)

                autoPopupParameterInfo(project, editor)
            }

            '>' -> {
                if (CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET && LtGtTypingUtils.handleCangJieGTInsert(
                        editor
                    )
                ) {
                    return Result.STOP
                }
            }

            '{' -> {
                // Returning Result.CONTINUE will cause inserting "{}" for unmatched '{'
                val offset = editor.caretModel.offset
                if (offset == 0) {
                    return Result.CONTINUE
                }

                val iterator = editor.highlighter.createIterator(offset - 1)
                while (!iterator.atEnd() && iterator.tokenType === TokenType.WHITE_SPACE) {
                    iterator.retreat()
                }

                if (iterator.atEnd() || iterator.tokenType !in CangJieTypedHandlerTokenSets.SUPPRESS_AUTO_INSERT_CLOSE_BRACE_AFTER) {
                    AutoPopupController.getInstance(project).autoPopupParameterInfo(editor, null)
                    return Result.CONTINUE
                }

                val tokenBeforeBraceOffset = iterator.start
                val document = editor.document
                PsiDocumentManager.getInstance(project).commitDocument(document)
                val leaf = file.findElementAt(offset)
                if (leaf != null) {
                    val parent = leaf.parent
                    if (parent != null && parent.node.elementType in CangJieTypedHandlerTokenSets.CONTROL_FLOW_EXPRESSIONS) {
                        val nonWhitespaceSibling = PsiTreeUtil.skipWhitespacesBackward(leaf)
                        if (nonWhitespaceSibling != null && nonWhitespaceSibling.startOffset == tokenBeforeBraceOffset) {
                            EditorModificationUtilEx.insertStringAtCaret(editor, "{", false, true)
                            TypedHandler.indentBrace(project, editor, '{')
                            return Result.STOP
                        }
                    }

                    if (leaf.text == "}" && parent is CjFunctionLiteral && document.getLineNumber(offset) == document.getLineNumber(
                            parent.getTextRange().startOffset
                        )
                    ) {
                        EditorModificationUtilEx.insertStringAtCaret(editor, "{} ", false, false)
                        editor.caretModel.moveToOffset(offset + 1)
                        return Result.STOP
                    }
                }
            }

            '.' -> CangJieTypedHandlerHelper.autoPopupMemberLookup(project, editor)
            ':' -> CangJieTypedHandlerHelper.autoPopupColon(project, editor)
            '[' -> CangJieTypedHandlerHelper.autoPopupParameterInfo(project, editor)
            '@' -> CangJieTypedHandlerHelper.autoPopupAt(project, editor)
        }

        return Result.CONTINUE
    }

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {


        if (file !is CjFile) return Result.CONTINUE
        var previousDollarInStringOffset: Int? = null
        if (isGlobalPreviousDollarInString) {
            isGlobalPreviousDollarInString = false
            previousDollarInStringOffset =
                editor.getUserData(CangJieTypedHandlerHelper.PREVIOUS_IN_STRING_DOLLAR_TYPED_OFFSET_KEY)
        }
        editor.putUserData(CangJieTypedHandlerHelper.PREVIOUS_IN_STRING_DOLLAR_TYPED_OFFSET_KEY, null)


        when {


//            //        c == '(' ->{
////            val offset = editor.caretModel.offset
////            editor.document.insertString(offset, ")")
////            editor.caretModel.moveToOffset(offset)
////        }
            cangjieLTTyped -> {
                cangjieLTTyped = false
                LtGtTypingUtils.handleCangJieAutoCloseLT(editor)

                return Result.STOP
            }

            c == '{' && CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET -> {
                PsiDocumentManager.getInstance(project).commitDocument(editor.document)
                val offset = editor.caretModel.offset
                val previousElement = file.findElementAt(offset - 1)
                if (previousElement is LeafPsiElement && previousElement.elementType === CjTokens.LONG_TEMPLATE_ENTRY_START) {
                    val identifier = file.findElementAt(offset)
                        ?.safeAs<LeafPsiElement>()
                        ?.takeIf { it.elementType == CjTokens.IDENTIFIER }
                        ?:  run {
                            editor.document.insertString(offset, "}")
                            return Result.STOP
                        }

                    val lastInLongTemplateEntry = previousElement.getParent().lastChild
                    val isSimpleLongTemplateEntry = lastInLongTemplateEntry is LeafPsiElement &&
                            lastInLongTemplateEntry.elementType === CjTokens.LONG_TEMPLATE_ENTRY_END &&
                            lastInLongTemplateEntry.getParent().textLength == identifier.textLength + "\${}".length

                    if (!isSimpleLongTemplateEntry) {
                        val isAfterTypedDollar =
                            previousDollarInStringOffset != null && previousDollarInStringOffset.toInt() == offset - 1
                        if (isAfterTypedDollar) {
                            editor.document.insertString(offset, "}")
                            return Result.STOP
                        }
                    }
                }
            }

            c == '$' -> {
                val offset = editor.caretModel.offset
                val element = file.findElementAt(offset)
                if (element is LeafPsiElement && element.elementType === CjTokens.REGULAR_STRING_PART) {
                    editor.putUserData(CangJieTypedHandlerHelper.PREVIOUS_IN_STRING_DOLLAR_TYPED_OFFSET_KEY, offset)
                    isGlobalPreviousDollarInString = true
                }
            }
        }

        return Result.CONTINUE
    }

    private object CangJieTypedHandlerTokenSets {
        val CONTROL_FLOW_EXPRESSIONS: TokenSet = TokenSet.create(
            CjNodeTypes.IF,
            CjNodeTypes.ELSE,
            CjNodeTypes.FOR,
            CjNodeTypes.WHILE,
            CjNodeTypes.TRY,
        )

        val SUPPRESS_AUTO_INSERT_CLOSE_BRACE_AFTER: TokenSet = TokenSet.create(
            CjTokens.RPAR,
            CjTokens.ELSE_KEYWORD,
            CjTokens.TRY_KEYWORD,
        )
    }

    companion object {
        private val PREVIOUS_IN_STRING_DOLLAR_TYPED_OFFSET_KEY =
            Key.create<Int>("PREVIOUS_IN_STRING_DOLLAR_TYPED_OFFSET_KEY")

        private fun autoPopupParameterInfo(project: Project, editor: Editor) {
            val offset = editor.caretModel.offset
            if (offset == 0) return
            val iterator = editor.highlighter.createIterator(offset - 1)
            val tokenType = iterator.tokenType
            if (CjTokens.COMMENTS.contains(tokenType) ||
                tokenType === CjTokens.REGULAR_STRING_PART ||
                tokenType === CjTokens.OPEN_QUOTE ||
                tokenType === CjTokens.RUNE_LITERAL ||
                tokenType == CjTokens.CHARACTER_BYTE_LITERAL
            ) {
                return
            }

            AutoPopupController.getInstance(project).autoPopupParameterInfo(editor, null)
        }

        private fun autoPopupMemberLookup(project: Project, editor: Editor): Unit =
            AutoPopupController.getInstance(project).autoPopupMemberLookup(editor, fun(file: PsiFile): Boolean {
                val offset = editor.caretModel.offset
                val lastToken = file.findElementAt(offset - 1) ?: return false
                val elementType = lastToken.node.elementType
                if (elementType === CjTokens.DOT) return true
                if (elementType === CjTokens.REGULAR_STRING_PART && lastToken.textRange.startOffset == offset - 1) {
                    val prevSibling = lastToken.parent.prevSibling
                    return prevSibling is CjSimpleNameStringTemplateEntry
                }
                return false
            })

        private fun isLabelCompletion(chars: CharSequence, offset: Int): Boolean {
            return endsWith(chars, offset, "this@")
                    || endsWith(chars, offset, "return@")
                    || endsWith(chars, offset, "break@")
                    || endsWith(chars, offset, "continue@")
        }

        private fun autoPopupAt(project: Project, editor: Editor) {
            AutoPopupController.getInstance(project).autoPopupMemberLookup(editor) { file: PsiFile ->
                val offset = editor.caretModel.offset
                val chars = editor.document.charsSequence
                val lastNodeType =
                    file.findElementAt(offset - 1)?.node?.elementType ?: return@autoPopupMemberLookup false

                lastNodeType === CDocTokens.TEXT || (lastNodeType === CjTokens.AT && isLabelCompletion(chars, offset))
            }
        }

//        private fun autoPopupCallableReferenceLookup(project: Project, editor: Editor): Unit =
//            AutoPopupController.getInstance(project).autoPopupMemberLookup(editor) { file: PsiFile ->
//                val offset = editor.caretModel.offset
//                val lastElement = file.findElementAt(offset - 1) ?: return@autoPopupMemberLookup false
//                lastElement.node.elementType === CjTokens.COLONCOLON
//            }

        private fun endsWith(chars: CharSequence, offset: Int, text: String): Boolean =
            if (offset < text.length) false else chars.subSequence(offset - text.length, offset).toString() == text

//        private fun dataClassValParameterInsert(
//            project: Project,
//            editor: Editor,
//            file: PsiFile,
//            beforeType: Boolean
//        ) {
//            if (!CangJieEditorOptions.getInstance().isAutoAddValKeywordToDataClassParameters) return
//            val document = editor.document
//            PsiDocumentManager.getInstance(project).commitDocument(document)
//            var commaOffset = editor.caretModel.offset
//            if (!beforeType) commaOffset--
//            if (commaOffset < 1) return
//            val elementOnCaret = file.findElementAt(commaOffset) ?: return
//            var contextMatched = false
//            var parentElement = elementOnCaret.parent
//            if (parentElement is CjParameterList) {
//                parentElement = parentElement.getParent()
//                if (parentElement is CjPrimaryConstructor) {
//                    parentElement = parentElement.getParent()
//                    if (parentElement is CjClass) {
//                        val cjlassElement = parentElement
////                        contextMatched = cjlassElement.mustHaveNonEmptyPrimaryConstructor()
//                    }
//                }
//            }
//
//            if (!contextMatched) return
//            val leftElement = PsiTreeUtil.skipWhitespacesAndCommentsBackward(elementOnCaret) as? CjParameter ?: return
//            val typeReference = leftElement.typeReference ?: return
//            if (leftElement.hasLetOrVar()) return
//            if (typeReference.textLength == 0) return
//            document.insertString(leftElement.textOffset, "val ")
//        }

        private fun autoIndentCase(
            editor: Editor,
            project: Project,
            file: PsiFile,
            cjlass: Class<*>,
            forFirstElement: Boolean = true,
        ): Boolean {
            val offset = editor.caretModel.offset
            PsiDocumentManager.getInstance(project).commitDocument(editor.document)
            val currElement = file.findElementAt(offset - 1)
            if (currElement != null) {
                // Should be applied only if there's nothing but the whitespace in line before the element
                val prevLeaf = PsiTreeUtil.prevLeaf(currElement)
                if (forFirstElement && !(prevLeaf is PsiWhiteSpace && prevLeaf.textContains('\n'))) {
                    return false
                }

                val parent = currElement.parent
                if (cjlass.isInstance(parent)) {
                    val curElementLength = currElement.text.length
                    if (offset < curElementLength) return false
                    if (forFirstElement) {
                        CodeStyleManager.getInstance(project).adjustLineIndent(file, offset - curElementLength)
                    } else {
                        PsiDocumentManager.getInstance(project).getDocument(file)?.adjustLineIndent(project, offset)
                    }

                    return true
                }
            }

            return false
        }
    }
}

