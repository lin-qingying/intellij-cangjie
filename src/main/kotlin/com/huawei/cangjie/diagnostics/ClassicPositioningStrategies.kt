package com.huawei.cangjie.diagnostics

import com.huawei.cangjie.cfg.UnreachableCode
import com.huawei.cangjie.psi.CjElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

object ClassicPositioningStrategies {

    @JvmField
    val UNREACHABLE_CODE: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun markDiagnostic(diagnostic: DiagnosticMarker): List<TextRange> {
            @Suppress("UNCHECKED_CAST")
            val unreachableCode = diagnostic as DiagnosticWithParameters2Marker<Set<CjElement>, Set<CjElement>>
            return UnreachableCode.getUnreachableTextRanges(unreachableCode.psiElement as CjElement, unreachableCode.a, unreachableCode.b)
        }
    }
}
