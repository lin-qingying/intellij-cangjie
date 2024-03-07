package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.psi.CjAnnotationEntry
import com.huawei.cangjie.psi.stubs.CangJieAnnotationEntryStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef

class CangJieAnnotationEntryStubImpl (
    parent: StubElement<out PsiElement>?,
    private val shortName: StringRef?,
    private val hasValueArguments: Boolean,
//    val valueArguments: Map<Name, ConstantValue<*>>?
):CangJieStubBaseImpl<CjAnnotationEntry>(parent, CjStubElementTypes.ANNOTATION_ENTRY), CangJieAnnotationEntryStub {

    override fun getShortName() = shortName?.string

    override fun hasValueArguments() = hasValueArguments
}
