/*
 * Copyright 2024 LinQingYing. and contributors.
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

