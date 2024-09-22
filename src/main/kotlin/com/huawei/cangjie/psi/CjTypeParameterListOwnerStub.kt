package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJieStubWithFqName
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

abstract class CjTypeParameterListOwnerStub<T : CangJieStubWithFqName<*>>
    : CjNamedDeclarationStub<T>, CjTypeParameterListOwner {
    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    constructor(node: ASTNode) : super(node)


    override val typeParameterList: CjTypeParameterList? get() = getStubOrPsiChild(CjStubElementTypes.TYPE_PARAMETER_LIST)
    override val typeConstraintList: CjTypeConstraintList? get() = getStubOrPsiChild(CjStubElementTypes.TYPE_CONSTRAINT_LIST)
    override val typeConstraints: List<CjTypeConstraint>
        get() {
            val typeConstraintList = typeConstraintList ?: return emptyList()
            return typeConstraintList.constraints
        }
    override val typeParameters: List<CjTypeParameter>
        get() {
            val list = typeParameterList ?: return emptyList()

            return list.parameters
        }

}
