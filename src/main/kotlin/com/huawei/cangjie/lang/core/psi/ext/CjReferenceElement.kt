package com.huawei.cangjie.lang.core.psi.ext



import com.huawei.cangjie.lang.core.psi.unescapedText
import com.huawei.cangjie.lang.core.resolve.ref.CjReference
import com.intellij.psi.PsiElement



interface CjReferenceElementBase : CjElement {
    val referenceNameElement: PsiElement?

    val referenceName: String? get() = referenceNameElement?.unescapedText
}


interface CjReferenceElement : CjReferenceElementBase {
    override fun getReference(): CjReference?
}


interface CjMandatoryReferenceElement : CjReferenceElement {

    override val referenceNameElement: PsiElement

    override val referenceName: String get() = referenceNameElement.unescapedText

    override fun getReference(): CjReference
}
