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

package org.cangnova.cangjie.search.usages

import cn.cangnova.cangjie.ide.searching.usages.CangJieClassFindUsagesOptions
import cn.cangnova.cangjie.ide.searching.usages.CangJieFunctionFindUsagesOptions
import cn.cangnova.cangjie.ide.searching.usages.CangJiePropertyFindUsagesOptions
import org.cangnova.cangjie.search.usages.handlers.CangJieFindClassUsagesHandler
import org.cangnova.cangjie.search.usages.handlers.CangJieFindMemberUsagesHandler
import org.cangnova.cangjie.search.usages.handlers.DelegatingFindMemberUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandler.NULL_HANDLER
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.search.usages.handlers.CangJieTypeParameterFindUsagesHandler
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getQualifiedElementSelector
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.references.mainReference

class CangJieFindUsagesHandlerFactory(project: Project) : FindUsagesHandlerFactory() {

    val findFunctionOptions: CangJieFunctionFindUsagesOptions = CangJieFunctionFindUsagesOptions(project)

    val findPropertyOptions = CangJiePropertyFindUsagesOptions(project)
    val findClassOptions = CangJieClassFindUsagesOptions(project)
    val defaultOptions = FindUsagesOptions(project)

    override fun canFindUsages(element: PsiElement): Boolean =
        element is CjTypeStatement ||
                element is CjNamedFunction ||
                element is CjProperty ||
                element is CjParameter ||
                element is CjTypeParameter ||
                element is CjConstructor<*> ||
                (element is CjImportAlias &&
                        // TODO: it is ambiguous case: ImportAlias does not have any reference to be resolved
                        element.getStrictParentOfType<CjImportItem>()?.importedReference?.getQualifiedElementSelector()?.mainReference?.resolve() != null)


    override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler? {
        when (element) {
            is CjImportAlias -> {
                return when (val resolvedElement =
                    element.getStrictParentOfType<CjImportItem>()?.importedReference?.getQualifiedElementSelector()?.mainReference?.resolve()) {
                    is CjTypeStatement ->
                        if (!forHighlightUsages) {
                            createFindUsagesHandler(resolvedElement, forHighlightUsages = false)
                        } else NULL_HANDLER

                    is CjNamedFunction, is CjProperty, is CjConstructor<*> ->
                        createFindUsagesHandler(resolvedElement, forHighlightUsages)

                    else -> NULL_HANDLER
                }
            }

            is CjTypeStatement ->
                return CangJieFindClassUsagesHandler(element, this)

            is CjParameter -> return if (!forHighlightUsages) handlerForMultiple(element, listOf(element))
            else CangJieFindMemberUsagesHandler.getInstance(element, factory = this)

            is CjNamedFunction, is CjProperty, is CjConstructor<*> -> {
                val declaration = element as CjNamedDeclaration

                if (forHighlightUsages) {
                    return CangJieFindMemberUsagesHandler.getInstance(declaration, factory = this)
                }
                return handlerForMultiple(declaration, listOf(declaration))
            }

            is CjTypeParameter ->
                return CangJieTypeParameterFindUsagesHandler(element, this)

            else ->
                throw IllegalArgumentException("unexpected element type: $element")
        }
    }

    private fun handlerForMultiple(
        originalDeclaration: CjNamedDeclaration,
        declarations: Collection<PsiElement>
    ): FindUsagesHandler? {
        return when (declarations.size) {
            0 -> NULL_HANDLER

            1 -> {
                val target = declarations.single() ?: return NULL_HANDLER

                if (target is CjNamedDeclaration) {
                    CangJieFindMemberUsagesHandler.getInstance(target, factory = this)
                } else {
                    null
                }
            }

            else -> DelegatingFindMemberUsagesHandler(originalDeclaration, declarations, factory = this)
        }
    }
}
