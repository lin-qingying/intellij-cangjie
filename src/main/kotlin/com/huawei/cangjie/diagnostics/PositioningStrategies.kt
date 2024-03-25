package com.huawei.cangjie.diagnostics

import com.huawei.cangjie.psi.CjArrayAccessExpression
import com.huawei.cangjie.psi.CjReferenceExpression
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

object PositioningStrategies {
    @JvmField
    val FOR_UNRESOLVED_REFERENCE: PositioningStrategy<CjReferenceExpression> =
        object : PositioningStrategy<CjReferenceExpression>() {
            override fun mark(element: CjReferenceExpression): List<TextRange> {
                if (element is CjArrayAccessExpression) {
                    val ranges = element.bracketRanges
                    if (ranges.isNotEmpty()) {
                        return ranges
                    }
                }
                return listOf(element.textRange)
            }
        }

    @JvmField
    val DEFAULT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            when (element) {

                else -> {
                    return super.mark(element)
                }
            }
        }
    }
}