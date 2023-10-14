package com.huawei.cangjie.lang.core.psi

import com.huawei.cangjie.ide.refactoring.isValidCangJieVariableIdentifier
import com.huawei.cangjie.lang.core.psi.CjElementTypes.IDENTIFIER
import com.huawei.cangjie.lang.core.psi.ext.elementType
import com.huawei.cangjie.lang.core.stubs.CjFileStub
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiUtilCore

val PsiElement.unescapedText: String get() {
    val text = text ?: return ""
    return if (elementType == IDENTIFIER) text.unescapeIdentifier() else text
}
fun String.unescapeIdentifier(): String = removePrefix(CJ_RAW_PREFIX)
const val CJ_RAW_PREFIX = "cj#"
private val CAN_NOT_BE_ESCAPED = listOf("super")
val String.canBeEscaped: Boolean
    get() = this !in CAN_NOT_BE_ESCAPED && !CAN_NOT_BE_ESCAPED.any { this.startsWith("$it::") }


fun String.escapeIdentifierIfNeeded(): String =
    if (isValidCangJieVariableIdentifier(this) || !this.canBeEscaped) this else "$CJ_RAW_PREFIX$this"
