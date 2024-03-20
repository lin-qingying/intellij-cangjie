package com.huawei.cangjie.psi.psiUtil

import com.huawei.cangjie.lexer.CjModifierKeywordToken
import com.huawei.cangjie.lexer.CjTokens.*
import com.huawei.cangjie.psi.*
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace

inline fun <reified T : Any> Sequence<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}


fun addModifier(owner: CjModifierListOwner, modifier: CjModifierKeywordToken) {
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
        .dropWhile { it is PsiComment || it is PsiWhiteSpace || it is CjContextReceiverList }
        .first()
    return addBefore(newModifierList, anchor) as CjModifierList
}


private fun createModifierList(text: String, owner: CjModifierListOwner): CjModifierList {
    return owner.addModifierList(CjPsiFactory(owner.project).createModifierList(text))
}


internal fun addModifier(modifierList: CjModifierList, modifier: CjModifierKeywordToken) {
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

fun removeModifier(owner: CjModifierListOwner, modifier: CjModifierKeywordToken) {
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
    OPEN_KEYWORD to listOf( ABSTRACT_KEYWORD),

    PUBLIC_KEYWORD to listOf(PROTECTED_KEYWORD, PRIVATE_KEYWORD),
    PROTECTED_KEYWORD to listOf(PUBLIC_KEYWORD, PRIVATE_KEYWORD),
    PRIVATE_KEYWORD to listOf(PUBLIC_KEYWORD, PROTECTED_KEYWORD),


)
val MODIFIERS_ORDER = listOf(
    PUBLIC_KEYWORD, PROTECTED_KEYWORD, PRIVATE_KEYWORD, OPEN_KEYWORD, ABSTRACT_KEYWORD,

    OVERRIDE_KEYWORD,

    ENUM_KEYWORD, FUNC_KEYWORD, MAIN_KEYWORD


)
