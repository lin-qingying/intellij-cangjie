package com.huawei.cangjie.idea.references

import com.huawei.cangjie.psi.CangJieReferenceProvidersService
import com.huawei.cangjie.utils.SmartList
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



class CjIdeReferenceProviderService(project: Project) : CangJieReferenceProvidersService() {
    private val originalProvidersBinding: MultiMap<Class<out PsiElement>, CangJiePsiReferenceProvider>
    private val providersBindingCache: Map<Class<out PsiElement>, List<CangJiePsiReferenceProvider>>

    init {
        val registrar = CangJiePsiReferenceRegistrar()
        CangJieReferenceProviderContributor.getInstance(project).registerReferenceProviders(registrar)
        originalProvidersBinding = registrar.providers

        providersBindingCache = ConcurrentFactoryMap.createMap<Class<out PsiElement>, List<CangJiePsiReferenceProvider>> { klass ->
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
