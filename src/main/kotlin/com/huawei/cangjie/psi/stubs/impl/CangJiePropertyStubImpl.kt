package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjProperty
import com.huawei.cangjie.psi.CjVariable
import com.huawei.cangjie.psi.stubs.CangJiePropertyStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef

class CangJiePropertyStubImpl(
    parent: StubElement<out PsiElement>?,
    private val name: StringRef?,
    private val fqName: FqName?,
    private val isExtension: Boolean,

    private val hasReturnTypeRef: Boolean,

) : CangJieStubBaseImpl<CjProperty>(parent, CjStubElementTypes.PROPERTY), CangJiePropertyStub {
    override fun getFqName(): FqName? = fqName
    override fun hasReturnTypeRef() = hasReturnTypeRef
    override fun isExtension() = isExtension

    override fun getName(): String? = StringRef.toString(name)
}
