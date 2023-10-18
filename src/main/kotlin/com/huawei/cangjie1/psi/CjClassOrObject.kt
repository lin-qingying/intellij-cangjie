package com.huawei.cangjie1.psi

import com.huawei.cangjie1.psi.stubs.CangJieClassOrObjectStub
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType


abstract class CjClassOrObject :
    CjTypeParameterListOwnerStub<CangJieClassOrObjectStub<out CjClassOrObject>>, CjDeclarationContainer, CjNamedDeclaration,
    CjPureClassOrObject, CjClassLikeDeclaration {

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieClassOrObjectStub<out CjClassOrObject>, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    fun isTopLevel(): Boolean = stub?.isTopLevel() ?: (parent is CjFile)



    }
