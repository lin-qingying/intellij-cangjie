package com.linqingying.lsp.impl.requests

import com.intellij.openapi.util.TextRange

import org.eclipse.lsp4j.MarkupContent
import kotlin.jvm.internal.Intrinsics

data class TextRangeAndMarkupContent(
  val  textRange: TextRange,
 val   markupContent: MarkupContent
) {
    override operator fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        } else if (other !is TextRangeAndMarkupContent) {
            return false
        } else {

            return if (!Intrinsics.areEqual(this.textRange, other.textRange)) {
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

