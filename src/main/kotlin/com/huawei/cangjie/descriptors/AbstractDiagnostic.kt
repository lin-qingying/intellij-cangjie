package com.huawei.cangjie.descriptors

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import java.util.*

abstract class AbstractDiagnostic<E : PsiElement>(
    override val psiElement: E,
    factory: DiagnosticFactoryWithPsiElement<E, *>,
    severity: Severity
) :
    ParametrizedDiagnostic<E> {
    override val factory: DiagnosticFactoryWithPsiElement<E, *> = factory
    override val severity: Severity = severity


    override val psiFile: PsiFile
        get() = psiElement.containingFile



    override val textRanges: List<TextRange>
        get() = factory.getTextRanges(this)

    override val isValid: Boolean
        get() {
            return factory.isValid(this)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as AbstractDiagnostic<*>
        return psiElement == that.psiElement && factory == that.factory && severity == that.severity
    }

    override fun hashCode(): Int {
        return Objects.hash(psiElement, factory, severity)
    }
}
