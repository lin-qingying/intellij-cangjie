package com.huawei.cangjie.resolve

import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.ModuleCapability
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.name.FqName

object ImplicitIntegerCoercion {

    val MODULE_CAPABILITY = ModuleCapability<Boolean>("ImplicitIntegerCoercion")

    fun isEnabledFor(descriptor: DeclarationDescriptor, languageVersionSettings: LanguageVersionSettings): Boolean =

                (languageVersionSettings.supportsFeature(LanguageFeature.ImplicitSignedToUnsignedIntegerConversion) &&
                        DescriptorUtils.getContainingModuleOrNull(descriptor)?.hasImplicitIntegerCoercionCapability() == true)


}

fun ModuleDescriptor.hasImplicitIntegerCoercionCapability(): Boolean {
    return getCapability(ImplicitIntegerCoercion.MODULE_CAPABILITY) == true
}
