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

package com.linqingying.cangjie.ide.search.ideExtensions

import com.linqingying.cangjie.ide.allScope
import com.linqingying.cangjie.highlighter.unwrapped
import com.linqingying.cangjie.ide.search.ideExtensions.CangJieReferencesSearchOptions.Companion.Empty
import com.linqingying.cangjie.psi.CjDestructuringDeclaration
import com.linqingying.cangjie.psi.CjNamedDeclaration
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceService
import com.intellij.psi.ReferenceRange
import com.intellij.psi.search.RequestResultProcessor
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor


class CangJieRequestResultProcessor(
    private val unwrappedElement: PsiElement,
    private val originalElement: PsiElement = unwrappedElement,
    private val filter: (PsiReference) -> Boolean = { true },
    private val options: CangJieReferencesSearchOptions = Empty
) : RequestResultProcessor(unwrappedElement, originalElement, filter, options) {
    private val referenceService = PsiReferenceService.getService()

    override fun processTextOccurrence(element: PsiElement, offsetInElement: Int, consumer: Processor<in PsiReference>): Boolean {
        val references = if (element is CjDestructuringDeclaration)
            element.entries.flatMap { referenceService.getReferences(it, PsiReferenceService.Hints.NO_HINTS) }
        else
            referenceService.getReferences(element, PsiReferenceService.Hints.NO_HINTS)
        return references.all { ref ->
            ProgressManager.checkCanceled()

            if (filter(ref) && ref.containsOffsetInElement(offsetInElement) && ref.isReferenceToTarget(unwrappedElement)) {
                consumer.process(ref)
            } else {
                true
            }
        }
    }

    private fun PsiReference.containsOffsetInElement(offsetInElement: Int): Boolean {
//        if (this is CjDestructuringDeclarationReference) return true
        return ReferenceRange.containsOffsetInElement(this, offsetInElement)
    }

    private fun PsiReference.isReferenceToTarget(element: PsiElement): Boolean {
        if (isReferenceTo(element)) {
            return true
        }
        if (resolve()?.unwrapped == element.originalElement) {
            return true
        }
        if (originalElement is CjNamedDeclaration) {
//            if (isInvokeOfCompanionObject(originalElement)) {
//                return true
//            }
//            if (options.acceptCallableOverrides && isCallableOverrideUsage(originalElement)) {
//                return true
//            }
//            if (options.acceptOverloads && originalElement is CjFunction && isUsageInContainingDeclaration(originalElement)) {
//                return true
//            }
//            if (options.acceptExtensionsOfDeclarationClass && isExtensionOfDeclarationClassUsage(originalElement)) {
//                return true
//            }
        }
        return false
    }
}
class CangJieReferencesSearchParameters(
    elementToSearch: PsiElement,
    scope: SearchScope = runReadAction { elementToSearch.project.allScope() },
    ignoreAccessScope: Boolean = false,
    optimizer: SearchRequestCollector? = null,
    override val cangjieOptions: CangJieReferencesSearchOptions = Empty
) : ReferencesSearch.SearchParameters(elementToSearch, scope, ignoreAccessScope, optimizer), CangJieAwareReferencesSearchParameters
