package com.huawei.cangjie.ide.searching.usages

import com.huawei.cangjie.highlighter.unwrapped
import com.huawei.cangjie.ide.searching.usages.handlers.CangJieFindClassUsagesHandler
import com.huawei.cangjie.psi.*
import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandler.NULL_HANDLER
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.huawei.cangjie.psi.psiUtil.getQualifiedElementSelector
import com.huawei.cangjie.references.mainReference
import com.huawei.cangjie.ide.searching.usages.handlers.CangJieFindMemberUsagesHandler
import com.huawei.cangjie.ide.searching.usages.handlers.CangJieTypeParameterFindUsagesHandler
import com.huawei.cangjie.ide.searching.usages.handlers.DelegatingFindMemberUsagesHandler

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
                        element.importDirective?.importedReference?.getQualifiedElementSelector()?.mainReference?.resolve() != null)


    override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler? {
        when (element) {
            is CjImportAlias -> {
                return when (val resolvedElement =
                    element.importDirective?.importedReference?.getQualifiedElementSelector()?.mainReference?.resolve()) {
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
                val target = declarations.single().unwrapped ?: return NULL_HANDLER

                if (target is CjNamedDeclaration) {
                    CangJieFindMemberUsagesHandler.getInstance(target, factory = this)
                }else{
                    null
                }
            }

            else -> DelegatingFindMemberUsagesHandler(originalDeclaration, declarations, factory = this)
        }
    }
}
