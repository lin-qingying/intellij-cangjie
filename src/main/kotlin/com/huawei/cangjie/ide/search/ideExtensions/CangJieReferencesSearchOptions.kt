package com.huawei.cangjie.ide.search.ideExtensions


import com.huawei.cangjie.ide.searching.effectiveSearchScope
import com.huawei.cangjie.ide.searching.isOnlyCangJieSearch
import com.huawei.cangjie.psi.CjDeclaration
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch

data class CangJieReferencesSearchOptions(
    val acceptCallableOverrides: Boolean = false,
    val acceptOverloads: Boolean = false,
    val acceptExtensionsOfDeclarationClass: Boolean = false,
    val acceptCompanionObjectMembers: Boolean = false,
    val acceptImportAlias: Boolean = true,
    val searchForComponentConventions: Boolean = true,
    val searchForOperatorConventions: Boolean = true,
    val searchNamedArguments: Boolean = true,
    val searchForExpectedUsages: Boolean = true
) {
    fun anyEnabled(): Boolean = acceptCallableOverrides || acceptOverloads || acceptExtensionsOfDeclarationClass

    companion object {
        val Empty = CangJieReferencesSearchOptions()

        internal fun calculateEffectiveScope(
            elementToSearch: PsiNamedElement,
            parameters: ReferencesSearch.SearchParameters
        ): SearchScope {
            val cangjieOptions = (parameters as? CangJieAwareReferencesSearchParameters)?.cangjieOptions ?: Empty
            val elements = /*if (elementToSearch is CjDeclaration && !isOnlyCangJieSearch(parameters.scopeDeterminedByUser)) {
                elementToSearch.toLightElements()*//*.filterDataClassComponentsIfDisabled(cangjieOptions).nullize()*//*
            } else {
                null
            } ?: */listOf(elementToSearch)

            return elements.fold(parameters.effectiveSearchScope) { scope, e ->
                scope.union(parameters.effectiveSearchScope(e))
            }
        }
    }
}
interface CangJieAwareReferencesSearchParameters {
    val cangjieOptions: CangJieReferencesSearchOptions
}
