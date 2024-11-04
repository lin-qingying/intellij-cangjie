/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package com.linqingying.cangjie.references

import com.linqingying.cangjie.lexer.CangJieLexer
import com.linqingying.cangjie.lexer.CjTokens.IDENTIFIER
import com.linqingying.cangjie.psi.psiUtil.elementType
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType

fun String.getCangJieLexerTokenType(): IElementType? {
    val lexer = CangJieLexer()
    lexer.start(this)
    return if (lexer.tokenEnd == length) lexer.tokenType else null
}


const val CJ_RAW_PREFIX = "r#"
private val CAN_NOT_BE_ESCAPED = listOf("self", "super", "crate", "Self")

val String.canBeEscaped: Boolean
    get() = this !in CAN_NOT_BE_ESCAPED && !CAN_NOT_BE_ESCAPED.any { this.startsWith("$it::") }
val RESERVED_KEYWORDS: Set<String> = setOf(
    "abstract",
    "become",
    "do",
    "final",
    "override",
    "priv",
    "typeof",
    "unsized",
    "virtual"
)

fun String.unescapeIdentifier(): String = removePrefix(CJ_RAW_PREFIX)
fun String.escapeIdentifierIfNeeded(): String =
    if (isValidCangJieVariableIdentifier(this) || !this.canBeEscaped) this else "$CJ_RAW_PREFIX$this"

val PsiElement.unescapedText: String
    get() {
        val text = text ?: return ""
        return if (elementType == IDENTIFIER) text.unescapeIdentifier() else text
    }

fun isValidCangJieVariableIdentifier(name: String): Boolean =
    name.getCangJieLexerTokenType() == IDENTIFIER && name !in RESERVED_KEYWORDS
