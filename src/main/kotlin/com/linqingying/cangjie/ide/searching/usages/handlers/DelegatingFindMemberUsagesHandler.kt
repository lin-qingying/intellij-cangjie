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
