package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjVariable
import com.huawei.cangjie.psi.stubs.CangJieVariableStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef


class CangJieVariableStubImpl(
    parent: StubElement<out PsiElement>?,
    private val name: StringRef?,
    private val isVar: Boolean,
    private val isTopLevel: Boolean,

    private val hasInitializer: Boolean,
    private val isExtension: Boolean,
    private val hasReturnTypeRef: Boolean,
    private val fqName: FqName?,

    val origin: CangJieStubOrigin?
) : CangJieStubBaseImpl<CjVariable>(parent, CjStubElementTypes.VARIABLE), CangJieVariableStub {

    init {
        if (isTopLevel && fqName == null) {
            throw IllegalArgumentException("fqName shouldn't be null for top level properties")
        }

    }

    override fun getFqName() = fqName
    override fun isVar() = isVar
    override fun isTopLevel() = isTopLevel

    override fun hasInitializer() = hasInitializer
    override fun isExtension() = isExtension
    override fun hasReturnTypeRef() = hasReturnTypeRef
    override fun getName() = StringRef.toString(name)
}
