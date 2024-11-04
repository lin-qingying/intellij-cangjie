package com.linqingying.cangjie.descriptors


import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.DescriptorVisibilities.*
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue


object DescriptorVisibilityUtils {
    @JvmStatic
    fun findInvisibleMember(
        receiver: ReceiverValue?,
        what: DeclarationDescriptor,
        from: DeclarationDescriptor,
        languageVersionSettings: LanguageVersionSettings
    ): DeclarationDescriptor? {
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
        what: DeclarationDescriptor,
        from: DeclarationDescriptor,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        return isVisibleIgnoringReceiver(what, from, languageVersionSettings.useSpecialRulesForPrivateSealedConstructors)
    }

    @JvmStatic
    fun isVisibleWithAnyReceiver(
        what: DeclarationDescriptor,
        from: DeclarationDescriptor,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        return isVisibleWithAnyReceiver(what, from, languageVersionSettings.useSpecialRulesForPrivateSealedConstructors)
    }

    val LanguageVersionSettings.useSpecialRulesForPrivateSealedConstructors: Boolean
        get() = !supportsFeature(LanguageFeature.AllowSealedInheritorsInDifferentFilesOfSamePackage) ||
                !supportsFeature(LanguageFeature.UseConsistentRulesForPrivateConstructorsOfSealedClasses)
}

