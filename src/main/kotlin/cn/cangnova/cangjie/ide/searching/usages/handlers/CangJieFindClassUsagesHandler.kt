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

package cn.cangnova.cangjie.ide.searching.usages.handlers

import cn.cangnova.cangjie.ide.search.ideExtensions.CangJieReferencesSearchOptions
import cn.cangnova.cangjie.ide.search.ideExtensions.CangJieReferencesSearchParameters
import cn.cangnova.cangjie.ide.search.isImportUsage
import cn.cangnova.cangjie.ide.searching.findUsages.CangJieFindUsagesSupport.Companion.isConstructorUsage
import cn.cangnova.cangjie.ide.searching.usages.CangJieClassFindUsagesOptions
import cn.cangnova.cangjie.ide.searching.usages.CangJieFindUsagesHandlerFactory
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.effectiveDeclarations
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageInfo
import com.intellij.util.FilteredQuery
import com.intellij.util.Processor


class CangJieFindClassUsagesHandler(
    cjClass: CjTypeStatement,
    factory: CangJieFindUsagesHandlerFactory
) : CangJieFindUsagesHandler<CjTypeStatement>(cjClass, factory) {
//    override fun getFindUsagesDialog(
//        isSingleFile: Boolean, toShowInNewTab: Boolean, mustOpenInNewTab: Boolean
//    ): AbstractFindUsagesDialog {
//        return CangJieFindClassUsagesDialog(
//            getElement(),
//            project,
//            factory.findClassOptions,
//            toShowInNewTab,
//            mustOpenInNewTab,
//            isSingleFile,
//            this
//        )
//    }

    override fun createSearcher(
        element: PsiElement,
        processor: Processor<in UsageInfo>,
        options: FindUsagesOptions
    ): Searcher {
        return MySearcher(element, processor, options)
    }

    private class MySearcher(
        element: PsiElement, processor: Processor<in UsageInfo>, options: FindUsagesOptions
    ) : Searcher(element, processor, options) {

        private val cangjieOptions = options as CangJieClassFindUsagesOptions
        private val referenceProcessor = createReferenceProcessor(processor)

        override fun buildTaskList(forHighlight: Boolean): Boolean {
            val classOrObject = element as CjTypeStatement

            if (cangjieOptions.isUsages || cangjieOptions.searchConstructorUsages) {
                processClassReferencesLater(classOrObject)
            }

            if (cangjieOptions.isFieldsUsages || cangjieOptions.isMethodsUsages) {
                processMemberReferencesLater(classOrObject)
            }




            if (cangjieOptions.isDerivedClasses || cangjieOptions.isDerivedInterfaces) {
                processInheritorsLater()
            }

            return true
        }

        private fun processInheritorsLater() {
//            val request = HierarchySearchRequest(element, options.searchScope, cangjieOptions.isCheckDeepInheritance)
//            addTask {
//                request.searchInheritors().forEach(
//                    PsiElementProcessorAdapter(
//                        PsiElementProcessor { element ->
//                            runReadAction {
//                                if (!element.isValid) return@runReadAction false
//                                val isInterface = element is CjInterface
//                                when {
//                                    isInterface && cangjieOptions.isDerivedInterfaces || !isInterface && cangjieOptions.isDerivedClasses ->
//                                        processUsage(processor, element.navigationElement)
//
//                                    else -> true
//                                }
//                            }
//                        }
//                    )
//                )
//            }
        }

        private fun processClassReferencesLater(classOrObject: CjTypeStatement) {
            val searchParameters = CangJieReferencesSearchParameters(
                classOrObject,
                scope = options.searchScope,
                cangjieOptions = CangJieReferencesSearchOptions(
                    acceptCompanionObjectMembers = true,
                    searchForExpectedUsages = cangjieOptions.searchExpected
                )
            )
            var usagesQuery = ReferencesSearch.search(searchParameters)

            if (cangjieOptions.isSkipImportStatements) {
                usagesQuery = FilteredQuery(usagesQuery) { !it.isImportUsage() }
            }

            if (!cangjieOptions.searchConstructorUsages) {
                usagesQuery = FilteredQuery(usagesQuery) { !it.isConstructorUsage(classOrObject) }
            } else if (!options.isUsages) {
                usagesQuery = FilteredQuery(usagesQuery) { it.isConstructorUsage(classOrObject) }
            }
            addTask { usagesQuery.forEach(referenceProcessor) }
        }

        private fun processMemberReferencesLater(classOrObject: CjTypeStatement) {
            for (declaration in classOrObject.effectiveDeclarations()) {
                if ((declaration is CjNamedFunction && cangjieOptions.isMethodsUsages) ||
                    ((declaration is CjProperty || declaration is CjParameter) && cangjieOptions.isFieldsUsages)
                ) {
                    addTask { ReferencesSearch.search(declaration, options.searchScope).forEach(referenceProcessor) }
                }
            }
        }
    }

    override fun getStringsToSearch(element: PsiElement): Collection<String> {
    return emptyList()
    }

    override fun isSearchForTextOccurrencesAvailable(psiElement: PsiElement, isSingleFile: Boolean): Boolean {
        return !isSingleFile
    }

    override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions {
        return factory.findClassOptions
    }
}
