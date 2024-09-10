package com.huawei.cangjie.ide.search

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.ide.util.fuzzyExtensionReceiverType
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjImportDirective
import com.huawei.cangjie.psi.CjTypeStatement
import com.huawei.cangjie.psi.psiUtil.getNonStrictParentOfType
import com.huawei.cangjie.references.util.DescriptorToSourceUtilsIde
import com.huawei.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.FuzzyType
import com.huawei.cangjie.types.toFuzzyType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.huawei.cangjie.resolve.isExtension

fun PsiReference.isImportUsage(): Boolean =
    element.getNonStrictParentOfType<CjImportDirective>() != null

fun PsiElement.getReceiverTypeSearcherInfo(): ReceiverTypeSearcherInfo? {
    val receiverType = runReadAction { extractReceiverType() } ?: return null
    val psiClass = runReadAction { receiverType.toTypeStatement(project) }
    return ReceiverTypeSearcherInfo(psiClass) {
        containsTypeOrDerivedInside(it, receiverType)
    }
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
