package com.huawei.cangjie.ide.completion.handlers

import com.huawei.cangjie.ide.completion.keywords.KeywordLookupObject
import com.huawei.cangjie.ide.formatter.adjustLineIndent
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.CjPsiFactory
import com.huawei.cangjie.psi.psiUtil.getNextSiblingIgnoringWhitespace
import com.huawei.cangjie.psi.psiUtil.moveCaret
import com.huawei.cangjie.psi.psiUtil.startOffset
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager

fun Document.isTextAt(offset: Int, text: String) =
    offset + text.length <= textLength && getText(TextRange(offset, offset + text.length)) == text

fun surroundWithBracesIfInStringTemplate(context: InsertionContext): Boolean {
    val startOffset = context.startOffset
    val document = context.document
    if (startOffset > 0 && document.charsSequence[startOffset - 1] == '$') {
        val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
        psiDocumentManager.commitDocument(document)

        if (context.file.findElementAt(startOffset - 1)?.node?.elementType == CjTokens.SHORT_TEMPLATE_ENTRY_START) {
            psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)

            document.insertString(startOffset, "{")
            context.offsetMap.addOffset(CompletionInitializationContext.START_OFFSET, startOffset + 1)

            val tailOffset = context.tailOffset
            document.insertString(tailOffset, "}")
            context.tailOffset = tailOffset
            return true
        }
    }

    return false
}

fun CharSequence.skipSpaces(index: Int): Int = index.until(length).firstOrNull {
    val c = this[it]
    c != ' ' && c != '\t'
} ?: this.length

fun LookupElement.withLineIndentAdjuster(): LookupElement = LookupElementDecorator.withDelegateInsertHandler(
    this,
) { context, item ->
    item.handleInsert(context)
    context.document.adjustLineIndent(context.project, context.startOffset)
}

private fun String.unindent(indent: String): String {
    val text = this
    return buildString {
        val lines = text.lines()
        for ((index, line) in lines.withIndex()) {
            append(line.removePrefix(indent))
            if (index != lines.lastIndex) append('\n')
        }
    }
}

fun CharSequence.skipSpacesAndLineBreaks(index: Int): Int = index.until(length).firstOrNull {
    val c = this[it]
    c != ' ' && c != '\t' && c != '\n' && c != '\r'
} ?: this.length

fun CharSequence.isCharAt(offset: Int, c: Char) = offset < length && this[offset] == c
fun CharSequence.indexOfSkippingSpace(c: Char, startIndex: Int): Int? {
    for (i in startIndex until this.length) {
        val currentChar = this[i]
        if (c == currentChar) return i
        if (currentChar != ' ' && currentChar != '\t') return null
    }

    return null
}

fun createKeywordConstructLookupElement(
    project: Project,
    keyword: String,
    fileTextToReformat: String,
    trimSpacesAroundCaret: Boolean = false,
    adjustLineIndent: Boolean = false,
): LookupElement {
    val file = CjPsiFactory(project).createFile(fileTextToReformat)
    CodeStyleManager.getInstance(project).reformat(file)
    val newFileText = file.text

    val keywordOffset = newFileText.indexOf(keyword)
    assert(keywordOffset >= 0)
    val keywordEndOffset = keywordOffset + keyword.length

    val caretPlaceHolder = "caret"

    val caretOffset = newFileText.indexOf(caretPlaceHolder)
    assert(caretOffset >= 0)
    assert(caretOffset >= keywordEndOffset)

    var tailBeforeCaret = newFileText.substring(keywordEndOffset, caretOffset)
    var tailAfterCaret = newFileText.substring(caretOffset + caretPlaceHolder.length)

    if (trimSpacesAroundCaret) {
        tailBeforeCaret = tailBeforeCaret.trimEnd()
        tailAfterCaret = tailAfterCaret.trimStart()
    }

    val indent = detectIndent(newFileText, keywordOffset)
    tailBeforeCaret = tailBeforeCaret.unindent(indent)
    tailAfterCaret = tailAfterCaret.unindent(indent)

    val tailText =
        (if (tailBeforeCaret.contains('\n')) tailBeforeCaret.replace("\n", "").trimEnd() else tailBeforeCaret) +
                "..." +
                (if (tailAfterCaret.contains('\n')) tailAfterCaret.replace("\n", "").trimStart() else tailAfterCaret)

    val lookupElement = KeywordConstructLookupObject(keyword, fileTextToReformat)
    return LookupElementBuilder.create(lookupElement, keyword)
        .bold()
        .withTailText(tailText)
        .withInsertHandler { insertionContext, _ ->
            if (insertionContext.completionChar == Lookup.NORMAL_SELECT_CHAR ||
                insertionContext.completionChar == Lookup.REPLACE_SELECT_CHAR ||
                insertionContext.completionChar == Lookup.AUTO_INSERT_SELECT_CHAR
            ) {
                val keywordStartOffset = if (!adjustLineIndent) {
                    insertionContext.tailOffset - keyword.length
                } else {
                    val offset = insertionContext.tailOffset - keyword.length
                    insertionContext.document.adjustLineIndent(insertionContext.project, offset)
                    insertionContext.tailOffset - keyword.length
                }

                val offset = keywordStartOffset + keyword.length
                val newIndent = detectIndent(insertionContext.document.charsSequence, keywordStartOffset)
                val beforeCaret = tailBeforeCaret.indentLinesAfterFirst(newIndent)
                val afterCaret = tailAfterCaret.indentLinesAfterFirst(newIndent)

                val element = insertionContext.file.findElementAt(offset)

                val sibling = when {
                    element !is PsiWhiteSpace -> element
                    element.textContains('\n') -> null
                    else -> element.getNextSiblingIgnoringWhitespace(true)
                }

                if (sibling != null &&
                    beforeCaret.trimStart()
                        .startsWith(insertionContext.document.getText(TextRange.from(sibling.startOffset, 1)))
                ) {
                    insertionContext.editor.moveCaret(sibling.startOffset + 1)
                } else {
                    insertionContext.document.insertString(offset, beforeCaret + afterCaret)
                    insertionContext.editor.moveCaret(offset + beforeCaret.length)
                }
            }
        }
}

private fun detectIndent(text: CharSequence, offset: Int): String {
    return text.substring(0, offset)
        .substringAfterLast('\n')
        .takeWhile(Char::isWhitespace)
}

private fun String.indentLinesAfterFirst(indent: String): String {
    val text = this
    return buildString {
        val lines = text.lines()
        for ((index, line) in lines.withIndex()) {
            if (index > 0) append(indent)
            append(line)
            if (index != lines.lastIndex) append('\n')
        }
    }
}

private data class KeywordConstructLookupObject(
    private val keyword: String,
    private val constructToInsert: String
) : KeywordLookupObject()
