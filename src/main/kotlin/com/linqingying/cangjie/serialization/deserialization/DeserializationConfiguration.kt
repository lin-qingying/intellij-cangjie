package com.linqingying.cangjie.serialization.deserialization

import com.linqingying.cangjie.config.AnalysisFlags
import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.metadata.deserialization.BinaryVersion


interface DeserializationConfiguration {

    val binaryVersion: BinaryVersion?
        get() = null

    val skipMetadataVersionCheck: Boolean
        get() = false

    val skipPrereleaseCheck: Boolean
        get() = false

    val reportErrorsOnPreReleaseDependencies: Boolean
        get() = false

    val allowUnstableDependencies: Boolean
        get() = false

    val typeAliasesAllowed: Boolean
        get() = true

    val isJvmPackageNameSupported: Boolean
        get() = true

    val readDeserializedContracts: Boolean
        get() = false

    /**
     * We may want to preserve the order of the declarations the same as in the serialized object
     * (for example, to later create a decompiled code with the original order of declarations).
     *
     */
    val preserveDeclarationsOrdering: Boolean
        get() = false

    object Default : DeserializationConfiguration
}
open class CompilerDeserializationConfiguration(
    protected val languageVersionSettings: LanguageVersionSettings
) : DeserializationConfiguration {

//    final override val skipMetadataVersionCheck = languageVersionSettings.getFlag(AnalysisFlags.skipMetadataVersionCheck)
//
//    final override val skipPrereleaseCheck = languageVersionSettings.getFlag(AnalysisFlags.skipPrereleaseCheck)
//
//    final override val reportErrorsOnPreReleaseDependencies =
//        !skipPrereleaseCheck && !languageVersionSettings.isPreRelease() && !KotlinCompilerVersion.isPreRelease()
//
//    final override val allowUnstableDependencies = languageVersionSettings.getFlag(AnalysisFlags.allowUnstableDependencies)
//
//    final override val typeAliasesAllowed = languageVersionSettings.supportsFeature(LanguageFeature.TypeAliases)
//
//    final override val isJvmPackageNameSupported = languageVersionSettings.supportsFeature(LanguageFeature.JvmPackageName)
//
//    final override val readDeserializedContracts: Boolean =
//        languageVersionSettings.supportsFeature(LanguageFeature.ReadDeserializedContracts)
}
