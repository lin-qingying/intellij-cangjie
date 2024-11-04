

package com.linqingying.cangjie.ide.formatter.util

import com.linqingying.cangjie.ide.formatter.CangJieCodeStyleSettings
import com.linqingying.cangjie.psi.CjElement
import com.intellij.psi.PsiElement


class TrailingCommaContext private constructor(val element: PsiElement, val state: TrailingCommaState) {

    val cjElement: CjElement get() = element as? CjElement ?: error("State is NOT_APPLICABLE")

    companion object {
        fun create(element: PsiElement): TrailingCommaContext = TrailingCommaContext(
            element,
            TrailingCommaState.stateForElement(element),
        )
    }
}

fun TrailingCommaContext.commaExistsOrMayExist(settings: CangJieCodeStyleSettings): Boolean = when (state) {
    TrailingCommaState.EXISTS -> true
    TrailingCommaState.MISSING -> settings.addTrailingCommaIsAllowedFor(element)
    else -> false
}
