/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.psi.psiUtil

import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjModifierKeywordToken
import org.cangnova.cangjie.lexer.CjTokens.*
import org.cangnova.cangjie.psi.*
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace

inline fun <reified T : Any> Sequence<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

fun addModifier(owner: CjModifierListOwner, modifier: CjKeywordToken) {
    val modifierList = owner.modifierList
    if (modifierList == null) {
        createModifierList(modifier.value, owner)
    } else {
        addModifier(modifierList, modifier)
    }
}

private fun CjModifierListOwner.addModifierList(newModifierList: CjModifierList): CjModifierList {
    val anchor = firstChild!!
        .siblings(forward = true)
        .dropWhile { it is PsiComment || it is PsiWhiteSpace  }
        .first()
    return addBefore(newModifierList, anchor) as CjModifierList
}

private fun createModifierList(text: String, owner: CjModifierListOwner): CjModifierList {
    return owner.addModifierList(CjPsiFactory(owner.project).createModifierList(text))
}

internal fun addModifier(modifierList: CjModifierList, modifier: CjKeywordToken) {
    if (modifierList.hasModifier(modifier)) return

    val newModifier = CjPsiFactory(modifierList.project).createModifier(modifier)
    val modifierToReplace = MODIFIERS_TO_REPLACE[modifier]
        ?.mapNotNull { modifierList.getModifier(it) }
        ?.firstOrNull()

    if (modifierToReplace != null && modifierList.firstChild == modifierList.lastChild) {
        modifierToReplace.replace(newModifier)
    } else {
        modifierToReplace?.delete()
        val newModifierOrder = MODIFIERS_ORDER.indexOf(modifier)

        fun placeAfter(child: PsiElement): Boolean {
            if (child is PsiWhiteSpace) return false

            val elementType = child.node!!.elementType
            val order = MODIFIERS_ORDER.indexOf(elementType)
            return newModifierOrder > order
        }

        val lastChild = modifierList.lastChild
        val anchor = lastChild?.siblings(forward = false)?.firstOrNull(::placeAfter).let {
            when {
                it?.nextSibling is PsiWhiteSpace && it is PsiComment -> it.nextSibling
                it == null && modifierList.firstChild is PsiWhiteSpace -> modifierList.firstChild
                else -> it
            }
        }
        modifierList.addAfter(newModifier, anchor)

        if (anchor == lastChild) { // add line break if needed, otherwise myVisibility keyword may appear on previous line
            val whiteSpace = modifierList.nextSibling as? PsiWhiteSpace
            if (whiteSpace != null && whiteSpace.text.contains('\n')) {
                modifierList.addAfter(whiteSpace, anchor)
                whiteSpace.delete()
            }
        }
    }
}

fun removeModifier(owner: CjModifierListOwner, modifier: CjKeywordToken) {
    owner.modifierList?.let {
        it.getModifier(modifier)?.delete()
        if (it.firstChild == null) {
            it.delete()
            return
        }

        val lastChild = it.lastChild
        if (lastChild is PsiComment) {
            it.addAfter(CjPsiFactory(owner.project).createNewLine(), lastChild)
        }
    }
}

fun sortModifiers(modifiers: List<CjModifierKeywordToken>): List<CjModifierKeywordToken> {
    return modifiers.sortedBy {
        val index = MODIFIERS_ORDER.indexOf(it)
        if (index == -1) Int.MAX_VALUE else index
    }
}

private val MODIFIERS_TO_REPLACE = mapOf(
    OVERRIDE_KEYWORD to listOf(OPEN_KEYWORD),
    ABSTRACT_KEYWORD to listOf(OPEN_KEYWORD),
    OPEN_KEYWORD to listOf(ABSTRACT_KEYWORD),
//    FINAL_KEYWORD to listOf(ABSTRACT_KEYWORD, OPEN_KEYWORD),
    PUBLIC_KEYWORD to listOf(PROTECTED_KEYWORD, PRIVATE_KEYWORD, INTERNAL_KEYWORD),
    PROTECTED_KEYWORD to listOf(PUBLIC_KEYWORD, PRIVATE_KEYWORD, INTERNAL_KEYWORD),
    PRIVATE_KEYWORD to listOf(PUBLIC_KEYWORD, PROTECTED_KEYWORD, INTERNAL_KEYWORD),
    INTERNAL_KEYWORD to listOf(PUBLIC_KEYWORD, PROTECTED_KEYWORD, PRIVATE_KEYWORD),

)
val MODIFIERS_ORDER = listOf(
    PUBLIC_KEYWORD, PROTECTED_KEYWORD, PRIVATE_KEYWORD, INTERNAL_KEYWORD,

    OPEN_KEYWORD, ABSTRACT_KEYWORD, SEALED_KEYWORD,
    CONST_KEYWORD,

    OVERRIDE_KEYWORD,

    ENUM_KEYWORD,

    OPERATOR_KEYWORD,

)
