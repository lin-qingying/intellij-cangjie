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

package cn.cangnova.cangjie.ide.search

import cn.cangnova.cangjie.ide.base.projectStructure.CangJieSourceFilterScope
import cn.cangnova.cangjie.highlighter.namedUnwrappedElement

import cn.cangnova.cangjie.ide.search.ideExtensions.CangJieAwareReferencesSearchParameters
import cn.cangnova.cangjie.ide.search.ideExtensions.CangJieReferencesSearchOptions.Companion.Empty
import cn.cangnova.cangjie.ide.search.ideExtensions.CangJieReferencesSearchOptions.Companion.calculateEffectiveScope
import cn.cangnova.cangjie.ide.search.ideExtensions.CangJieRequestResultProcessor
import cn.cangnova.cangjie.ide.search.operators.OperatorReferenceSearcher
import cn.cangnova.cangjie.ide.searching.effectiveSearchScope
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.references.CjSimpleNameReference
import cn.cangnova.cangjie.utils.safeAs
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.*
import com.intellij.psi.impl.cache.CacheManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import com.intellij.util.concurrency.annotations.RequiresReadLock
import java.util.concurrent.Callable

class CangJieReferencesSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>() {
    override fun processQuery(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ) {
        val processor = QueryProcessor(queryParameters, consumer)
        processor.process()
        processor.executeLongRunningTasks()
    }

