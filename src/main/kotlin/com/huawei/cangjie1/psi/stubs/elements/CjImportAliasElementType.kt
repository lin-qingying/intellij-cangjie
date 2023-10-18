package com.huawei.cangjie1.psi.stubs.elements

import com.huawei.cangjie1.psi.CjImportAlias
import com.huawei.cangjie1.psi.stubs.CangJieImportAliasStub
import com.huawei.cangjie1.psi.stubs.impl.CangJieImportAliasStubImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef



class CjImportAliasElementType(debugName: String) :
    CjStubElementType<CangJieImportAliasStub, CjImportAlias>(debugName, CjImportAlias::class.java, CangJieImportAliasStub::class.java) {
    override fun createStub(psi: CjImportAlias, parentStub: StubElement<out PsiElement>?): CangJieImportAliasStub {
        return CangJieImportAliasStubImpl(parentStub, StringRef.fromString(psi.name))
    }

    override fun serialize(stub: CangJieImportAliasStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getName())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<PsiElement>?): CangJieImportAliasStub {
        val name = dataStream.readName()
        return CangJieImportAliasStubImpl(parentStub, name)
    }
}
