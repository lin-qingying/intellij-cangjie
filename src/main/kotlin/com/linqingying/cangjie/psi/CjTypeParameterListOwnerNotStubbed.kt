package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes
import com.intellij.lang.ASTNode

@Deprecated("")
abstract class CjTypeParameterListOwnerNotStubbed(node: ASTNode) :
    CjNamedDeclarationNotStubbed(node), CjTypeParameterListOwner {
    override val typeParameterList: CjTypeParameterList?
        get() = findChildByType(CjNodeTypes.TYPE_PARAMETER_LIST)

    override val typeConstraintList: CjTypeConstraintList?
        get() = findChildByType(CjNodeTypes.TYPE_CONSTRAINT_LIST)

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
