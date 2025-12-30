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

package org.cangnova.cangjie.completion.handlers

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.completion.DescriptorBasedDeclarationLookupObject
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjTypeStatement
import com.intellij.psi.PsiNamedElement
import org.cangnova.cangjie.resolve.calls.util.languageVersionSettings
import org.cangnova.cangjie.resolve.descriptorsEqualWithSubstitution
import org.cangnova.cangjie.utils.importableFqName


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
