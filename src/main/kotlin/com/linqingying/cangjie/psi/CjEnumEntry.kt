package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJieEnumEntryStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjEnumEntry : CjTypeStatement {
    constructor(node: ASTNode) : super(node)

    constructor(
        stub: CangJieEnumEntryStub

    ) : super(stub, CjStubElementTypes.ENUM_ENTRY)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitEnumEntry(this, data)
    }
    override val typeName: String
        get() = "enum entry"
    override fun hasExplicitPrimaryConstructor(): Boolean {
        return typeReferences.isNotEmpty()
    }

    override fun toString(): String {
        return node.elementType.toString()
    }

    val typeEntry: CjEnumEntryTypeEntry?
        get() = findChildByClass(CjEnumEntryTypeEntry::class.java)
    val typeReferences: List<CjTypeReference>
        get() {
            if (typeEntry != null) {
                return typeEntry!!.getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_REFERENCE)
            }
            return emptyList()


        }
}
