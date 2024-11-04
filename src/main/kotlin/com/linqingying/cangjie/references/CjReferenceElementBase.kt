package com.linqingying.cangjie.references

import com.linqingying.cangjie.psi.CjElement
import com.intellij.psi.PsiElement

//val PsiElement.unescapedText: String get() {
//    val text = text ?: return ""
//    return if (elementType == IDENTIFIER) text.unescapeIdentifier() else text
//}

/**
 * Provides basic methods for reference implementation ([org.rust.lang.core.resolve.ref.RsReferenceBase]).
 * This interface should not be used in any analysis.
 */
interface CjReferenceElementBase : CjElement {
    val referenceNameElement: PsiElement?

    val referenceName: String? get() = referenceNameElement?.text
}
