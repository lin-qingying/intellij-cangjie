package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJieClassOrStructStub
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType


abstract class CjClassOrStruct :
    CjTypeParameterListOwnerStub<CangJieClassOrStructStub<out CjClassOrStruct>>, CjDeclarationContainer, CjNamedDeclaration,
    CjPureClassOrStruct, CjClassLikeDeclaration {

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieClassOrStructStub<out CjClassOrStruct>, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    fun isTopLevel(): Boolean = stub?.isTopLevel() ?: (parent is CjFile)

    override fun toString(): String {
        return node.elementType.toString()
    }

    }
