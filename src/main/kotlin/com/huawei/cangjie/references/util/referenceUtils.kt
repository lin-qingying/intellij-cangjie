package com.huawei.cangjie.references.util

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptorWithSource
import com.huawei.cangjie.highlighter.unwrapped
import com.huawei.cangjie.ide.imports.importableFqName
import com.huawei.cangjie.psi.CjNamedDeclaration
import com.huawei.cangjie.psi.CjProperty
import com.huawei.cangjie.psi.CjPropertyAccessor
import com.huawei.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.huawei.cangjie.resolve.source.getPsi
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope

// Navigation element of the resolved reference
// For property accessor return enclosing property
val PsiReference.unwrappedTargets: Set<PsiElement>
    get() {
        fun PsiElement.adjust(): PsiElement? = when (val target = unwrapped?.originalElement) {
            is CjPropertyAccessor -> target.getNonStrictParentOfType<CjProperty>()
            else -> target
        }

        return when (this) {
            is PsiPolyVariantReference -> multiResolve(false).mapNotNullTo(HashSet()) { it.element?.adjust() }
            else -> listOfNotNull(resolve()?.adjust()).toSet()
        }
    }
fun DeclarationDescriptor.findPsiDeclarations(project: Project, resolveScope: GlobalSearchScope): Collection<PsiElement> {
    val fqName = importableFqName ?: return emptyList()

    fun Collection<CjNamedDeclaration>.fqNameFilter() = filter { it.fqName == fqName }
    return when (this) {
//        is DeserializedClassDescriptor ->
//            CangJieFullClassNameIndex.get(fqName.asString(), project, resolveScope)
//
//        is DeserializedTypeAliasDescriptor ->
//            CangJieTypeAliasShortNameIndex.get(fqName.shortName().asString(), project, resolveScope).fqNameFilter()
//
//        is DeserializedSimpleFunctionDescriptor,
//        is FunctionImportedFromObject ->
//            CangJieFunctionShortNameIndex.get(fqName.shortName().asString(), project, resolveScope).fqNameFilter()
//
//        is DeserializedPropertyDescriptor,
//        is PropertyImportedFromObject ->
//            CangJiePropertyShortNameIndex.get(fqName.shortName().asString(), project, resolveScope).fqNameFilter()

        is DeclarationDescriptorWithSource -> listOfNotNull(source.getPsi())

        else -> emptyList()
    }
}
