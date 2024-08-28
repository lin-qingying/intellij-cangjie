package com.huawei.cangjie.psi


import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.stubs.CangJieExtendStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

class CjExtend : CjTypeStatement {
    private val _stub: CangJieExtendStub?
        get() = stub as? CangJieExtendStub

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitExtend(this, data)
    }

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieExtendStub) : super(stub, CjStubElementTypes.EXTEND)

    //被扩展类型
    val receiverTypeReceiver: CjTypeReference?
        get() {
            val stub = stub
            if (stub != null) {

                val childTypeReferences = getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_REFERENCE)
                return if (childTypeReferences.isNotEmpty()) {
                    childTypeReferences[0]
                } else {
                    null
                }
            }
            return getReceiverTypeRefByTree()
        }
    override val nameAsSafeName: Name
        get() = Name.identifier("extend_" + receiverTypeReceiver?.text)

    override fun getNameIdentifier(): PsiElement? {
//        val psiFactory = CjPsiFactory.contextual(this)
//        return psiFactory.createIdentifier(nameAsSafeName.toString())
        return receiverTypeReceiver
    }

    private fun getReceiverTypeRefByTree(): CjTypeReference? {
        var child = firstChild
        while (child != null) {
            val tt = child.node.elementType
            if (tt === CjTokens.LPAR || tt === CjTokens.COLON) break
            if (child is CjTypeReference) {
                return child
            }
            child = child.nextSibling
        }

        return null
    }

}
//
//class CjExtend1 : CjDeclarationStub<CangJieExtendStub> {
//    constructor(node: ASTNode) : super(node)
//    constructor(stub: CangJieExtendStub) : super(stub, CjStubElementTypes.EXTEND)
//
//
//    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D?): R {
//        return visitor.visitExtend(this, data)
//    }
//
//    //被扩展类型
//    val receiverTypeReceiver: CjTypeReference?
//        get() {
//            val stub = stub
//            if (stub != null) {
//
//                val childTypeReferences = getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_REFERENCE)
//                return if (childTypeReferences.isNotEmpty()) {
//                    childTypeReferences[0]
//                } else {
//                    null
//                }
//            }
//            return getReceiverTypeRefByTree()
//        }
//
//    private fun getReceiverTypeRefByTree(): CjTypeReference? {
//        var child = firstChild
//        while (child != null) {
//            val tt = child.node.elementType
//            if (tt === CjTokens.LPAR || tt === CjTokens.COLON) break
//            if (child is CjTypeReference) {
//                return child
//            }
//            child = child.nextSibling
//        }
//
//        return null
//    }
//
//
//
//    override fun toString(): String = node.elementType.toString() /*+ ": " + receiverTypeReceiver?.text*/
//    fun getSuperNames(): List<String> {
//        return emptyList()
//    }
//
//    fun getFqName(): FqName? {
//        return null
//    }
//}
