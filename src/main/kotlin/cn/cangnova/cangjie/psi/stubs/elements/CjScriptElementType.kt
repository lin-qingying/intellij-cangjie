package cn.cangnova.cangjie.psi.stubs.elements

import cn.cangnova.cangjie.psi.CjScript
import cn.cangnova.cangjie.psi.stubs.CangJieScriptStub
import cn.cangnova.cangjie.psi.stubs.impl.CangJieScriptStubImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef

class CjScriptElementType(debugName: String) : CjStubElementType<CangJieScriptStub, CjScript>(
    debugName,
    CjScript::class.java,
    CangJieScriptStub::class.java,
) {

    override fun createStub(psi: CjScript, parentStub: StubElement<out PsiElement>): CangJieScriptStub {
        return CangJieScriptStubImpl(parentStub, StringRef.fromString(psi.fqName.asString()))
    }

    override fun serialize(stub: CangJieScriptStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getFqName().asString())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<PsiElement>): CangJieScriptStub {
        val fqName = dataStream.readName()
        return CangJieScriptStubImpl(parentStub, fqName)
    }

    override fun indexStub(stub: CangJieScriptStub, sink: IndexSink) {
        StubIndexService.getInstance().indexScript(stub, sink)
    }
}
