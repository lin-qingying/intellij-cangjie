package com.huawei.cangjie.lang.core.stubs

import com.huawei.cangjie.lang.core.psi.ext.CjElement
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.*


open class CjPlaceholderStub<PsiT : CjElement>(parent: StubElement<*>?, elementType: IStubElementType<*, *>)
    : StubBase<PsiT>(parent, elementType) {

    open class Type<PsiT : CjElement>(
        debugName: String,
        private val psiCtor: (CjPlaceholderStub<*>, IStubElementType<*, *>) -> PsiT
    ) : CjStubElementType<CjPlaceholderStub<*>, PsiT>(debugName) {

        override fun shouldCreateStub(node: ASTNode): Boolean = createStubIfParentIsStub(node)

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CjPlaceholderStub<PsiT>
                = CjPlaceholderStub(parentStub, this)

        override fun serialize(stub: CjPlaceholderStub<*>, dataStream: StubOutputStream) {
        }

        override fun createPsi(stub: CjPlaceholderStub<*>): PsiT = psiCtor(stub, this)

        override fun createStub(psi: PsiT, parentStub: StubElement<*>?): CjPlaceholderStub<PsiT>
                = CjPlaceholderStub(parentStub, this)
    }
}
