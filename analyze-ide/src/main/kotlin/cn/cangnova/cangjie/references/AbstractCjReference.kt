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

package cn.cangnova.cangjie.references

import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.psi.CjConstructor
import cn.cangnova.cangjie.psi.CjElement
import cn.cangnova.cangjie.psi.CjImportAlias
import cn.cangnova.cangjie.psi.psiUtil.containingTypeStatement
import cn.cangnova.cangjie.references.util.unwrappedTargets
import cn.cangnova.cangjie.resolve.BindingContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache

//
//import cn.cangnova.cangjie.lexer.CjTokens.IDENTIFIER
//import cn.cangnova.cangjie.psi.CjElement
//import cn.cangnova.cangjie.psi.CjPsiFactory
//import cn.cangnova.cangjie.psi.psiUtil.elementType
//import com.intellij.codeInsight.lookup.LookupElement
//import com.intellij.openapi.util.TextRange
//import com.intellij.psi.PsiElement
//import com.intellij.psi.PsiElementResolveResult
//import com.intellij.psi.PsiPolyVariantReferenceBase
//import com.intellij.psi.ResolveResult
//
//abstract class AbstractCjReference<T : CjReferenceElementBase>(
//    element: T
//) : PsiPolyVariantReferenceBase<T>(element),
//    CjReference {
//    override fun resolve(): CjElement? = super.resolve() as? CjElement
//
//
//    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
//        multiResolve().map { PsiElementResolveResult(it) }.toTypedArray()
//
//
//    open val T.referenceAnchor: PsiElement? get() = referenceNameElement
//    final override fun getRangeInElement(): TextRange = super.getRangeInElement()
//    final override fun calculateDefaultRangeInElement(): TextRange {
//        val anchor = element.referenceAnchor ?: return TextRange.EMPTY_RANGE
//        check(anchor.parent === element)
//        return TextRange.from(anchor.startOffsetInParent, anchor.textLength)
//    }
//
//
//    override fun getVariants(): Array<out LookupElement> = LookupElement.EMPTY_ARRAY
//
//    override fun equals(other: Any?): Boolean = other is AbstractCjReference<*> && element === other.element
//
//    override fun hashCode(): Int = element.hashCode()
//
//    override fun handleElementRename(newName: String): PsiElement {
//        val referenceNameElement = element.referenceNameElement
//        if (referenceNameElement != null) {
//            doRename(referenceNameElement, newName)
//        }
//        return element
//    }
//
//    companion object {
//        @JvmStatic
//        fun doRename(identifier: PsiElement, newName: String) {
//            val factory = CjPsiFactory(identifier.project)
//            val newId = when (identifier.elementType) {
//                IDENTIFIER -> {
//                    // Renaming files is tricky: we don't want to change `RenamePsiFileProcessor`,
//                    // so we must be ready for invalid names here
//                    val name = newName.replace(".cj", "").escapeIdentifierIfNeeded()
//                    if (!isValidCangJieVariableIdentifier(name)) return
//                    factory.createNameIdentifier(name)
//
//                }
////                QUOTE_IDENTIFIER -> factory.createQuoteIdentifier(newName)
////                META_VAR_IDENTIFIER -> factory.createMetavarIdentifier(newName)
//                else -> error("Unsupported identifier type for `$newName` (${identifier.elementType})")
//            }
//            identifier.replace(newId)
//        }
//    }
//}
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
