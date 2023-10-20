package com.huawei.cangjie.ide.refactoring

import com.huawei.cangjie.lang.core.lexer.getCangJieLexerTokenType
import com.huawei.cangjie.lang.core.psi.CJSERVED_KEYWORDS
import com.huawei.cangjie.lang.core.psi.CJ_KEYWORDS
import com.huawei.cangjie.lang.core.psi.CjElementTypes.IDENTIFIER
import com.huawei.cangjie.lang.core.psi.CjElementTypes.QUOTE_IDENTIFIER
import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project


class CjNamesValidator : NamesValidator {

    override fun isKeyword(name: String, project: Project?): Boolean = isKeyword(name)

    override fun isIdentifier(name: String, project: Project?): Boolean = isIdentifier(name)

    companion object {
        val RESERVED_LIFETIME_NAMES: Set<String> = setOf("'static", "'_")

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

        fun isIdentifier(name: String): Boolean = when (name.getCangJieLexerTokenType()) {
            IDENTIFIER -> name !in RESERVED_KEYWORDS
            QUOTE_IDENTIFIER -> true
            else -> false
        }

        fun isKeyword(name: String): Boolean = name.getCangJieLexerTokenType() in CJ_KEYWORDS
    }
}

fun isValidCangJieVariableIdentifier(name: String): Boolean =
    name.getCangJieLexerTokenType() == IDENTIFIER && name !in CJSERVED_KEYWORDS
