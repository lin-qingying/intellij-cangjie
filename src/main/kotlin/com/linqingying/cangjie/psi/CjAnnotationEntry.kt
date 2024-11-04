package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.name.Name.Companion.identifier
import com.linqingying.cangjie.psi.stubs.CangJieAnnotationEntryStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

class CjAnnotationEntry : CjElementImplStub<CangJieAnnotationEntryStub >, CjCallElement {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieAnnotationEntryStub) : super(stub, CjStubElementTypes.ANNOTATION_ENTRY)

    override val calleeExpression : CjConstructorCalleeExpression? get(){
        return getStubOrPsiChild(CjStubElementTypes.CONSTRUCTOR_CALLEE)
    }

    override val lambdaArguments: List<CjLambdaArgument> get(){
        return emptyList()
    }

    override val typeArguments : List<CjTypeProjection> get(){
        val typeArgumentList = typeArgumentList ?: return emptyList()
        return typeArgumentList.arguments
    }

    override val typeArgumentList : CjTypeArgumentList? = null

    override val valueArgumentList : CjValueArgumentList? get(){
        val stub = stub
        if (stub == null && greenStub != null) {
            return findChildByType(CjNodeTypes.VALUE_ARGUMENT_LIST)
        }

        return getStubOrPsiChild(CjStubElementTypes.VALUE_ARGUMENT_LIST)
    }

    override val valueArguments : List<ValueArgument > get(){
        val stub = stub
        if (stub != null && !stub.hasValueArguments()) {
            return emptyList<CjValueArgument>()
        }

        val list = valueArgumentList
        return list?.arguments ?: emptyList<CjValueArgument>()
    }
    val atSymbol : PsiElement? get()    {
        return findChildByType(CjTokens.AT)
    }
    @get:IfNotParsed
    val typeReference: CjTypeReference?
        get() {
            val calleeExpression = calleeExpression ?: return null
            return calleeExpression.typeReference
        }
    val shortName: Name?
        get() {
            val stub = stub
            if (stub != null) {
                val shortName = stub.getShortName()
                if (shortName != null) {
                    return identifier(shortName)
                }
                return null
            }
            typeReference ?: return null
            val typeReference =
                checkNotNull(typeReference) { "Annotation entry hasn't typeReference $text" }
            val typeElement = typeReference.typeElement
            if (typeElement is CjUserType) {
                val shortName = typeElement.referencedName
                if (shortName != null) {
                    return identifier(shortName)
                }
            }
            return null
        }

}
