package com.linqingying.cangjie.references

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.doc.psi.impl.CDocName
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.resolve.BindingContext
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
