package com.huawei.cangjie.references

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.doc.psi.impl.CDocLink
import com.huawei.cangjie.doc.psi.impl.CDocName
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.psiUtil.getStrictParentOfType
import com.huawei.cangjie.references.util.findPsiDeclarations
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.caches.getResolutionFacade
import com.huawei.cangjie.resolve.caches.resolveImportReference
import com.huawei.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.huawei.cangjie.resolve.caches.safeAnalyze


class CjReferenceResolutionHelperImpl:CjReferenceResolutionHelper {
    override fun partialAnalyze(element: CjElement): BindingContext = element.safeAnalyzeNonSourceRootCode(
        BodyResolveMode.PARTIAL)

    override fun resolveImportReference(file: CjFile, fqName: FqName): Collection<DeclarationDescriptor> =
        file.resolveImportReference(fqName)
    override fun resolveCDocLink(element: CDocName): Collection<DeclarationDescriptor> {
        val declaration = element.getContainingDoc().owner ?: return emptyList()
        val resolutionFacade = element.getResolutionFacade()
        val correctContext = declaration.safeAnalyze(resolutionFacade, BodyResolveMode.PARTIAL)
        if (correctContext == BindingContext.EMPTY) return emptyList()
        val declarationDescriptor = correctContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] ?: return emptyList()

        val cdocLink = element.getStrictParentOfType<CDocLink>()!!
        return resolveCDocLink(
            correctContext,
            resolutionFacade,
            declarationDescriptor,
            element,
            cdocLink.getTagIfSubject(),
            element.getQualifiedName()
        )
    }
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
//        com.huawei.cangjie.idea.decompiler.navigation.findDecompiledDeclaration(project, referencedDescriptor, builtInsSearchScope)
//

}
