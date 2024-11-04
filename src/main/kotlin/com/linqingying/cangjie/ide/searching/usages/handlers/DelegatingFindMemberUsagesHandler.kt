package com.linqingying.cangjie.ide.searching.usages.handlers

import com.linqingying.cangjie.ide.searching.usages.CangJieCallableFindUsagesOptions
import com.linqingying.cangjie.ide.searching.usages.CangJieFindUsagesHandlerFactory
import com.linqingying.cangjie.psi.CjNamedDeclaration
import com.intellij.find.findUsages.AbstractFindUsagesDialog
import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor



class DelegatingFindMemberUsagesHandler(
    val declaration: CjNamedDeclaration,
    private val elementsToSearch: Collection<PsiElement>,
    val factory: CangJieFindUsagesHandlerFactory
) : FindUsagesHandler(declaration) {
    private val cangjieHandler = CangJieFindMemberUsagesHandler.getInstance(declaration, elementsToSearch, factory)

    private data class HandlerAndOptions(
        val handler: FindUsagesHandler,
        val options: FindUsagesOptions?
    )

    private fun getHandlerAndOptions(element: PsiElement, options: FindUsagesOptions?): HandlerAndOptions? {
        return when (element) {
            is CjNamedDeclaration ->
                HandlerAndOptions(CangJieFindMemberUsagesHandler.getInstance(element, elementsToSearch, factory), options)



            else -> null
        }
    }

    override fun getFindUsagesDialog(isSingleFile: Boolean, toShowInNewTab: Boolean, mustOpenInNewTab: Boolean): AbstractFindUsagesDialog {
        return getHandlerAndOptions(psiElement, null)?.handler?.getFindUsagesDialog(isSingleFile, toShowInNewTab, mustOpenInNewTab)
            ?: super.getFindUsagesDialog(isSingleFile, toShowInNewTab, mustOpenInNewTab)
    }

    override fun getPrimaryElements(): Array<PsiElement> {
        return cangjieHandler.primaryElements
    }

    override fun getSecondaryElements(): Array<out PsiElement> {
        return cangjieHandler.secondaryElements
    }

    override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions {
        return cangjieHandler.getFindUsagesOptions(dataContext)
    }

    override fun processElementUsages(
        element: PsiElement,
        processor: Processor<in UsageInfo>,
        options: FindUsagesOptions
    ): Boolean {
        val (handler, handlerOptions) = runReadAction { getHandlerAndOptions(element, options) } ?: return true
        return handler.processElementUsages(element, processor, handlerOptions!!)
    }

    override fun isSearchForTextOccurrencesAvailable(psiElement: PsiElement, isSingleFile: Boolean): Boolean = !isSingleFile
}
