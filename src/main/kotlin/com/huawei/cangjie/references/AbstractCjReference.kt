package com.huawei.cangjie.references

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.resolve.BindingContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache

//
//import com.huawei.cangjie.lexer.CjTokens.IDENTIFIER
//import com.huawei.cangjie.psi.CjElement
//import com.huawei.cangjie.psi.CjPsiFactory
//import com.huawei.cangjie.psi.psiUtil.elementType
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


abstract class AbstractCjReference<T : CjElement>(element: T) : PsiPolyVariantReferenceBase<T>(element), CjReference {
    open fun canRename(): Boolean = false

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
        ResolveCache.getInstance(expression.project).resolveWithCaching(this, resolver, false, incompleteCode)

    override fun toString() = this::class.java.simpleName + ": " + expression.text

    abstract override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor>
    protected fun getCjReferenceMutateService(): CjReferenceMutateService =
        ApplicationManager.getApplication().getService(CjReferenceMutateService::class.java)
            ?: throw IllegalStateException("Cannot handle element rename because KtReferenceMutateService is missing")

    protected open fun canBeReferenceTo(candidateTarget: PsiElement): Boolean = true


    val expression: T
        get() = element


    override val resolver: ResolveCache.PolyVariantResolver<CjReference>
        get() = CjPolyVariantResolver
}
