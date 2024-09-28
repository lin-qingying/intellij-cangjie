

package com.huawei.cangjie.ide.formatter.util

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.ide.formatter.CangJieCodeStyleSettings
import com.huawei.cangjie.psi.CjFunctionLiteral
import com.huawei.cangjie.psi.CjMatchEntry
import com.huawei.cangjie.psi.CjMatchExpression
import com.huawei.cangjie.utils.cast
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiUtilCore


fun trailingCommaIsAllowedOnCallSite(): Boolean = Registry.`is`("cangjie.formatter.allowTrailingCommaOnCallSite")

private val TYPES_WITH_TRAILING_COMMA_ON_DECLARATION_SITE = TokenSet.create(
    CjNodeTypes.TYPE_PARAMETER_LIST,
    CjNodeTypes.DESTRUCTURING_DECLARATION,
    CjNodeTypes.MATCH_ENTRY,
    CjNodeTypes.FUNCTION_LITERAL,
    CjNodeTypes.VALUE_PARAMETER_LIST,
)

private val TYPES_WITH_TRAILING_COMMA_ON_CALL_SITE = TokenSet.create(
    CjNodeTypes.COLLECTION_LITERAL_EXPRESSION,
    CjNodeTypes.TYPE_ARGUMENT_LIST,
    CjNodeTypes.INDICES,
    CjNodeTypes.VALUE_ARGUMENT_LIST,
)

private val TYPES_WITH_TRAILING_COMMA = TokenSet.orSet(
    TYPES_WITH_TRAILING_COMMA_ON_DECLARATION_SITE,
    TYPES_WITH_TRAILING_COMMA_ON_CALL_SITE,
)

fun PsiElement.canAddTrailingCommaWithRegistryCheck(): Boolean {
    val type = PsiUtilCore.getElementType(this) ?: return false
    return type in TYPES_WITH_TRAILING_COMMA_ON_DECLARATION_SITE ||
            trailingCommaIsAllowedOnCallSite() && type in TYPES_WITH_TRAILING_COMMA_ON_CALL_SITE
}

fun CangJieCodeStyleSettings.addTrailingCommaIsAllowedFor(node: ASTNode): Boolean =
    addTrailingCommaIsAllowedFor(PsiUtilCore.getElementType(node))

fun CangJieCodeStyleSettings.addTrailingCommaIsAllowedFor(element: PsiElement): Boolean =
    addTrailingCommaIsAllowedFor(PsiUtilCore.getElementType(element))

private fun CangJieCodeStyleSettings.addTrailingCommaIsAllowedFor(type: IElementType?): Boolean = when (type) {
    null -> false
    in TYPES_WITH_TRAILING_COMMA_ON_DECLARATION_SITE -> ALLOW_TRAILING_COMMA
    in TYPES_WITH_TRAILING_COMMA_ON_CALL_SITE -> ALLOW_TRAILING_COMMA_ON_CALL_SITE || trailingCommaIsAllowedOnCallSite()
    else -> false
}

fun PsiElement.canAddTrailingComma(): Boolean = when {
    this is CjMatchEntry && (isElse || parent.cast<CjMatchExpression>().leftParenthesis == null) -> false
    this is CjFunctionLiteral && arrow == null -> false
    else -> PsiUtilCore.getElementType(this) in TYPES_WITH_TRAILING_COMMA
}
