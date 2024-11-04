package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.ModuleCapability
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.name.FqName

object ImplicitIntegerCoercion {

    val MODULE_CAPABILITY = ModuleCapability<Boolean>("ImplicitIntegerCoercion")

    fun isEnabledFor(descriptor: DeclarationDescriptor, languageVersionSettings: LanguageVersionSettings): Boolean =

                (languageVersionSettings.supportsFeature(LanguageFeature.ImplicitSignedToUnsignedIntegerConversion) &&
                        DescriptorUtils.getContainingModuleOrNull(descriptor)?.hasImplicitIntegerCoercionCapability() == true)


}

fun ModuleDescriptor.hasImplicitIntegerCoercionCapability(): Boolean {
    return getCapability(ImplicitIntegerCoercion.MODULE_CAPABILITY) == true
}
