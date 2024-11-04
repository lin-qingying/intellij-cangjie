package com.linqingying.cangjie.ide.searching.usages.handlers

import com.linqingying.cangjie.ide.searching.usages.CangJieFindUsagesHandlerFactory
import com.linqingying.cangjie.ide.searching.usages.dialog.CangJieTypeParameterFindUsagesDialog
import com.linqingying.cangjie.psi.CjTypeParameter
import com.intellij.find.findUsages.AbstractFindUsagesDialog
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor



class CangJieTypeParameterFindUsagesHandler(
    element: CjTypeParameter,
    factory: CangJieFindUsagesHandlerFactory
) : CangJieFindUsagesHandler<CjTypeParameter>(element, factory) {
    override fun getFindUsagesDialog(
        isSingleFile: Boolean, toShowInNewTab: Boolean, mustOpenInNewTab: Boolean
    ): AbstractFindUsagesDialog = CangJieTypeParameterFindUsagesDialog(
        getElement(), project, findUsagesOptions, toShowInNewTab, mustOpenInNewTab, isSingleFile, this
    )

    override fun createSearcher(
        element: PsiElement,
        processor: Processor<in UsageInfo>,
        options: FindUsagesOptions
    ): Searcher = object : Searcher(element, processor, options) {
        override fun buildTaskList(forHighlight: Boolean): Boolean {
            addTask {
                runReadAction {
                    val searchScope = element.useScope.intersectWith(options.searchScope)
                    ReferencesSearch.search(element, searchScope).all { processUsage(processor, it) }
                }
            }

            return true
        }
    }

    override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions = factory.defaultOptions
}
