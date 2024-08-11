package com.huawei.cangjie.ide.searching.findUsages

import com.huawei.cangjie.psi.CjDeclaration
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.util.Processor

interface CangJieFindUsagesSupport {

//    companion object{
//        fun getInstance(project: Project): CangJieFindUsagesSupport = project.service()
//
//        fun processCompanionObjectInternalReferences(
//            companionObject: CjObjectDeclaration,
//            referenceProcessor: Processor<PsiReference>
//        ): Boolean =
//            getInstance(companionObject.project).processCompanionObjectInternalReferences(companionObject, referenceProcessor)
//
//        fun tryRenderDeclarationCompactStyle(declaration: CjDeclaration): String? =
//            getInstance(declaration.project).tryRenderDeclarationCompactStyle(declaration)
//
//        @NlsSafe
//        fun formatJavaOrLightMethod(method: PsiMethod): String =
//            getInstance(method.project).formatJavaOrLightMethod(method)
//
//        fun PsiReference.isConstructorUsage(ktClassOrObject: CjClassOrObject): Boolean {
//            fun isJavaConstructorUsage(): Boolean {
//                val call = element.getNonStrictParentOfType<PsiConstructorCall>()
//                return call == element.parent && call?.resolveConstructor()?.containingClass?.navigationElement == ktClassOrObject
//            }
//
//            return isJavaConstructorUsage() || getInstance(ktClassOrObject.project).isCangJieConstructorUsage(this, ktClassOrObject)
//        }
//
//        fun getSuperMethods(declaration: CjDeclaration, ignore: Collection<PsiElement>?) : List<PsiElement> =
//            getInstance(declaration.project).getSuperMethods(declaration, ignore)
//    }
}
