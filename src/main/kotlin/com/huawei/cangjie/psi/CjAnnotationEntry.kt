package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.name.Name.Companion.identifier
import com.huawei.cangjie.psi.stubs.CangJieAnnotationEntryStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

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
