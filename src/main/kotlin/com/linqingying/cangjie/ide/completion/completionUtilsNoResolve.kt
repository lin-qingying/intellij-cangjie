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

package com.linqingying.cangjie.ide.completion

import com.linqingying.cangjie.ide.completion.back.or
import com.linqingying.cangjie.ide.completion.back.singleCharPattern
import com.linqingying.cangjie.ide.completion.handlers.WithTailInsertHandler
import com.linqingying.cangjie.ide.completion.keywords.KeywordLookupObject
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.*
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.util.Key
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.JBColor

fun cangjieIdentifierStartPattern(): ElementPattern<Char> =
    StandardPatterns.character().javaIdentifierStart().andNot(singleCharPattern('$'))

fun cangjieIdentifierPartPattern(): ElementPattern<Char> =
    StandardPatterns.character().javaIdentifierPart().andNot(singleCharPattern('$')) or singleCharPattern('@')

fun createKeywordElementWithSpace(
    keyword: String,
    tail: String = "",
    addSpaceAfter: Boolean = false,
    lookupObject: KeywordLookupObject = KeywordLookupObject()
): LookupElement {
    val element = createKeywordElement(keyword, tail, lookupObject)
    return if (addSpaceAfter) {
        element.withInsertHandler(WithTailInsertHandler.SPACE.asPostInsertHandler)
    } else {
        element
    }
}

fun isAtFunctionLiteralStart(position: PsiElement): Boolean {
    val lBrace = PsiTreeUtil.prevCodeLeaf(position)
        ?.let { if (it.node.elementType == CjTokens.LPAR) PsiTreeUtil.prevCodeLeaf(it) else it }
        ?.takeIf { it.node.elementType == CjTokens.LBRACE }

    val functionLiteral = lBrace?.parent as? CjFunctionLiteral ?: return false
    return functionLiteral.lBrace == lBrace
}

fun LookupElement.suppressAutoInsertion() = AutoCompletionPolicy.NEVER_AUTOCOMPLETE.applyPolicy(this)
val CANGJIE_CAST_REQUIRED_COLOR = JBColor(0x4E4040, 0x969696)
tailrec fun <T : Any> LookupElement.putUserDataDeep(key: Key<T>, value: T?) {
    if (this is LookupElementDecorator<*>) {
        delegate.putUserDataDeep(key, value)
    } else {
        putUserData(key, value)
    }
}

fun PrefixMatcher.asNameFilter(): NameFilter {
    return { name -> !name.isSpecial && prefixMatches(name.identifier) }
}
fun LookupElementPresentation.prependTailText(text: String, grayed: Boolean) {
    val tails = tailFragments.toList()
    clearTail()
    appendTailText(text, grayed)
    tails.forEach { appendTailText(it.text, it.isGrayed) }
}
enum class ItemPriority {
    SUPER_METHOD_WITH_ARGUMENTS,
    FROM_UNRESOLVED_NAME_SUGGESTION,
    GET_OPERATOR,
    DEFAULT,
    IMPLEMENT,
    OVERRIDE,
    STATIC_MEMBER_FROM_IMPORTS,
    STATIC_MEMBER
}

var LookupElement.priority by UserDataProperty(Key<ItemPriority>("ITEM_PRIORITY_KEY"))
fun referenceScope(declaration: CjNamedDeclaration): CjElement? = when (val parent = declaration.parent) {
    is CjParameterList -> parent.parent as CjElement
    is CjAbstractClassBody -> {
        val classOrObject = parent.parent as CjTypeStatement

        classOrObject

    }

    is CjFile -> parent
    is CjBlockExpression -> parent
    else -> null
}

fun createKeywordElement(
    keyword: String,
    tail: String = "",
    lookupObject: KeywordLookupObject = KeywordLookupObject()
): LookupElementBuilder {
    var element = LookupElementBuilder.create(lookupObject, keyword + tail)
    element = element.withPresentableText(keyword)
    element = element.withBoldness(true)
    if (tail.isNotEmpty()) {
        element = element.withTailText(tail, false)
    }
    return element
}
