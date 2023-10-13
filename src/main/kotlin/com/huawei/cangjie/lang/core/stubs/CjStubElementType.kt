package com.huawei.cangjie.lang.core.stubs

import com.huawei.cangjie.lang.CjLanguage
import com.huawei.cangjie.lang.core.psi.ext.CjElement
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IStubFileElementType

abstract class CjStubElementType<StubT : StubElement<*>, PsiT : CjElement>(
    debugName: String
) : IStubElementType<StubT, PsiT>(debugName, CjLanguage) {

    final override fun getExternalId(): String = "cangjie.${super.toString()}"

    override fun indexStub(stub: StubT, sink: IndexSink) {}
}

fun createStubIfParentIsStub(node: ASTNode): Boolean {
    val parent = node.treeParent
    val parentType = parent.elementType
    return (parentType is IStubElementType<*, *> && parentType.shouldCreateStub(parent)) ||
            parentType is IStubFileElementType<*>
}
