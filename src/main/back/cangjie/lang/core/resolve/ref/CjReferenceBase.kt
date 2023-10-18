package com.huawei.cangjie.lang.core.resolve.ref

import com.huawei.cangjie.ide.refactoring.isValidCangJieVariableIdentifier
import com.huawei.cangjie.lang.core.psi.CjElementTypes.IDENTIFIER
import com.huawei.cangjie.lang.core.psi.CjElementTypes.QUOTE_IDENTIFIER
import com.huawei.cangjie.lang.core.psi.CjPsiFactory
import com.huawei.cangjie.lang.core.psi.escapeIdentifierIfNeeded
import com.huawei.cangjie.lang.core.psi.ext.CjElement
import com.huawei.cangjie.lang.core.psi.ext.CjReferenceElementBase
import com.huawei.cangjie.lang.core.psi.ext.elementType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult


abstract class CjReferenceBase<T : CjReferenceElementBase>(
    element: T
) : PsiPolyVariantReferenceBase<T>(element),
    CjReference {

    override fun resolve(): CjElement? = super.resolve() as? CjElement

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
        multiResolve().map { PsiElementResolveResult(it) }.toTypedArray()

    open val T.referenceAnchor: PsiElement? get() = referenceNameElement

    final override fun getRangeInElement(): TextRange = super.getRangeInElement()

    final override fun calculateDefaultRangeInElement(): TextRange {
        val anchor = element.referenceAnchor ?: return TextRange.EMPTY_RANGE
        check(anchor.parent === element)
        return TextRange.from(anchor.startOffsetInParent, anchor.textLength)
    }

    override fun handleElementRename(newName: String): PsiElement {
        val referenceNameElement = element.referenceNameElement
        if (referenceNameElement != null) {
            doRename(referenceNameElement, newName)
        }
        return element
    }

    override fun getVariants(): Array<out LookupElement> = LookupElement.EMPTY_ARRAY

    override fun equals(other: Any?): Boolean = other is CjReferenceBase<*> && element === other.element

    override fun hashCode(): Int = element.hashCode()

    companion object {
        @JvmStatic fun doRename(identifier: PsiElement, newName: String) {
            val factory = CjPsiFactory(identifier.project)
            val newId = when (identifier.elementType) {
                IDENTIFIER -> {
                    // Renaming files is tricky: we don't want to change `RenamePsiFileProcessor`,
                    // so we must be ready for invalid names here
                    val name = newName.replace(".rs", "").escapeIdentifierIfNeeded()
                    if (!isValidCangJieVariableIdentifier(name)) return
                    factory.createIdentifier(name)

                }
                QUOTE_IDENTIFIER -> factory.createQuoteIdentifier(newName)
//                META_VAR_IDENTIFIER -> factory.createMetavarIdentifier(newName)
                else -> error("Unsupported identifier type for `$newName` (${identifier.elementType})")
            }
            newId?.let { identifier.replace(it) }
        }
    }
}
