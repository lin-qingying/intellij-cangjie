package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjAnnotation : CjElementImplStub<CangJiePlaceHolderStub<CjAnnotation>> {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjAnnotation>) : super(stub, CjStubElementTypes.ANNOTATION)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitAnnotation(this, data)
    }

    val entries: List<CjAnnotationEntry>
        get() = getStubOrPsiChildrenAsList(CjStubElementTypes.ANNOTATION_ENTRY)


    fun removeEntry(entry: CjAnnotationEntry) {

        if (entries.size > 1) {
            entry.delete()
        } else {
            delete()
        }
    }
}

