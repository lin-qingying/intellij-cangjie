package com.linqingying.cangjie.psi

import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.intellij.psi.PsiNameIdentifierOwner



interface CjNamedDeclaration : CjDeclaration, PsiNameIdentifierOwner, CjStatementExpression,
    CjNamed {
    val nameAsSafeName: Name
    val fqName: FqName?
}

