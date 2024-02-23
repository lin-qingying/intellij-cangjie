package com.linqingying.lsp.api.customization.requests.util


import com.intellij.injected.editor.DocumentWindow
import com.intellij.markdown.utils.convertMarkdownToHtml
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.StringUtilRt
import org.eclipse.lsp4j.*

private object Lsp4jUtil

fun getLsp4jPosition(document: Document, offset: Int): Position {
    if (document is DocumentWindow) {
        // It's very likely that the caller uses DocumentWindow not only when calling this method but also somewhere else.
        // The error helps to find the problematic place earlier.
        logger<Lsp4jUtil>().error(
            "DocumentWindow is not expected here. Make sure to use DocumentWindow.delegate when working with the LSP server."
        )
        return getLsp4jPosition(document.delegate, document.injectedToHost(offset))
    }

    val lineNumber = document.getLineNumber(offset)
    return Position(lineNumber , offset - document.getLineStartOffset(lineNumber) )
}

fun getLsp4jRange(document: Document, offset: Int, length: Int): Range =
    Range(getLsp4jPosition(document, offset), getLsp4jPosition(document, offset + length))

/**
 * Returns `null` if [position] is outside the document text textRange.
 */
fun getOffsetInDocument(document: Document, position: Position): Int? {
    if (document is DocumentWindow) {
        // It's very likely that the caller uses DocumentWindow not only when calling this method but also somewhere else.
        // The error helps to find the problematic place earlier.
        logger<Lsp4jUtil>().error(
            "DocumentWindow is not expected here. Make sure to use DocumentWindow.delegate when working with the LSP server."
        )
        return getOffsetInDocument(document.delegate, position)
    }

    val lineCount = document.lineCount
    if (position.line == lineCount && position.character == 0) return document.textLength
    if (position.line >= lineCount) return null
    return (document.getLineStartOffset(position.line) + position.character).let { if (it <= document.textLength) it else null }
}

/**
 * Returns `null` if [range] is partially or fully outside the document text textRange.
 */
fun getRangeInDocument(document: Document, range: Range): TextRange? {
    val start = getOffsetInDocument(document, range.start) ?: return null
    val end = getOffsetInDocument(document, range.end) ?: return null
    return TextRange(start, end)
}

fun convertMarkupContentToHtml(markupContent: MarkupContent): @NlsSafe String {
    return when {
        MarkupKind.MARKDOWN == markupContent.kind -> convertMarkdownToHtml(markupContent.value)
        MarkupKind.PLAINTEXT == markupContent.kind -> HtmlBuilder().append(markupContent.value).toString()
        else -> {
            logger<Lsp4jUtil>().warn("Unexpected MarkupKind: ${markupContent.kind}, treating as plain text")
            HtmlBuilder().append(markupContent.value).toString()
        }
    }
}


fun applyTextEdits(
    document: Document,
    textEdits: List<TextEdit>
): Boolean {
    val sortedTextEdits = textEdits.sortedWith { edit1, edit2 ->
        val lineDiff = edit2.range.start.line - edit1.range.start.line
        if (lineDiff != 0) {
            lineDiff
        } else {
            val charDiff = edit2.range.start.character - edit1.range.start.character
            if (charDiff != 0) charDiff else 0
        }
    }
    sortedTextEdits.forEach { textEdit ->
        if (!applyTextEdit(document, textEdit)) {
            return false
        }
    }
    return true
}


fun applyTextEdit(
    document: Document,
    textEdit: TextEdit
): Boolean {

    val start = textEdit.range.start
    val offsetStart = getOffsetInDocument(document, start)
    val end = textEdit.range.end
    val offsetEnd = getOffsetInDocument(document, end)

    if (offsetStart != null && offsetEnd != null) {
        val newText = StringUtilRt.convertLineSeparators(textEdit.newText)
        document.replaceString(offsetStart, offsetEnd, newText)
        return true
    } else {
        val logger = Logger.getInstance(Lsp4jUtil::class.java)
        val lineCount = document.lineCount
        logger.warn("Ignoring TextEdit, its text range is outside the document text range.\ndocument.lineCount = $lineCount, document.textLength = ${document.textLength}, range: ${textEdit.range}")
        return false
    }
}
