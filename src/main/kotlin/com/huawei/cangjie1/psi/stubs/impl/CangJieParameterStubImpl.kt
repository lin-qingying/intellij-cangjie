package com.huawei.cangjie1.psi.stubs.impl

import com.huawei.cangjie1.name.FqName
import com.huawei.cangjie1.psi.CjParameter
import com.huawei.cangjie1.psi.stubs.CangJieParameterStub
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef


class CangJieParameterStubImpl(
    parent: StubElement<out PsiElement>?,
    private val fqName: StringRef?,
    private val name: StringRef?,
    private val isMutable: Boolean,
    private val hasValOrVar: Boolean,
    private val hasDefaultValue: Boolean,
    val functionTypeParameterName: String? = null
) : CangJieStubBaseImpl<CjParameter>(parent, CjStubElementTypes.VALUE_PARAMETER), CangJieParameterStub {

    override fun getName(): String? {
        return StringRef.toString(name)
    }

    override fun getFqName(): FqName? {
        return if (fqName != null) FqName(fqName.string) else null
    }

    override fun isMutable() = isMutable
    override fun hasValOrVar() = hasValOrVar
    override fun hasDefaultValue() = hasDefaultValue
}
