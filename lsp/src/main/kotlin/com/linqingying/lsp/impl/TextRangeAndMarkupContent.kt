package com.linqingying.lsp.impl

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.lsp.util.getRangeInDocument
import org.eclipse.lsp4j.Hover

import org.eclipse.lsp4j.MarkupContent
import kotlin.jvm.internal.Intrinsics

data class TextRangeAndMarkupContent(
    val textRange: TextRange,
    val markupContent: MarkupContent
) {
    internal companion object {


        internal fun fromHover(
            hover: Hover,
            file: VirtualFile,
            offset: Int
        ): TextRangeAndMarkupContent {
            val document = FileDocumentManager.getInstance().getDocument(file)
            val textRange = if (document != null && hover.range != null) {
                getRangeInDocument(document, hover.range) ?: TextRange(offset, offset)
            } else {
                TextRange(offset, offset)
            }

            return if (hover.contents.isRight) {
                val markupContent = hover.contents.right

                TextRangeAndMarkupContent(textRange, markupContent)
            } else {
                val leftContent = hover.contents.left

                val markdownContent = leftContent.joinToString("\n\n"){
                    it.right.value
                }
                TextRangeAndMarkupContent(textRange, MarkupContent("markdown", markdownContent))
            }
        }
    }

    override operator fun equals(other: Any?): Boolean {
        return if (this === other) {
            true
        } else if (other !is TextRangeAndMarkupContent) {
            false
        } else {

            if (!Intrinsics.areEqual(this.textRange, other.textRange)) {
                false
            } else {
                Intrinsics.areEqual(this.markupContent, other.markupContent)
            }
        }
    }


    override fun toString(): String {
        return "TextRangeAndMarkupContent(textRange=" + this.textRange + ", markupContent=" + this.markupContent + ")"

    }

    override fun hashCode(): Int {
        var result = textRange.hashCode()
        result = 31 * result + markupContent.hashCode()
        return result
    }
}

