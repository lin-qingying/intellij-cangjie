package com.huawei.cangjie.ide.searching.usages.handlers

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.ide.search.isImportUsage
import com.huawei.cangjie.ide.searching.findUsages.CangJieFindUsagesSupport
import com.huawei.cangjie.ide.searching.usages.CangJieCallableFindUsagesOptions
import com.huawei.cangjie.ide.searching.usages.CangJieFindUsagesHandlerFactory
import com.huawei.cangjie.ide.searching.usages.CangJieFunctionFindUsagesOptions
import com.huawei.cangjie.ide.stubindex.resolve.isUnitTestMode
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.utils.match
import com.huawei.cangjie.utils.parents
import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector
import com.intellij.find.findUsages.AbstractFindUsagesDialog
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageInfo
import com.intellij.util.*


abstract class CangJieFindMemberUsagesHandler<T : CjNamedDeclaration> protected constructor(
    declaration: T,
    elementsToSearch: Collection<PsiElement>,
    factory: CangJieFindUsagesHandlerFactory
) : CangJieFindUsagesHandler<T>(declaration, elementsToSearch, factory)
{
    override fun createSearcher(
        element: PsiElement,
        processor: Processor<in UsageInfo>,
        options: FindUsagesOptions
    ): Searcher {
        return MySearcher(element, processor, options)
    }

    private inner class MySearcher(
        element: PsiElement, processor: Processor<in UsageInfo>, options: FindUsagesOptions
    ) : Searcher(element, processor, options) {

        private val cangjieOptions = options as CangJieCallableFindUsagesOptions

        override fun buildTaskList(forHighlight: Boolean): Boolean {
//            val referenceProcessor = createReferenceProcessor(processor)
//            val uniqueProcessor = CommonProcessors.UniqueProcessor(processor)
//
//            if (options.isUsages) {
//                val baseCangJieSearchOptions = createCangJieReferencesSearchOptions(options, forHighlight)
//                val cangjieSearchOptions = if (element is CjNamedFunction && CangJiePsiHeuristics.isPossibleOperator(element)) {
//                    baseCangJieSearchOptions
//                } else {
//                    baseCangJieSearchOptions.copy(searchForOperatorConventions = false)
//                }
//
//                val searchParameters = CangJieReferencesSearchParameters(element, options.searchScope, cangjieOptions = cangjieSearchOptions)
//
//                addTask { applyQueryFilters(element, options, ReferencesSearch.search(searchParameters)).forEach(referenceProcessor) }
//
//                if (element is CjElement && !isOnlyCangJieSearch(options.searchScope)) {
//                    // TODO: very bad code!! ReferencesSearch does not work correctly for constructors and annotation parameters
//                    val psiMethodScopeSearch = when {
//                        element is CjParameter && element.dataClassComponentMethodName != null ->
//                            options.searchScope.excludeCangJieSources(project)
//                        else -> options.searchScope
//                    }
//
//                    for (psiMethod in element.toLightMethods().filterDataClassComponentsIfDisabled(cangjieSearchOptions)) {
//                        addTask {
//                            val query = MethodReferencesSearch.search(psiMethod, psiMethodScopeSearch, true)
//                            applyQueryFilters(
//                                element,
//                                options,
//                                query
//                            ).forEach(referenceProcessor)
//                        }
//                    }
//                }
//            }
//
//            if (cangjieOptions.searchOverrides) {
//                addTask {
//                    val overriders = HierarchySearchRequest(element, options.searchScope, true).searchOverriders()
//                    overriders.all {
//                        val element = runReadAction { it.takeIf { it.isValid }?.navigationElement } ?: return@all true
//                        processUsage(uniqueProcessor, element)
//                    }
//                }
//            }

            return true
        }
    }
    private class Function(
        declaration: CjFunction,
        elementsToSearch: Collection<PsiElement>,
        factory: CangJieFindUsagesHandlerFactory
    ) : CangJieFindMemberUsagesHandler<CjFunction>(declaration, elementsToSearch, factory) {

        override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions = factory.findFunctionOptions

//        override fun getPrimaryElements(): Array<PsiElement> =
//            if (factory.findFunctionOptions.isSearchForBaseMethod) {
//                val supers = CangJieFindUsagesSupport.getSuperMethods(psiElement as CjFunction, null)
//                if (supers.contains(psiElement)) supers.toTypedArray() else (supers + psiElement).toTypedArray()
//            } else super.getPrimaryElements()

        override fun getFindUsagesDialog(
            isSingleFile: Boolean,
            toShowInNewTab: Boolean,
            mustOpenInNewTab: Boolean
        ): AbstractFindUsagesDialog {
//            val options = factory.findFunctionOptions
//            val lightMethod = getElement().toLightMethods().firstOrNull()
//            if (lightMethod != null) {
//                return CangJieFindFunctionUsagesDialog(lightMethod, project, options, toShowInNewTab, mustOpenInNewTab, isSingleFile, this)
//            }

            return super.getFindUsagesDialog(isSingleFile, toShowInNewTab, mustOpenInNewTab)
        }

//        override fun createCangJieReferencesSearchOptions(options: FindUsagesOptions, forHighlight: Boolean): CangJieReferencesSearchOptions {
//            val cangjieOptions = options as CangJieFunctionFindUsagesOptions
//            return CangJieReferencesSearchOptions(
//                acceptCallableOverrides = true,
//                acceptOverloads = cangjieOptions.isIncludeOverloadUsages,
//                acceptExtensionsOfDeclarationClass = cangjieOptions.isIncludeOverloadUsages,
//                searchForExpectedUsages = cangjieOptions.searchExpected,
//                searchForComponentConventions = !forHighlight
//            )
//        }

//        override fun applyQueryFilters(element: PsiElement, options: FindUsagesOptions, query: Query<PsiReference>): Query<PsiReference> {
//            val cangjieOptions = options as CangJieFunctionFindUsagesOptions
//            return query
//                .applyFilter(/*cangjieOptions.isSkipImportStatements*/true) { !it.isImportUsage() }
//        }
    }

    private class Property(
        propertyDeclaration: CjNamedDeclaration,
        elementsToSearch: Collection<PsiElement>,
        factory: CangJieFindUsagesHandlerFactory
    ) : CangJieFindMemberUsagesHandler<CjNamedDeclaration>(propertyDeclaration, elementsToSearch, factory) {
//
//        override fun processElementUsages(
//            element: PsiElement,
//            processor: Processor<in UsageInfo>,
//            options: FindUsagesOptions
//        ): Boolean {
//
//            if (isUnitTestMode() ||
//                !isPropertyOfDataClass ||
//                psiElement.getDisableComponentAndDestructionSearch(resetSingleFind = false)
//            ) return super.processElementUsages(element, processor, options)
//
//            val indicator = ProgressManager.getInstance().progressIndicator
//
//            val notificationCanceller = scheduleNotificationForDataClassComponent(project, element, indicator)
//            try {
//                return super.processElementUsages(element, processor, options)
//            } finally {
//                Disposer.dispose(notificationCanceller)
//            }
//        }

//        private val isPropertyOfDataClass = true == runReadAction {
//            propertyDeclaration.parents.match(CjParameterList::class, CjPrimaryConstructor::class, last = CjClass::class)?.isData()
//        }

//        override fun getPrimaryElements(): Array<PsiElement> {
//            val element = psiElement as CjNamedDeclaration
//            if (element is CjParameter && !element.hasValOrVar() && factory.findPropertyOptions.isSearchInOverridingMethods) {
//                return ActionUtil.underModalProgress(project, CangJieBundle.message("find.usages.progress.text.declaration.superMethods")) { getPrimaryElementsUnderProgress(element) }
//            } else if (factory.findPropertyOptions.isSearchForBaseAccessors) {
//                val supers = CangJieFindUsagesSupport.getSuperMethods(element, null)
//                return if (supers.contains(psiElement)) supers.toTypedArray() else (supers + psiElement).toTypedArray()
//            }
//
//            return super.getPrimaryElements()
//        }

//        private fun getPrimaryElementsUnderProgress(element: CjParameter): Array<PsiElement> {
//            val function = element.ownerFunction
//            if (function != null && function.isOverridable()) {
//                function.toLightMethods().singleOrNull()?.let { method ->
//                    if (OverridingMethodsSearch.search(method).any()) {
//                        val parametersCount = method.parameterList.parametersCount
//                        val parameterIndex = element.parameterIndex()
//
//                        assert(parameterIndex < parametersCount)
//                        return super.getPrimaryElements() + OverridingMethodsSearch.search(method, true)
//                            .filter { it.parameterList.parametersCount == parametersCount }
//                            .mapNotNull { it.parameterList.parameters[parameterIndex].unwrapped }
//                            .toTypedArray()
//                    }
//                }
//            }
//            return super.getPrimaryElements()
//        }

//        override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions = factory.findPropertyOptions
//
//        override fun getFindUsagesDialog(
//            isSingleFile: Boolean,
//            toShowInNewTab: Boolean,
//            mustOpenInNewTab: Boolean
//        ): AbstractFindUsagesDialog {
//            return CangJieFindPropertyUsagesDialog(
//                getElement(),
//                project,
//                factory.findPropertyOptions,
//                toShowInNewTab,
//                mustOpenInNewTab,
//                isSingleFile,
//                this
//            )
//        }

//        override fun applyQueryFilters(element: PsiElement, options: FindUsagesOptions, query: Query<PsiReference>): Query<PsiReference> {
//            val cangjieOptions = options as CangJiePropertyFindUsagesOptions
//
//            if (!cangjieOptions.isReadAccess && !cangjieOptions.isWriteAccess) {
//                return EmptyQuery()
//            }
//
//            val result = query.applyFilter(cangjieOptions.isSkipImportStatements) { !it.isImportUsage() }
//
//            if (!cangjieOptions.isReadAccess || !cangjieOptions.isWriteAccess) {
//                val detector = CangJieReadWriteAccessDetector()
//
//                return FilteredQuery(result) {
//                    when (detector.getReferenceAccess(element, it)) {
//                        ReadWriteAccessDetector.Access.Read -> cangjieOptions.isReadAccess
//                        ReadWriteAccessDetector.Access.Write -> cangjieOptions.isWriteAccess
//                        ReadWriteAccessDetector.Access.ReadWrite -> cangjieOptions.isReadWriteAccess
//                    }
//                }
//            }
//            return result
//        }

//        private fun PsiElement.getDisableComponentAndDestructionSearch(resetSingleFind: Boolean): Boolean {
//
//            if (!isPropertyOfDataClass) return false
//
//            if (forceDisableComponentAndDestructionSearch) return true
//
//            if ( CangJieFindPropertyUsagesDialog.getDisableComponentAndDestructionSearch(project)) return true
//
//            return if (getUserData(FIND_USAGES_ONES_FOR_DATA_CLASS_KEY) == true) {
//                if (resetSingleFind) {
//                    putUserData(FIND_USAGES_ONES_FOR_DATA_CLASS_KEY, null)
//                }
//                true
//            } else false
//        }


//        override fun createCangJieReferencesSearchOptions(options: FindUsagesOptions, forHighlight: Boolean): CangJieReferencesSearchOptions {
//            val cangjieOptions = options as CangJiePropertyFindUsagesOptions
//
//            val disabledComponentsAndOperatorsSearch =
//                !forHighlight && psiElement.getDisableComponentAndDestructionSearch(resetSingleFind = true)
//
//            return CangJieReferencesSearchOptions(
//                acceptCallableOverrides = true,
//                acceptOverloads = false,
//                acceptExtensionsOfDeclarationClass = false,
//                searchForExpectedUsages = cangjieOptions.searchExpected,
//                searchForOperatorConventions = !disabledComponentsAndOperatorsSearch,
//                searchForComponentConventions = !disabledComponentsAndOperatorsSearch
//            )
//        }
    }

//    protected abstract fun createCangJieReferencesSearchOptions(
//        options: FindUsagesOptions,
//        forHighlight: Boolean
//    ): CangJieReferencesSearchOptions
//
//    protected abstract fun applyQueryFilters(
//        element: PsiElement,
//        options: FindUsagesOptions,
//        query: Query<PsiReference>
//    ): Query<PsiReference>
    companion object{
        fun getInstance(
            declaration: CjNamedDeclaration,
            elementsToSearch: Collection<PsiElement> = emptyList(),
            factory: CangJieFindUsagesHandlerFactory
        ): CangJieFindMemberUsagesHandler<out CjNamedDeclaration> {
            return if (declaration is CjFunction)
                Function(declaration, elementsToSearch, factory)
            else
                Property(declaration, elementsToSearch, factory)
        }
    }
}
fun Query<PsiReference>.applyFilter(flag: Boolean, condition: (PsiReference) -> Boolean): Query<PsiReference> {
    return if (flag) FilteredQuery(this, condition) else this
}
