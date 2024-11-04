package com.linqingying.cangjie.ide.quickfix

import com.linqingying.cangjie.lexer.CjModifierKeywordToken
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

interface QuickFixFactory


interface UnresolvedReferenceQuickFixFactory : QuickFixFactory

fun interface PsiElementSuitabilityChecker<in PSI : PsiElement> {
    fun isSupported(psiElement: PSI): Boolean
}

abstract class QuickFixesPsiBasedFactory<PSI : PsiElement>(
    private val classTag: KClass<PSI>,
    private val suitabilityChecker: PsiElementSuitabilityChecker<PSI>,
) : QuickFixFactory {
    fun createQuickFix(psiElement: PsiElement): List<IntentionAction> {
        checkIfPsiElementIsSupported(psiElement)
        @Suppress("UNCHECKED_CAST")
        return doCreateQuickFix(psiElement as PSI)
    }

    private fun checkIfPsiElementIsSupported(psiElement: PsiElement) {
        if (!psiElement::class.isSubclassOf(classTag)) {
            throw InvalidPsiElementTypeException(
                expectedPsiType = psiElement::class,
                actualPsiType = classTag,
                factoryName = this::class.toString()
            )
        }

        @Suppress("UNCHECKED_CAST")
        if (!suitabilityChecker.isSupported(psiElement as PSI)) {
            throw UnsupportedPsiElementException(psiElement, this::class.toString())
        }
    }

    protected abstract fun doCreateQuickFix(psiElement: PSI): List<IntentionAction>
}

class UnsupportedPsiElementException(
    psiElement: PsiElement,
    factoryName: String
) : Exception("PsiElement $psiElement is unsopported for $factoryName")

class InvalidPsiElementTypeException(
    expectedPsiType: KClass<out PsiElement>,
    actualPsiType: KClass<out PsiElement>,
    factoryName: String,
) : Exception("PsiElement with type $expectedPsiType is expected but $actualPsiType found for $factoryName")

inline fun <reified PSI : PsiElement> quickFixesPsiBasedFactory(
    suitabilityChecker: PsiElementSuitabilityChecker<PSI> = PsiElementSuitabilityCheckers.ALWAYS_SUITABLE,
    crossinline createQuickFix: (PSI) -> List<IntentionAction>,
): QuickFixesPsiBasedFactory<PSI> {
    return object : QuickFixesPsiBasedFactory<PSI>(PSI::class, suitabilityChecker) {
        override fun doCreateQuickFix(psiElement: PSI): List<IntentionAction> = createQuickFix(psiElement)
    }
}

object PsiElementSuitabilityCheckers {
    val ALWAYS_SUITABLE = PsiElementSuitabilityChecker<PsiElement> { true }

    val MODIFIER = PsiElementSuitabilityChecker<LeafPsiElement> { psiElement ->
        psiElement.elementType is CjModifierKeywordToken
    }
}

inline fun <reified PSI : PsiElement, reified PSI2 : PsiElement> QuickFixesPsiBasedFactory<PSI>.coMap(
    suitabilityChecker: PsiElementSuitabilityChecker<PSI2> = PsiElementSuitabilityCheckers.ALWAYS_SUITABLE,
    crossinline map: (PSI2) -> PSI?
): QuickFixesPsiBasedFactory<PSI2> {
    return quickFixesPsiBasedFactory(suitabilityChecker) { psiElement ->
        val newPsi = map(psiElement) ?: return@quickFixesPsiBasedFactory emptyList()
        createQuickFix(newPsi)
    }
}
