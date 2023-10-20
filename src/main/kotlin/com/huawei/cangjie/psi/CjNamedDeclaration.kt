package com.huawei.cangjie.psi

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.intellij.psi.PsiNameIdentifierOwner



interface CjNamedDeclaration : CjDeclaration, PsiNameIdentifierOwner, CjStatementExpression,
    CjNamed {
    val nameAsSafeName: Name
    val fqName: FqName?
}

