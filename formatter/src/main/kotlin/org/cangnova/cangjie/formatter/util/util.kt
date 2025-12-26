/*
 * Copyright 2025 LinQingYing. and contributors.
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



package org.cangnova.cangjie.formatter.util

import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.formatter.CangJieCodeStyleSettings
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjMatchEntry
import org.cangnova.cangjie.psi.CjMatchExpression
import org.cangnova.cangjie.utils.cast
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiUtilCore


fun trailingCommaIsAllowedOnCallSite(): Boolean = Registry.`is`("cangjie.formatter.allowTrailingCommaOnCallSite")

private val TYPES_WITH_TRAILING_COMMA_ON_DECLARATION_SITE = TokenSet.create(
    CjNodeTypes.TYPE_PARAMETER_LIST,

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
