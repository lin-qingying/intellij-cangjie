package com.huawei.cangjie.descriptors


import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.DescriptorVisibilities.*
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue


object DescriptorVisibilityUtils {
    @JvmStatic
    fun findInvisibleMember(
        receiver: ReceiverValue?,
        what: DeclarationDescriptorWithVisibility,
        from: DeclarationDescriptor,
        languageVersionSettings: LanguageVersionSettings
    ): DeclarationDescriptorWithVisibility? {
        return findInvisibleMember(receiver, what, from, languageVersionSettings.useSpecialRulesForPrivateSealedConstructors)
    }

    @JvmStatic
    fun isVisible(
        receiver: ReceiverValue?,
        what: DeclarationDescriptorWithVisibility,
        from: DeclarationDescriptor,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        return isVisible(receiver, what, from, languageVersionSettings.useSpecialRulesForPrivateSealedConstructors)
    }

    @JvmStatic
    fun isVisibleIgnoringReceiver(
        what: DeclarationDescriptorWithVisibility,
        from: DeclarationDescriptor,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        return isVisibleIgnoringReceiver(what, from, languageVersionSettings.useSpecialRulesForPrivateSealedConstructors)
    }

    @JvmStatic
    fun isVisibleWithAnyReceiver(
        what: DeclarationDescriptorWithVisibility,
        from: DeclarationDescriptor,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        return isVisibleWithAnyReceiver(what, from, languageVersionSettings.useSpecialRulesForPrivateSealedConstructors)
    }

    val LanguageVersionSettings.useSpecialRulesForPrivateSealedConstructors: Boolean
        get() = !supportsFeature(LanguageFeature.AllowSealedInheritorsInDifferentFilesOfSamePackage) ||
                !supportsFeature(LanguageFeature.UseConsistentRulesForPrivateConstructorsOfSealedClasses)
}

