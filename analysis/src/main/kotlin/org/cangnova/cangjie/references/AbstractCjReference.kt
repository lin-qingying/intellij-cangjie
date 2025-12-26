/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.references

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjImportAlias
import org.cangnova.cangjie.psi.psiUtil.containingTypeStatement
import org.cangnova.cangjie.references.util.unwrappedTargets
import org.cangnova.cangjie.resolve.binding.BindingContext


abstract class CjMultiReference<T : CjElement>(expression: T) : AbstractCjReference<T>(expression)


abstract class AbstractCjReference<T : CjElement>(element: T) : PsiPolyVariantReferenceBase<T>(element), CjReference {
    open fun canRename(): Boolean = false

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
        ResolveCache.getInstance(expression.project).resolveWithCaching(this, resolver, false, incompleteCode)

    override fun toString() = this::class.java.simpleName + ": " + expression.text
    abstract override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor>
    protected fun getCjReferenceMutateService(): CjReferenceMutateService =
        ApplicationManager.getApplication().getService(CjReferenceMutateService::class.java)
            ?: throw IllegalStateException("Cannot handle element rename because CjReferenceMutateService is missing")

    protected open fun canBeReferenceTo(candidateTarget: PsiElement): Boolean = true
    protected open fun isReferenceToImportAlias(alias: CjImportAlias): Boolean {
        val importDirective = alias.importDirective ?: return false
        val importedFqName = importDirective.importedFqName ?: return false
        val helper = CjReferenceResolutionHelper.getInstance()
        val importedDescriptors = helper.resolveImportReference(importDirective.getContainingCjFile(), importedFqName)
        val importableTargets = unwrappedTargets.mapNotNull {
            when {
                it is CjConstructor<*> -> it.containingTypeStatement

                else -> it
            }
        }

        val project = element.project
        val resolveScope = element.resolveScope

        return importedDescriptors.any {
            helper.findPsiDeclarations(it, project, resolveScope).any { declaration ->
                declaration in importableTargets
            }
        }
    }

    override fun handleElementRename(newElementName: String): PsiElement? =
        if (canRename())
            getCjReferenceMutateService().handleElementRename(this, newElementName)
        else
            null

    val expression: T
        get() = element


    override val resolver: ResolveCache.PolyVariantResolver<CjReference>
        get() = CjPolyVariantResolver
}
