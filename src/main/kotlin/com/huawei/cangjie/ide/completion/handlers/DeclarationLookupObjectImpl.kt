package com.huawei.cangjie.ide.completion.handlers

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.ide.completion.DescriptorBasedDeclarationLookupObject
import com.huawei.cangjie.ide.imports.importableFqName
import com.huawei.cangjie.ide.projectStructure.languageVersionSettings
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjTypeStatement
import com.huawei.cangjie.resolve.descriptorUtil.descriptorsEqualWithSubstitution
import com.intellij.psi.PsiNamedElement


/**
 * Stores information about resolved descriptor and position of that descriptor.
 * Position will be used for sorting
 */
abstract class DeclarationLookupObjectImpl(
    final override val descriptor: DeclarationDescriptor?
) : DescriptorBasedDeclarationLookupObject {
    override val name: Name?
        get() = descriptor?.name ?: (psiElement as? PsiNamedElement)?.name?.let { Name.identifier(it) }

    override val importableFqName: FqName?
        get() {
            return if (descriptor != null)
                descriptor.importableFqName
            else
                (psiElement as? CjTypeStatement)?.fqName
        }

    override fun toString() = super.toString() + " " + (descriptor ?: psiElement)

    override fun hashCode(): Int {
        return descriptor?.original?.hashCode() ?: psiElement!!.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.java != other::class.java) return false
        val lookupObject = other as DeclarationLookupObjectImpl
        return if (descriptor != null)
            descriptorsEqualWithSubstitution(descriptor, lookupObject.descriptor)
        else
            lookupObject.descriptor == null && psiElement == lookupObject.psiElement
    }

    override val isDeprecated: Boolean
        get() {
            return if (descriptor != null)
                isDeprecatedAtCallSite(descriptor) { psiElement?.languageVersionSettings }
            else
                false
        }

}

// This function is kind of a hack to avoid using DeprecationResolver as it's hard to preserve same resolutionFacade for descriptor
fun isDeprecatedAtCallSite(
    descriptor: DeclarationDescriptor,
    languageVersionSettings: () -> LanguageVersionSettings?
): Boolean {
    if (!CangJieBuiltIns.isDeprecated(descriptor)) return false

//    val annotation = descriptor.original.annotations.findAnnotation(StandardNames.FqNames.deprecatedSinceCangJie) ?: return true

//    //only from here we probably need languageVersionSettings, which evaluation could be costly
//    val hiddenSince = annotation.getSinceVersion("hiddenSince")
//    val errorSince = annotation.getSinceVersion("errorSince")
//    val warningSince = annotation.getSinceVersion("warningSince")
//
//    if (hiddenSince == null && errorSince == null && warningSince == null) {
//        return false //actually shouldn't happen, was false before refactoring
//    }
//    val apiVersion = languageVersionSettings()?.apiVersion ?: return true
//    if (hiddenSince != null && apiVersion >= hiddenSince) return true
//    if (errorSince != null && apiVersion >= errorSince) return true
//    if (warningSince != null && apiVersion >= warningSince) return true

    return false
}
