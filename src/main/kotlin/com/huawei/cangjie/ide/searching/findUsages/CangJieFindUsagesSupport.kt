package com.huawei.cangjie.ide.searching.findUsages

import com.huawei.cangjie.ide.searching.isCangJieConstructorUsage
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjTypeStatement
import com.huawei.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.util.Processor

interface CangJieFindUsagesSupport
{
    fun getSuperMethods(declaration: CjDeclaration, ignore: Collection<PsiElement>?) : List<PsiElement>
    fun tryRenderDeclarationCompactStyle(declaration: CjDeclaration): String?
    fun isCangJieConstructorUsage(psiReference: PsiReference,cjClassOrObject: CjTypeStatement): Boolean

    companion object{
        fun getInstance(project: Project): CangJieFindUsagesSupport = project.service()
        fun tryRenderDeclarationCompactStyle(declaration: CjDeclaration): String? =
            getInstance(declaration.project).tryRenderDeclarationCompactStyle(declaration)
        fun PsiReference.isConstructorUsage(cjClassOrObject: CjTypeStatement): Boolean {

            return   getInstance(cjClassOrObject.project).isCangJieConstructorUsage(this, cjClassOrObject)
        }
        fun getSuperMethods(declaration: CjDeclaration, ignore: Collection<PsiElement>?) : List<PsiElement> =
            getInstance(declaration.project).getSuperMethods(declaration, ignore)
    }
}
class CangJieFindUsagesSupportImpl : CangJieFindUsagesSupport {
    override fun getSuperMethods(declaration:CjDeclaration, ignore: Collection<PsiElement>?): List<PsiElement> =
        com.huawei.cangjie.ide .refactoring.getSuperMethods(declaration, ignore)
    override fun tryRenderDeclarationCompactStyle(declaration: CjDeclaration): String? =
        com.huawei.cangjie.ide.searching.  tryRenderDeclarationCompactStyle(declaration)

    override fun isCangJieConstructorUsage(psiReference: PsiReference, cjClassOrObject: CjTypeStatement): Boolean {
        return       psiReference.isCangJieConstructorUsage(cjClassOrObject)
    }
}
