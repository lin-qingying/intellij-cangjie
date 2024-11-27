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

package com.linqingying.lsp.util

import com.intellij.injected.editor.DocumentWindow
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtilRt
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextEdit
import kotlin.math.min

fun getLsp4jPosition(document: Document, offset: Int): Position {
    if (document is DocumentWindow) {
        // 调用此函数时，调用者很可能不仅仅使用了 DocumentWindow，还可能在其他地方也使用了它。
        // 这个错误有助于更早地发现问题所在。
        fileLogger().error("DocumentWindow is not expected here. Make sure to use DocumentWindow.delegate when working with the LSP server.")
        return getLsp4jPosition(document.delegate, document.injectedToHost(offset))
    }

    val lineNumber = document.getLineNumber(offset)
    return Position(lineNumber, offset - document.getLineStartOffset(lineNumber))
}

fun getLsp4jRange(document: Document, offset: Int, length: Int): Range =
    Range(getLsp4jPosition(document, offset), getLsp4jPosition(document, offset + length))

/**
 * 如果 [position] 超出文档文本范围，则返回 `null`。
 */
fun getOffsetInDocument(document: Document, position: Position): Int? {
    if (document is DocumentWindow) {
        // 调用此函数时，调用者很可能不仅仅使用了 DocumentWindow，还可能在其他地方也使用了它。
        // 这个错误有助于更早地发现问题所在。
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
        // 确保 `character` 值不会超过行的长度。
        // 一些有问题的服务器可能会为零长度行发送 `"character":80` 的范围
        // (https://youtrack.jetbrains.com/issue/IDEA-332939#focus=Comments-27-8189497.0-0)
        val nextLineStartOffset = document.getLineStartOffset(line + 1)
        character = min(character, nextLineStartOffset - 1 - lineStartOffset)
    } else if (line + 1 == lineCount && character > 0) {
        // 针对服务器发送 { "line": <last_line>, "character": 2147483647 } 的情况的解决方法
        character = min(character, document.textLength - lineStartOffset)
    }

    return (lineStartOffset + character).let { if (it <= document.textLength) it else null }
}

/**
 * 如果 [range] 部分或完全超出文档文本范围，则返回 `null`。
 */
fun getRangeInDocument(document: Document, range: Range): TextRange? {
    val start = getOffsetInDocument(document, range.start) ?: return null
    val end = getOffsetInDocument(document, range.end) ?: return null
    if (!TextRange.isProperRange(start, end)) return null
    return TextRange(start, end)
}

/**
 * @return 如果所有 `textEdits` 都成功应用，则返回 `true`；
 * 如果某些 `textEdit` 无法应用到 `document` 中，
 * 因为 `textEdit.range` 超出了 `document` 的文本范围，则返回 `false`
 */
fun applyTextEdits(document: Document, textEdits: List<TextEdit>): Boolean {
    textEdits
        // 降序排序，从文档末尾开始应用编辑，以避免编辑之间相互影响
        .sortedWith { edit1, edit2 ->
            (edit2.range.start.line - edit1.range.start.line).takeIf { it != 0 }
                ?: (edit2.range.start.character - edit1.range.start.character)
        }
        .forEach { if (!applyTextEdit(document, it)) return@applyTextEdits false }

    return true
}

/**
 * @return 如果 `textEdit` 成功应用，则返回 `true`；
 * 如果 `textEdit` 无法应用到 `document` 中，
 * 因为 `textEdit.range` 超出了 `document` 的文本范围，则返回 `false`
 */
fun applyTextEdit(document: Document, textEdit: TextEdit): Boolean {
    val startOffset = getOffsetInDocument(document, textEdit.range.start)
    val endOffset = getOffsetInDocument(document, textEdit.range.end)
    if (startOffset == null || endOffset == null) {
        // 忽略超出文档范围的 TextEdit
        fileLogger().warn(
            "Ignoring TextEdit, its text range is outside the document text range.\n" +
                    "document.lineCount = ${document.lineCount}, document.textLength = ${document.textLength}, range: ${textEdit.range}"
        )
        return false
    }

    val newText = StringUtilRt.convertLineSeparators(textEdit.newText)
    document.replaceString(startOffset, endOffset, newText)
    return true
}
