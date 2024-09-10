package com.huawei.cangjie.references

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.references.util.findPsiDeclarations
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.caches.resolveImportReference
import com.huawei.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope



class CjReferenceResolutionHelperImpl:CjReferenceResolutionHelper {
    override fun partialAnalyze(element: CjElement): BindingContext = element.safeAnalyzeNonSourceRootCode(
        BodyResolveMode.PARTIAL)

    override fun resolveImportReference(file: CjFile, fqName: FqName): Collection<DeclarationDescriptor> =
        file.resolveImportReference(fqName)

    override fun findPsiDeclarations(
        declaration: DeclarationDescriptor,
        project: Project,
        resolveScope: GlobalSearchScope
    ): Collection<PsiElement> = declaration.findPsiDeclarations(project, resolveScope)

//    override fun findDecompiledDeclaration(
//        project: Project,
//        referencedDescriptor: DeclarationDescriptor,
//        builtInsSearchScope: GlobalSearchScope?
//    ): CjDeclaration? =
//        org.jetbrains.kotlin.idea.decompiler.navigation.findDecompiledDeclaration(project, referencedDescriptor, builtInsSearchScope)
//

}
