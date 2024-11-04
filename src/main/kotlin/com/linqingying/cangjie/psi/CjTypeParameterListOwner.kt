package com.linqingying.cangjie.psi

interface CjTypeParameterListOwner : CjNamedDeclaration {
    val typeParameterList: CjTypeParameterList?

    val typeConstraintList: CjTypeConstraintList?




    val typeConstraints: List<CjTypeConstraint >


    val typeParameters: List<CjTypeParameter >

}
