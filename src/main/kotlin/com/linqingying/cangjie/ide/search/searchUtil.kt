package com.linqingying.cangjie.ide.search

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.psi.*

import com.linqingying.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.linqingying.cangjie.references.util.DescriptorToSourceUtilsIde
import com.linqingying.cangjie.resolve.caches.getResolutionFacade
import com.linqingying.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.FuzzyType
import com.linqingying.cangjie.types.toFuzzyType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.linqingying.cangjie.resolve.isExtension
import com.linqingying.cangjie.types.fuzzyExtensionReceiverType

fun PsiReference.isImportUsage(): Boolean =
    element.getNonStrictParentOfType<CjImportDirective>() != null

fun PsiElement.getReceiverTypeSearcherInfo(): ReceiverTypeSearcherInfo? {
    val receiverType = runReadAction { extractReceiverType() } ?: return null
    val psiClass = runReadAction { receiverType.toTypeStatement(project) }
    return ReceiverTypeSearcherInfo(psiClass) {
        containsTypeOrDerivedInside(it, receiverType)
    }
}

fun CjFile.forceResolveReferences(elements: List<CjElement>) {
    getResolutionFacade().analyze(elements, BodyResolveMode.PARTIAL)
}

private fun containsTypeOrDerivedInside(declaration: CjDeclaration, typeToSearch: FuzzyType): Boolean {

    fun CangJieType.containsTypeOrDerivedInside(): Boolean {
        return typeToSearch.checkIsSuperTypeOf(this) != null || arguments.any {  it.type.containsTypeOrDerivedInside() }
    }

    val descriptor = declaration.resolveToDescriptorIfAny() as? CallableDescriptor
    val type = descriptor?.returnType
    return type != null && type.containsTypeOrDerivedInside()
}

private fun FuzzyType.toTypeStatement(project: Project): CjTypeStatement? {
    val classDescriptor = type.constructor.declarationDescriptor ?: return null
    return DescriptorToSourceUtilsIde.getAnyDeclaration(project, classDescriptor) as? CjTypeStatement
}
private fun PsiElement.extractReceiverType(): FuzzyType? {
    val descriptor = resolveTargetToDescriptor()?.takeIf { it.isValidOperator() } ?: return null

    return if (descriptor.isExtension) {
        descriptor.fuzzyExtensionReceiverType()!!
    } else {
        val classDescriptor = descriptor.containingDeclaration as? ClassDescriptor ?: return null
        classDescriptor.defaultType.toFuzzyType(classDescriptor.typeConstructor.parameters)
    }
}
fun FunctionDescriptor.isValidOperator() = isOperator /*&& OperatorChecks.check(this).isSuccess*/
private fun PsiElement.resolveTargetToDescriptor( ): FunctionDescriptor? {



    return when {
        this is CjDeclaration -> resolveToDescriptorIfAny(BodyResolveMode.FULL)

        else -> null
    } as? FunctionDescriptor
}

