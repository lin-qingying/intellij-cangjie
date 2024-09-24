package com.linqingying.lsp.util


import com.intellij.injected.editor.DocumentWindow
import com.intellij.markdown.utils.convertMarkdownToHtml
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.StringUtilRt
import org.eclipse.lsp4j.*
import org.jetbrains.annotations.ApiStatus
import kotlin.math.min


fun getLsp4jPosition(document: Document, offset: Int): Position {
    if (document is DocumentWindow) {
        // It's very likely that the caller uses DocumentWindow not only when calling this function but also somewhere else.
        // The error helps to find the problematic place earlier.
        fileLogger().error("DocumentWindow is not expected here. Make sure to use DocumentWindow.delegate when working with the LSP server.")
        return getLsp4jPosition(document.delegate, document.injectedToHost(offset))
    }

    val lineNumber = document.getLineNumber(offset)
    return Position(lineNumber, offset - document.getLineStartOffset(lineNumber))
}

fun getLsp4jRange(document: Document, offset: Int, length: Int): Range =
    Range(getLsp4jPosition(document, offset), getLsp4jPosition(document, offset + length))

/**
 * Returns `null` if [position] is outside the document text range.
 */
fun getOffsetInDocument(document: Document, position: Position): Int? {
    if (document is DocumentWindow) {
        // It's very likely that the caller uses DocumentWindow not only when calling this function but also somewhere else.
        // The error helps to find the problematic place earlier.
        fileLogger().error("DocumentWindow is not expected here. Make sure to use DocumentWindow.delegate when working with the LSP server.")
        return getOffsetInDocument(document.delegate, position)
    }

    val lineCount = document.lineCount
    val line = position.line
    var character = position.character
    if (line == lineCount && character == 0) return document.textLength
    if (line < 0 || line >= lineCount || character < 0) return null

    val lineStartOffset = document.getLineStartOffset(line)
    if (line + 1 < lineCount && character > 0) {
        // Make sure that the `character` value doesn't exceed the line length.
        // Some buggy servers may send range with `"character":80` for zero-length lines
        // (https://youtrack.jetbrains.com/issue/IDEA-332939#focus=Comments-27-8189497.0-0)
        val nextLineStartOffset = document.getLineStartOffset(line + 1)
        character = min(character, nextLineStartOffset - 1 - lineStartOffset)
    }
    else if (line + 1 == lineCount && character > 0) {
        // workaround for servers that send { "line": <last_line>, "character": 2147483647 }
        character = min(character, document.textLength - lineStartOffset)
    }

    return (lineStartOffset + character).let { if (it <= document.textLength) it else null }
}

/**
 * Returns `null` if [range] is partially or fully outside the document text range.
 */
fun getRangeInDocument(document: Document, range: Range): TextRange? {
    val start = getOffsetInDocument(document, range.start) ?: return null
    val end = getOffsetInDocument(document, range.end) ?: return null
    if (!TextRange.isProperRange(start, end)) return null
    return TextRange(start, end)
}

/**
 * @return `true` if all `textEdits` were applied successfully;
 * or `false` as soon as some `textEdit` failed to get applied to the `document`
 * because `textEdit.range` was outside the `document` text range
 */
fun applyTextEdits(document: Document, textEdits: List<TextEdit>): Boolean {
    textEdits
        // descending sorting needed to apply edits starting from the end of the document, so the edits they don't influence each other
        .sortedWith { edit1, edit2 ->
            (edit2.range.start.line - edit1.range.start.line).takeIf { it != 0 }
                ?: (edit2.range.start.character - edit1.range.start.character)
        }
        .forEach { if (!applyTextEdit(document, it)) return@applyTextEdits false }

    return true
}

/**
 * @return `true` if `textEdit` was applied successfully;
 * `false` if the `textEdit` can't be applied to the `document` because `textEdit.range` is outside the `document` text range
 */
fun applyTextEdit(document: Document, textEdit: TextEdit): Boolean {
    val startOffset = getOffsetInDocument(document, textEdit.range.start)
    val endOffset = getOffsetInDocument(document, textEdit.range.end)
    if (startOffset == null || endOffset == null) {
        fileLogger().warn("Ignoring TextEdit, its text range is outside the document text range.\n" +
                "document.lineCount = ${document.lineCount}, document.textLength = ${document.textLength}, range: ${textEdit.range}")
        return false
    }

    val newText = StringUtilRt.convertLineSeparators(textEdit.newText)
    document.replaceString(startOffset, endOffset, newText)
    return true
}

@Deprecated("This function is not used and going to be removed. Third-party plugins may copy it to their own codebase.")
@ApiStatus.ScheduledForRemoval
fun convertMarkupContentToHtml(markupContent: MarkupContent): @NlsSafe String {
    return when {
        MarkupKind.MARKDOWN == markupContent.kind -> convertMarkdownToHtml(markupContent.value)
        MarkupKind.PLAINTEXT == markupContent.kind -> HtmlBuilder().append(markupContent.value).toString()
        else -> {
            fileLogger().warn("Unexpected MarkupKind: ${markupContent.kind}, treating as plain text")
            HtmlBuilder().append(markupContent.value).toString()
        }
    }
}
