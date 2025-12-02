/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.serialization.deserialization

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion


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
