package com.huawei.cangjie.psi.stubs.elements

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjProperty
import com.huawei.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import com.huawei.cangjie.psi.stubs.CangJiePropertyStub
import com.huawei.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import com.huawei.cangjie.psi.stubs.impl.CangJiePropertyStubImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef

class CjPropertyElementType(debugName: String) : CjStubElementType<CangJiePropertyStub, CjProperty>(
    debugName,
    CjProperty::class.java,
    CangJiePropertyStub::class.java
) {
    override fun serialize(stub: CangJiePropertyStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeName(stub.getFqName()?.asString())
        dataStream.writeBoolean(stub.isExtension())

        dataStream.writeBoolean(stub.hasReturnTypeRef())

    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJiePropertyStub {
        val name = dataStream.readName()
        val fqNameAsString = dataStream.readName()
        val fqName: FqName? = if (fqNameAsString != null) FqName(fqNameAsString.toString()) else null
        val hasReceiverTypeRef = dataStream.readBoolean()

        val hasReturnTypeRef = dataStream.readBoolean()

        return CangJiePropertyStubImpl(
            parentStub, name,
            fqName, hasReceiverTypeRef, hasReturnTypeRef
        )
    }

    override fun createStub(psi: CjProperty, parentStub: StubElement<out PsiElement>?): CangJiePropertyStub {
        return CangJiePropertyStubImpl(
            parentStub, StringRef.fromString(psi.name),
            psi.safeFqNameForLazyResolve(), psi.receiverTypeReference != null,
            psi.typeReference != null
        )
    }

    override fun indexStub(stub: CangJiePropertyStub, sink: IndexSink) {
        getInstance().indexProperty(stub, sink)

    }

    override fun createPsi(stub: CangJiePropertyStub): CjProperty {
        return CjProperty(stub)
    }

    override fun createPsiFromAst(node: ASTNode): CjProperty {
        return CjProperty(node)
    }
}
