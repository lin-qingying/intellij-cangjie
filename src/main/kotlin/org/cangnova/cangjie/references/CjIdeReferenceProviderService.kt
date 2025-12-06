/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.references

import org.cangnova.cangjie.psi.CangJieReferenceProvidersService
import com.intellij.util.SmartList
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.psi.ContributedReferenceHost
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceService
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.ConcurrentFactoryMap
import com.intellij.util.containers.MultiMap


internal class CjIdeReferenceProviderService(project: Project) : CangJieReferenceProvidersService() {
    private val originalProvidersBinding: MultiMap<Class<out PsiElement>, CangJiePsiReferenceProvider>
    private val providersBindingCache: Map<Class<out PsiElement>, List<CangJiePsiReferenceProvider>>

    init {
        val registrar = CangJiePsiReferenceRegistrar()
        CangJieReferenceProviderContributor.getInstance(project).registerReferenceProviders(registrar)
        originalProvidersBinding = registrar.providers

        providersBindingCache =
            ConcurrentFactoryMap.createMap<Class<out PsiElement>, List<CangJiePsiReferenceProvider>> { klass ->
                val result = SmartList<CangJiePsiReferenceProvider>()
                for (bindingClass in originalProvidersBinding.keySet()) {
                    if (bindingClass.isAssignableFrom(klass)) {
                        result.addAll(originalProvidersBinding.get(bindingClass))
                    }
                }
                result
            }
    }

    private fun doGetCangJieReferencesFromProviders(context: PsiElement): Array<PsiReference> {
        val providers: List<CangJiePsiReferenceProvider>? = providersBindingCache[context.javaClass]
        if (providers.isNullOrEmpty()) return PsiReference.EMPTY_ARRAY

        val result = SmartList<PsiReference>()
        for (provider in providers) {
            try {
                result.addAll(provider.getReferencesByElement(context))
            } catch (ignored: IndexNotReadyException) {
                // Ignore and continue to next provider
            }
        }

        if (result.isEmpty()) {
            return PsiReference.EMPTY_ARRAY
        }

        return result.toTypedArray()
    }

    override fun getReferences(psiElement: PsiElement): Array<PsiReference> {
        if (psiElement is ContributedReferenceHost) {
            return ReferenceProvidersRegistry.getReferencesFromProviders(psiElement, PsiReferenceService.Hints.NO_HINTS)
        }

        return CachedValuesManager.getCachedValue(psiElement) {
            CachedValueProvider.Result.create(
                doGetCangJieReferencesFromProviders(psiElement),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }
    }
}
