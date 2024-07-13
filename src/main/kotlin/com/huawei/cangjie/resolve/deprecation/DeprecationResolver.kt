package com.huawei.cangjie.resolve.deprecation

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.psi.Call
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.storage.StorageManager


class DeprecationResolver(
    storageManager: StorageManager,

    private val deprecationSettings: DeprecationSettings
) {
//    fun isHiddenInResolution(
//        descriptor: DeclarationDescriptor,
//        callElement: CjElement?,
//        bindingContext: BindingContext?,
//        isSuperCall: Boolean,
//        fromImportingScope: Boolean
//    ): Boolean {
//        if (descriptor is FunctionDescriptor) {
//            if (descriptor.isHiddenToOvercomeSignatureClash) return true
//            if (descriptor.isHiddenForResolutionEverywhereBesideSupercalls && !isSuperCall) return true
//        }
//
//        val sinceKotlinAccessibility = isHiddenBecauseOfKotlinVersionAccessibility(descriptor.original)
//        if (sinceKotlinAccessibility is SinceKotlinAccessibility.NotAccessible) return true
//
//        if (sinceKotlinAccessibility is SinceKotlinAccessibility.NotAccessibleButWasExperimental) {
//            return if (callElement != null && bindingContext != null) {
//                with(OptInUsageChecker) {
//                    sinceKotlinAccessibility.markerClasses.any { classDescriptor ->
//                        !callElement.isOptInAllowed(classDescriptor.fqNameSafe, languageVersionSettings, bindingContext)
//                    }
//                }
//            } else {
//                // We need a softer check for descriptors from importing scope as there is no access to PSI elements
//                // It's fine to return false here as there will be additional checks for accessibility later
//                !fromImportingScope
//            }
//        }
//
//        return isDeprecatedHidden(descriptor)
//    }
//    fun getDeprecations(descriptor: DeclarationDescriptor): List<DescriptorBasedDeprecationInfo> =
//        deprecations(descriptor.original).deprecations
//
//    fun isDeprecatedHidden(descriptor: DeclarationDescriptor): Boolean =
//        getDeprecations(descriptor).any { it.deprecationLevel == DeprecationLevelValue.HIDDEN }
//

//    @JvmOverloads
//    fun isHiddenInResolution(
//        descriptor: DeclarationDescriptor,
//        call: Call? = null,
//        bindingContext: BindingContext? = null,
//        isSuperCall: Boolean = false,
//        fromImportingScope: Boolean = false
//    ): Boolean =
//        isHiddenInResolution(descriptor, call?.callElement, bindingContext, isSuperCall, fromImportingScope)

}
