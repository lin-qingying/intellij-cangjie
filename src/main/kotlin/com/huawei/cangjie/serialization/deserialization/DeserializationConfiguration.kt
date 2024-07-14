package com.huawei.cangjie.serialization.deserialization


interface DeserializationConfiguration {

//    val binaryVersion: BinaryVersion?
//        get() = null

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
     * It is required to avoid PSI-Stub mismatch errors like in KT-41346.
     */
    val preserveDeclarationsOrdering: Boolean
        get() = false

    object Default : DeserializationConfiguration
}
