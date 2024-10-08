package com.huawei.cangjie.references

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.doc.psi.impl.CDocName
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.resolve.BindingContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope

interface CjReferenceResolutionHelper {
    fun partialAnalyze(element: CjElement): BindingContext
    fun resolveImportReference(file: CjFile, fqName: FqName): Collection<DeclarationDescriptor>
    fun findPsiDeclarations(declaration: DeclarationDescriptor, project: Project, resolveScope: GlobalSearchScope): Collection<PsiElement>
    fun resolveCDocLink(element: CDocName): Collection<DeclarationDescriptor>

//    fun findDecompiledDeclaration(
//        project: Project,
//        referencedDescriptor: DeclarationDescriptor,
//        builtInsSearchScope: GlobalSearchScope?
//    ): CjDeclaration?
    companion object {
        fun getInstance(): CjReferenceResolutionHelper = ApplicationManager.getApplication().getService(CjReferenceResolutionHelper::class.java)
    }
}
