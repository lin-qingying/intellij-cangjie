package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.stubs.CangJieBasicTypeStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType

class CjBasicType : CjElementImplStub<CangJieBasicTypeStub>, CjTypeElement, CjSimpleNameExpression {
    //public class CjBasicType extends  CjElementImpl  implements CjTypeElement {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieBasicTypeStub) : super(stub, CjStubElementTypes.BASIC_TYPE)

    override fun toString(): String {
        return elementType.toString()
    }

    override fun getReferencedName(): String {
        return text
    }

    override fun getReferencedNameAsName(): Name {
        return Name.identifier(this.text)
    }

    override fun getReferencedNameElement(): PsiElement {
        return this
    }

    override fun getIdentifier(): PsiElement {
        return this
    }

    override fun getReferencedNameElementType(): IElementType {
        return this.elementType
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitBasicType(this, data)
    }

    override fun getText(): String {
        val stub = stub
        if (stub != null) {
            return stub.basicType
        }
        return super.getText()
    }


    override fun getName(): String {
        return text
    }


    val typeArguments: List<CjTypeProjection>
        get() = emptyList()

    override val typeArgumentsAsTypes: List<CjTypeReference>
        get() = emptyList()
}
