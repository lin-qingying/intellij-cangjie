package com.linqingying.cangjie.descriptors

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiInvalidElementAccessException

class PsiDiagnosticUtils {
    class LineAndColumn(val line: Int, val column: Int, val lineContent: String?) {
        // NOTE: This method is used for presenting positions to the user
        override fun toString(): String {
            if (line < 0) {
                return "(offset: $column line unknown)"
            }
            return "($line,$column)"
        }

        companion object {
            val NONE: LineAndColumn = LineAndColumn(-1, -1, null)
        }
    }

    companion object {

@JvmStatic
        fun atLocation(element: PsiElement): String {
            if (element.isValid) {
                return atLocation(
                    element.containingFile,
                    element.textRange
                )
            }

            var file: PsiFile? = null
            var offset = -1
            try {
                file = element.containingFile
                offset = element.textOffset
            } catch (invalidException: PsiInvalidElementAccessException) {
                // ignore
            }

            return "at offset: " + (if (offset != -1) offset else "<unknown>") + " file: " + (file
                ?: "<unknown>")
        }

        @JvmStatic
        fun offsetToLineAndColumn(
            document: Document?,
            offset: Int
        ): LineAndColumn {
            if (document == null || document.textLength == 0) {
                return LineAndColumn(-1, offset, null)
            }

            val lineNumber = document.getLineNumber(offset)
            val lineStartOffset = document.getLineStartOffset(lineNumber)
            val column = offset - lineStartOffset

            val lineEndOffset = document.getLineEndOffset(lineNumber)
            val lineContent = document.charsSequence.subSequence(lineStartOffset, lineEndOffset)

            return LineAndColumn(
                lineNumber + 1,
                column + 1,
                lineContent.toString()
            )
        }

        @JvmStatic
        fun atLocation(file: PsiFile, textRange: TextRange, document: Document?): String {
            val offset = textRange.startOffset
            val virtualFile = file.virtualFile
            val pathSuffix = " in " + (virtualFile?.path ?: file.name)
            return offsetToLineAndColumn(document, offset)
                .toString() + pathSuffix
        }
        @JvmStatic
        fun atLocation(file: PsiFile, textRange: TextRange): String {
            val document = file.viewProvider.document
            return atLocation(file, textRange, document)
        }
    }
}
