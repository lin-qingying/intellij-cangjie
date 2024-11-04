package com.linqingying.cangjie.psi


import com.linqingying.cangjie.CjNodeTypes
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.psiUtil.getChildrenOfType
import com.linqingying.cangjie.psi.stubs.CangJieExtendStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

class CjExtend : CjTypeStatement {
    private val _stub: CangJieExtendStub?
        get() = stub as? CangJieExtendStub

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitExtend(this, data)
    }
    override val typeName: String
        get() = "extend"
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieExtendStub) : super(stub, CjStubElementTypes.EXTEND)

//    override val fqName: FqName?
//        get() = super.fqName

    override fun getName(): String? {
        val name = super.getName()



        return name?.let { name.split('.').last().replace(Regex("<.*?>"), "") }
    }

//    override val nameAsName: Name?
//        get() = super.nameAsName

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
        get() = Name.identifier( receiverTypeReceiver?.text ?: "")
    override val nameAsName: Name
        get() =Name.identifier(name ?: "")
    override fun getNameIdentifier(): PsiElement? {
//        val psiFactory = CjPsiFactory.contextual(this)
//        return psiFactory.createIdentifier(nameAsSafeName.toString())
        return receiverTypeReceiver
    }

    //    扩展id ，需要具有唯一性  ，通过被扩展名，父类，包名，行号
    fun getExtendId(): String {
        val sb = StringBuilder()

        sb.append(name)
        sb.append(getSupernames())

        sb.append(fqName)

        sb.append(textOffset)
        sb.append(textRange)
        sb.append(text)

        return sb.toString()
    }

    private fun getSupernames(): String {
        val list = findChildByType<CjSuperTypeList>(CjNodeTypes.SUPER_TYPE_LIST) ?: return "null"

        val names = list.getChildrenOfType<CjSuperTypeEntry>().map {
            it.children[0].text
        }
        return names.joinToString()

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
