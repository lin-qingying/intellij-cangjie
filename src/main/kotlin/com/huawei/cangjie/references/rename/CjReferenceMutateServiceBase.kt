package com.huawei.cangjie.references.rename


import com.huawei.cangjie.lexer.CjSingleValueToken
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjNameReferenceExpression
import com.huawei.cangjie.psi.CjPsiFactory
import com.huawei.cangjie.psi.CjUnaryExpression
import com.huawei.cangjie.psi.psiUtil.cangjieFqName
import com.huawei.cangjie.references.CjReference
import com.huawei.cangjie.references.CjReferenceMutateService
import com.huawei.cangjie.references.CjSimpleNameReference
import com.huawei.cangjie.references.CjSimpleReference
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parents
import com.intellij.util.IncorrectOperationException

/**
 * 重命名重构
 */
abstract class CjReferenceMutateServiceBase : CjReferenceMutateService {
    override fun handleElementRename(ktReference: CjReference, newElementName: String): PsiElement? {
        TODO("Not yet implemented")
    }

    override fun bindToElement(ktReference: CjReference, element: PsiElement): PsiElement {
        TODO("Not yet implemented")
    }

    override fun bindToElement(
        simpleNameReference: CjSimpleNameReference,
        element: PsiElement,
        shorteningMode: CjSimpleNameReference.ShorteningMode
    ): PsiElement {
        TODO("Not yet implemented")
    }

    override fun bindToFqName(
        simpleNameReference: CjSimpleNameReference,
        fqName: FqName,
        shorteningMode: CjSimpleNameReference.ShorteningMode,
        targetElement: PsiElement?
    ): PsiElement {
        TODO("Not yet implemented")
    }
}

class CangJieReferenceMutateService : CjReferenceMutateServiceBase() {

}