    private class QueryProcessor(
        val queryParameters: ReferencesSearch.SearchParameters,
        val consumer: Processor<in PsiReference>
    ) {

        private val cangjieOptions =
            queryParameters.safeAs<CangJieAwareReferencesSearchParameters>()?.cangjieOptions ?: Empty

        private val longTasks = mutableListOf<() -> Unit>()

        fun executeLongRunningTasks() {
            longTasks.forEach {
                ProgressManager.checkCanceled()
                it()
            }
        }

        fun process() {
            var element: SmartPsiElementPointer<PsiElement>? = null

            val (elementToSearchPointer: SmartPsiElementPointer<PsiNamedElement>, effectiveSearchScope) = ReadAction.nonBlocking(
                Callable {
                    val psiElement = queryParameters.elementToSearch

                    if (!psiElement.isValid) return@Callable null

                    val unwrappedElement = psiElement.namedUnwrappedElement ?: return@Callable null

                    val elementToSearch =
                        if (cangjieOptions.searchForExpectedUsages && unwrappedElement is CjDeclaration) {
                            unwrappedElement
                        } else {
                            null
                        } ?: unwrappedElement

                    val effectiveSearchScope = calculateEffectiveScope(elementToSearch, queryParameters)

                    element = SmartPointerManager.createPointer(psiElement)

                    SmartPointerManager.createPointer(elementToSearch) to effectiveSearchScope
                }).executeSynchronously() ?: return

//            runReadAction { element?.element } ?: return

            runReadAction {
                elementToSearchPointer.element?.let { elementToSearch ->
                    val refFilter: (PsiReference) -> Boolean = when (elementToSearch) {
                        is CjParameter -> ({ ref: PsiReference -> !ref.isNamedArgumentReference()/* they are processed later*/ })
                        else -> ({ true })
                    }

                    val resultProcessor =
                        CangJieRequestResultProcessor(elementToSearch, filter = refFilter, options = cangjieOptions)
                    if (cangjieOptions.anyEnabled() || elementToSearch is CjNamedDeclaration) {
                        elementToSearch.name?.let { name ->
                            longTasks.add {
                                // Check difference with default scope
                                runReadAction { elementToSearchPointer.element }?.let { elementToSearch ->
                                    queryParameters.optimizer.searchWord(
                                        name,
                                        effectiveSearchScope,
                                        UsageSearchContext.IN_CODE,
                                        true,
                                        elementToSearch,
                                        resultProcessor
                                    )
                                }
                            }
                        }
                    }

//                    classNameForCompanionObject?.let { name ->
//                        longTasks.add {
//                            runReadAction { elementToSearchPointer.element }?.let { elementToSearch ->
//                                queryParameters.optimizer.searchWord(
//                                    name, effectiveSearchScope, UsageSearchContext.ANY, true, elementToSearch, resultProcessor
//                                )
//                            }
//                        }
//                    }
                }


                if (elementToSearchPointer.element is CjParameter && cangjieOptions.searchNamedArguments) {
                    longTasks.add {
                        ReadAction.nonBlocking(Callable {
                            elementToSearchPointer.element.safeAs<CjParameter>()?.let(::searchNamedArguments)
                        }).executeSynchronously()
                    }
                }

//                if (!(elementToSearchPointer.element is CjElement && runReadAction { isOnlyCangJieSearch(effectiveSearchScope) })) {
//                    longTasks.add {
//                        ReadAction.nonBlocking(Callable {
//                            element?.element?.let(::searchLightElements)
//                        }).executeSynchronously()
//                    }
//                }

                element?.element?.takeIf { it is CjFunction }?.let { _ ->
                    element?.element?.let {
                        OperatorReferenceSearcher.create(
                            it, effectiveSearchScope, consumer, queryParameters.optimizer, cangjieOptions
                        )
                    }
                        ?.let { searcher ->
                            longTasks.add { searcher.run() }
                        }
                }
            }

            if (cangjieOptions.searchForComponentConventions) {
                element?.let(::searchForComponentConventions)
            }
        }

        private fun searchNamedArguments(parameter: CjParameter) {
            val parameterName = parameter.name ?: return
            val function = parameter.ownerFunction as? CjFunction ?: return
            if (function.nameAsName?.isSpecial != false) return
            val project = function.project
            var namedArgsScope = function.useScope.intersectWith(queryParameters.scopeDeterminedByUser)

            if (namedArgsScope is GlobalSearchScope) {
                namedArgsScope = CangJieSourceFilterScope.everything(namedArgsScope, project)

                val filesWithFunctionName = CacheManager.getInstance(project).getVirtualFilesWithWord(
                    function.name!!, UsageSearchContext.IN_CODE, namedArgsScope, true
                )
                namedArgsScope = GlobalSearchScope.filesScope(project, filesWithFunctionName.asList())
            }

            val processor = CangJieRequestResultProcessor(parameter, filter = { it.isNamedArgumentReference() })
            queryParameters.optimizer.searchWord(
                parameterName,
                namedArgsScope,
                CANGJIE_NAMED_ARGUMENT_SEARCH_CONTEXT,
                true,
                parameter,
                processor
            )
        }


        private fun searchForComponentConventions(elementPointer: SmartPsiElementPointer<PsiElement>) {
            ReadAction.nonBlocking(Callable {
                when (val element = elementPointer.element) {


                    else -> return@Callable
                }
            }).executeSynchronously()
        }


        @RequiresReadLock
        private fun searchNamedElement(
            element: PsiNamedElement?,
            name: String? = null,
            modifyScope: ((SearchScope) -> SearchScope)? = null
        ) {
            assertReadAccessAllowed()
            element ?: return
            val nameToUse = name ?: element.name ?: return
            val baseScope = queryParameters.effectiveSearchScope(element)
            val scope = if (modifyScope != null) modifyScope(baseScope) else baseScope
            val context =
                UsageSearchContext.IN_CODE + UsageSearchContext.IN_FOREIGN_LANGUAGES + UsageSearchContext.IN_COMMENTS
            val resultProcessor = CangJieRequestResultProcessor(
                element,
                queryParameters.elementToSearch.namedUnwrappedElement ?: element,
                options = cangjieOptions
            )
            queryParameters.optimizer.searchWord(nameToUse, scope, context.toShort(), true, element, resultProcessor)
        }

        private fun assertReadAccessAllowed() {
            ApplicationManager.getApplication().assertReadAccessAllowed()
        }

        private fun PsiReference.isNamedArgumentReference(): Boolean {
            return this is CjSimpleNameReference && expression.parent is CjValueArgumentName
        }
    }

}
