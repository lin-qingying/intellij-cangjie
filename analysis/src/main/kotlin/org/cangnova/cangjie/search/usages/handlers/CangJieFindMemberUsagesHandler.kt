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

package org.cangnova.cangjie.search.usages.handlers
import org.cangnova.cangjie.messages.CangJieSearchBundle

import org.cangnova.cangjie.search.findUsages.CangJieFindUsagesSupport
import cn.cangnova.cangjie.ide.searching.usages.CangJieCallableFindUsagesOptions
import cn.cangnova.cangjie.ide.searching.usages.CangJieFunctionFindUsagesOptions
import cn.cangnova.cangjie.ide.searching.usages.CangJiePropertyFindUsagesOptions
import cn.cangnova.cangjie.ide.searching.usages.handlers.CangJieFindUsagesHandler
import com.intellij.find.FindManager
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.find.impl.FindManagerImpl
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageInfo
import com.intellij.util.*
import org.cangnova.cangjie.search.usages.CangJieFindUsagesHandlerFactory
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.search.CangJieSearchUsagesSupport
import org.cangnova.cangjie.search.ideExtensions.CangJieReferencesSearchOptions
import org.cangnova.cangjie.search.ideExtensions.CangJieReferencesSearchParameters
import org.cangnova.cangjie.search.isImportUsage
import org.cangnova.cangjie.utils.isUnitTestMode
import org.cangnova.cangjie.utils.runReadActionInSmartMode
import org.jetbrains.annotations.TestOnly


