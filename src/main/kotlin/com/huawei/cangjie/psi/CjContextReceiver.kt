package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.stubs.CangJieContextReceiverStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode


class CjContextReceiver : CjElementImplStub<CangJieContextReceiverStub> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieContextReceiverStub) : super(stub, CjStubElementTypes.CONTEXT_RECEIVER)

    fun targetLabel(): CjSimpleNameExpression? =
        findChildByType<CjContainerNode?>(CjNodeTypes.LABEL_QUALIFIER)
            ?.findChildByType(CjNodeTypes.LABEL)

    fun labelName(): String? {
        stub?.let { return it.getLabel() }
        return targetLabel()?.getReferencedName()
    }

    fun labelNameAsName(): Name? {
        stub?.let { stub -> return stub.getLabel()?.let { Name.identifier(it) } }
        return targetLabel()?.getReferencedNameAsName()
    }

    fun typeReference(): CjTypeReference? = getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)

    fun name(): String? = labelName() ?: typeReference()?.nameForReceiverLabel()
}
