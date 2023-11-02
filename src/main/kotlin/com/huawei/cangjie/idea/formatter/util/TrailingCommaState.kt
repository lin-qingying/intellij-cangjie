package com.huawei.cangjie.idea.formatter.util

import com.huawei.cangjie.idea.formatter.containsLineBreakInChild
import com.huawei.cangjie.idea.formatter.isMultiline
import com.huawei.cangjie.psi.CjDestructuringDeclaration
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFunctionLiteral
import com.huawei.cangjie.psi.CjMatchEntry
import com.huawei.cangjie.psi.psiUtil.endOffset
import com.huawei.cangjie.psi.psiUtil.startOffset
import com.intellij.psi.PsiElement

enum class TrailingCommaState {

    EXISTS,


    MISSING,



    NOT_EXISTS,


    REDUNDANT,


    NOT_APPLICABLE,
    ;

    companion object {
        fun stateForElement(element: PsiElement): TrailingCommaState = when {
            element !is CjElement || !element.canAddTrailingComma() -> NOT_APPLICABLE
            isMultiline(element) ->
                if (TrailingCommaHelper.trailingCommaExists(element))
                    EXISTS
                else
                    MISSING
            else ->
                if (TrailingCommaHelper.trailingCommaExists(element))
                    REDUNDANT
                else
                    NOT_EXISTS
        }
    }
}

private fun isMultiline(cjElement: CjElement): Boolean = when {
    cjElement.parent is CjFunctionLiteral -> isMultiline(cjElement.parent as CjElement)

    cjElement is CjFunctionLiteral -> cjElement.isMultiline(
        startOffsetGetter = { valueParameterList?.startOffset },
        endOffsetGetter = { arrow?.endOffset },
    )

    cjElement is CjMatchEntry -> cjElement.isMultiline(
        startOffsetGetter = { startOffset },
        endOffsetGetter = { arrow?.endOffset },
    )

    cjElement is CjDestructuringDeclaration -> cjElement.isMultiline(
        startOffsetGetter = { lPar?.startOffset },
        endOffsetGetter = { rPar?.endOffset },
    )

    else -> cjElement.isMultiline()
}

private fun <T : PsiElement> T.isMultiline(
    startOffsetGetter: T.() -> Int?,
    endOffsetGetter: T.() -> Int?,
): Boolean {
    val startOffset = startOffsetGetter() ?: startOffset
    val endOffset = endOffsetGetter() ?: endOffset
    return containsLineBreakInChild(startOffset, endOffset)
}
