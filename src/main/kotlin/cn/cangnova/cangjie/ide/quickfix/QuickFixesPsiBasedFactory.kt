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

package cn.cangnova.cangjie.ide.quickfix

import cn.cangnova.cangjie.lexer.CjModifierKeywordToken
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
