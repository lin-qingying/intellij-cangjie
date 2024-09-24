package com.linqingying.lsp.impl.highlighting

import com.intellij.openapi.util.TextRange

internal class LspSemanticToken(
    textRange: TextRange,
    val tokenType: String,
    val tokenModifiers: List<String>
) : LspTextRangeOwner(textRange) {

    override fun toString(): String {

        return "" + textRange + " " + this.tokenType
    }
}

