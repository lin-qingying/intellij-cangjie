

package com.huawei.cangjie.idea.formatter.util

import com.huawei.cangjie.idea.formatter.CangJieCodeStyleSettings
import com.huawei.cangjie.psi.CjElement
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
