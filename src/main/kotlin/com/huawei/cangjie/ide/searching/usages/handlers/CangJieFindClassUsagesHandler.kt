package com.huawei.cangjie.ide.searching.usages.handlers

import com.huawei.cangjie.ide.search.declarationsSearch.HierarchySearchRequest
import com.huawei.cangjie.ide.search.ideExtensions.CangJieReferencesSearchOptions
import com.huawei.cangjie.ide.search.ideExtensions.CangJieReferencesSearchParameters
import com.huawei.cangjie.ide.search.isImportUsage
import com.huawei.cangjie.ide.searching.findUsages.CangJieFindUsagesSupport.Companion.isConstructorUsage
import com.huawei.cangjie.ide.searching.usages.CangJieClassFindUsagesOptions
import com.huawei.cangjie.ide.searching.usages.CangJieFindUsagesHandlerFactory
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.effectiveDeclarations
import com.intellij.find.findUsages.AbstractFindUsagesDialog
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.search.PsiElementProcessorAdapter
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageInfo
import com.intellij.util.FilteredQuery
import com.intellij.util.Processor
import java.util.*



class CangJieFindClassUsagesHandler(
    ktClass: CjTypeStatement,
    factory: CangJieFindUsagesHandlerFactory
) : CangJieFindUsagesHandler<CjTypeStatement>(ktClass, factory) {
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