abstract class CangJieFindMemberUsagesHandler<T : CjNamedDeclaration> protected constructor(
    declaration: T,
    elementsToSearch: Collection<PsiElement>,
    factory: CangJieFindUsagesHandlerFactory
) : CangJieFindUsagesHandler<T>(declaration, elementsToSearch, factory) {
    override fun createSearcher(
        element: PsiElement,
        processor: Processor<in UsageInfo>,
        options: FindUsagesOptions
    ): Searcher {
        return MySearcher(element, processor, options)
    }

    private fun searchReferences(
        element: PsiElement,
        processor: Processor<in UsageInfo>,
        options: FindUsagesOptions,
        forHighlight: Boolean
    ): Boolean {
        val searcher = createSearcher(element, processor, options)
        if (!runReadAction {
                project
            }.runReadActionInSmartMode {
                searcher.buildTaskList(forHighlight)
            }
        ) return false
        return searcher.executeTasks()
    }

    override fun findReferencesToHighlight(target: PsiElement, searchScope: SearchScope): Collection<PsiReference> {

        val baseDeclarations = CangJieSearchUsagesSupport.SearchUtils.findDeepestSuperMethodsNoWrapping(target)

        return if (baseDeclarations.isNotEmpty()) {
            baseDeclarations.flatMap {
                val handler =
                    (FindManager.getInstance(project) as FindManagerImpl).findUsagesManager.getFindUsagesHandler(
                        it,
                        true
                    )
                handler?.findReferencesToHighlight(it, searchScope) ?: emptyList()
            }
        } else {
            super.findReferencesToHighlight(target, searchScope)
        }
    }

    private inner class MySearcher(
        element: PsiElement, processor: Processor<in UsageInfo>, options: FindUsagesOptions
    ) : Searcher(element, processor, options) {

        private val cangjieOptions = options as CangJieCallableFindUsagesOptions

        override fun buildTaskList(forHighlight: Boolean): Boolean {
            val referenceProcessor = createReferenceProcessor(processor)
            val uniqueProcessor = CommonProcessors.UniqueProcessor(processor)

            if (options.isUsages) {
                val baseCangJieSearchOptions = createCangJieReferencesSearchOptions(options, forHighlight)
                val cangjieSearchOptions =
                    if (element is CjNamedFunction && CangJiePsiHeuristics.isPossibleOperator(element)) {
                        baseCangJieSearchOptions
                    } else {
                        baseCangJieSearchOptions.copy(searchForOperatorConventions = false)
                    }

                val searchParameters = CangJieReferencesSearchParameters(
                    element,
                    options.searchScope,
                    cangjieOptions = cangjieSearchOptions
                )

                addTask {
                    applyQueryFilters(element, options, ReferencesSearch.search(searchParameters)).forEach(
                        referenceProcessor
                    )
                }


            }
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

        override fun getPrimaryElements(): Array<PsiElement> =
            if (factory.findFunctionOptions.isSearchForBaseMethod) {
                val supers = CangJieFindUsagesSupport.getSuperMethods(psiElement as CjFunction, null)
                if (supers.contains(psiElement)) supers.toTypedArray() else (supers + psiElement).toTypedArray()
            } else super.getPrimaryElements()

        override fun createCangJieReferencesSearchOptions(
            options: FindUsagesOptions,
            forHighlight: Boolean
        ): CangJieReferencesSearchOptions {
            val cangjieOptions = options as CangJieFunctionFindUsagesOptions
            return CangJieReferencesSearchOptions(
                acceptCallableOverrides = true,
                acceptOverloads = cangjieOptions.isIncludeOverloadUsages,
                acceptExtensionsOfDeclarationClass = cangjieOptions.isIncludeOverloadUsages,
                searchForExpectedUsages = cangjieOptions.searchExpected,
                searchForComponentConventions = !forHighlight
            )
        }

        override fun applyQueryFilters(
            element: PsiElement,
            options: FindUsagesOptions,
            query: Query<PsiReference>
        ): Query<PsiReference> {
            val cangjieOptions = options as CangJieFunctionFindUsagesOptions
            return query
                .applyFilter(/*cangjieOptions.isSkipImportStatements*/true) { !it.isImportUsage() }
        }
    }

    private class Property(
        propertyDeclaration: CjNamedDeclaration,
        elementsToSearch: Collection<PsiElement>,
        factory: CangJieFindUsagesHandlerFactory
    ) : CangJieFindMemberUsagesHandler<CjNamedDeclaration>(propertyDeclaration, elementsToSearch, factory) {
        //
        override fun processElementUsages(
            element: PsiElement,
            processor: Processor<in UsageInfo>,
            options: FindUsagesOptions
        ): Boolean {

            if (isUnitTestMode ||

                psiElement.getDisableComponentAndDestructionSearch(resetSingleFind = false)
            ) return super.processElementUsages(element, processor, options)

            val indicator = ProgressManager.getInstance().progressIndicator


            try {
                return super.processElementUsages(element, processor, options)
            } finally {

            }
        }


        override fun getPrimaryElements(): Array<PsiElement> {
            val element = psiElement as CjNamedDeclaration
            if (element is CjParameter && !element.hasLetOrVar() && factory.findPropertyOptions.isSearchInOverridingMethods) {
                return ActionUtil.underModalProgress(
                    project,
                    CangJieSearchBundle.message("find.usages.progress.text.declaration.superMethods")
                ) { getPrimaryElementsUnderProgress(element) }
            } else if (factory.findPropertyOptions.isSearchForBaseAccessors) {
                val supers = CangJieFindUsagesSupport.getSuperMethods(element, null)
                return if (supers.contains(psiElement)) supers.toTypedArray() else (supers + psiElement).toTypedArray()
            }

            return super.getPrimaryElements()
        }

        private fun getPrimaryElementsUnderProgress(element: CjParameter): Array<PsiElement> {
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
            return super.getPrimaryElements()
        }

        override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions = factory.findPropertyOptions

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

        override fun applyQueryFilters(
            element: PsiElement,
            options: FindUsagesOptions,
            query: Query<PsiReference>
        ): Query<PsiReference> {
            val cangjieOptions = options as CangJiePropertyFindUsagesOptions

            if (!cangjieOptions.isReadAccess && !cangjieOptions.isWriteAccess) {
                return EmptyQuery()
            }

            val result = query.applyFilter(cangjieOptions.isSkipImportStatements) { !it.isImportUsage() }

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
            return result
        }

        private fun PsiElement.getDisableComponentAndDestructionSearch(resetSingleFind: Boolean): Boolean {


            return forceDisableComponentAndDestructionSearch

//            if ( CangJieFindPropertyUsagesDialog.getDisableComponentAndDestructionSearch(project)) return true
        }


        override fun createCangJieReferencesSearchOptions(
            options: FindUsagesOptions,
            forHighlight: Boolean
        ): CangJieReferencesSearchOptions {
            val cangjieOptions = options as CangJiePropertyFindUsagesOptions

            val disabledComponentsAndOperatorsSearch =
                !forHighlight && psiElement.getDisableComponentAndDestructionSearch(resetSingleFind = true)

            return CangJieReferencesSearchOptions(
                acceptCallableOverrides = true,
                acceptOverloads = false,
                acceptExtensionsOfDeclarationClass = false,
                searchForExpectedUsages = cangjieOptions.searchExpected,
                searchForOperatorConventions = !disabledComponentsAndOperatorsSearch,
                searchForComponentConventions = !disabledComponentsAndOperatorsSearch
            )
        }
    }

    protected abstract fun createCangJieReferencesSearchOptions(
        options: FindUsagesOptions,
        forHighlight: Boolean
    ): CangJieReferencesSearchOptions

    protected abstract fun applyQueryFilters(
        element: PsiElement,
        options: FindUsagesOptions,
        query: Query<PsiReference>
    ): Query<PsiReference>

    companion object {
        @Volatile
        @get:TestOnly
        var forceDisableComponentAndDestructionSearch = false

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
