package com.huawei.cangjie.references

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.resolve.BindingContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

interface CjReferenceResolutionHelper {
    fun partialAnalyze(element: CjElement): BindingContext

//    fun findDecompiledDeclaration(
//        project: Project,
//        referencedDescriptor: DeclarationDescriptor,
//        builtInsSearchScope: GlobalSearchScope?
//    ): CjDeclaration?
    companion object {
        fun getInstance(): CjReferenceResolutionHelper = ApplicationManager.getApplication().getService(CjReferenceResolutionHelper::class.java)
    }
}
