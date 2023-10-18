package com.huawei.cangjie1.psi

import com.huawei.cangjie1.name.FqName
import com.huawei.cangjie1.name.Name
import com.intellij.psi.PsiNameIdentifierOwner



interface CjNamedDeclaration : CjDeclaration, PsiNameIdentifierOwner, CjStatementExpression,
    CjNamed {
    val nameAsSafeName: Name
    val fqName: FqName?
}

