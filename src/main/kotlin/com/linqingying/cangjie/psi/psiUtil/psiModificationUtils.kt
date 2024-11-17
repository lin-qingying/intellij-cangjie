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

package com.linqingying.cangjie.psi.psiUtil

import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.extensions.DeclarationAttributeAltererExtension
import com.linqingying.cangjie.ide.projectStructure.languageVersionSettings
import com.linqingying.cangjie.lexer.CjModifierKeywordToken
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.linqingying.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.linqingying.cangjie.resolve.toKeywordToken
import com.linqingying.cangjie.utils.match
import com.linqingying.cangjie.utils.safeAs
import com.intellij.psi.tree.IElementType

fun CjDeclaration.getModalityFromDescriptor(descriptor: DeclarationDescriptor? = resolveToDescriptorIfAny()): CjModifierKeywordToken? {
    if (descriptor is MemberDescriptor) {
        return mapModality(descriptor.modality)
    }

    return null
}

fun CjDeclaration.implicitVisibility(): CjModifierKeywordToken? {
    return when {
        this is CjPropertyAccessor && isSetter && property.hasModifier(CjTokens.OVERRIDE_KEYWORD) -> {
            property.resolveToDescriptorIfAny()
                ?.safeAs<PropertyDescriptor>()
                ?.overriddenDescriptors?.forEach {
                    val visibility = it.setter?.visibility?.toKeywordToken()
                    if (visibility != null) return visibility
                }

            CjTokens.DEFAULT_VISIBILITY_KEYWORD
        }

        this is CjConstructor<*> -> {
            // constructors cannot be declared in objects
            val cclass = getContainingTypeStatement() as? CjTypeStatement ?: return CjTokens.DEFAULT_VISIBILITY_KEYWORD

            when {
                cclass.isEnum() -> CjTokens.PRIVATE_KEYWORD
                cclass.isSealed() ->
                    if (cclass.languageVersionSettings.supportsFeature(LanguageFeature.SealedInterfaces)) CjTokens.PROTECTED_KEYWORD
                    else CjTokens.PRIVATE_KEYWORD

                else -> CjTokens.DEFAULT_VISIBILITY_KEYWORD
            }
        }

//        hasModifier(CjTokens.OVERRIDE_KEYWORD) -> {
//            resolveToDescriptorIfAny()?.safeAs<CallableMemberDescriptor>()
//                ?.overriddenDescriptors
//                ?.let { OverridingUtil.findMaxVisibility(it) }
//                ?.toKeywordToken()
//        }

        else -> CjTokens.DEFAULT_VISIBILITY_KEYWORD
    }
}
fun CjDeclaration.isOverridable(): Boolean =
    !hasModifier(CjTokens.PRIVATE_KEYWORD) &&  // 'private' is incompatible with 'open'
            (parents.match(CjParameterList::class, CjPrimaryConstructor::class, last = CjTypeStatement::class)
                ?: parents.match(CjAbstractClassBody::class, last = CjTypeStatement::class))
                ?.let { it.isInheritable() || it.isEnum() } == true &&
            getModalityFromDescriptor() in setOf(CjTokens.ABSTRACT_KEYWORD, CjTokens.OPEN_KEYWORD)

fun CjTypeStatement.isInheritable(): Boolean {
    return when (getModalityFromDescriptor()) {
        CjTokens.ABSTRACT_KEYWORD, CjTokens.OPEN_KEYWORD, CjTokens.SEALED_KEYWORD -> true
        else -> false
    }
}
private fun CjDeclaration.predictImplicitModality(): CjModifierKeywordToken? {
    if (this is CjTypeStatement) {
        if (this is CjInterface) return CjTokens.ABSTRACT_KEYWORD
        return null
    }
    val cclass = containingTypeStatement ?: return null
    if (hasModifier(CjTokens.OVERRIDE_KEYWORD)) {
        if (cclass.hasModifier(CjTokens.ABSTRACT_KEYWORD) ||
            cclass.hasModifier(CjTokens.OPEN_KEYWORD) ||
            cclass.hasModifier(CjTokens.SEALED_KEYWORD)
        ) {
            return CjTokens.OPEN_KEYWORD
        }
    }
    if (cclass is CjInterface && !hasModifier(CjTokens.PRIVATE_KEYWORD)) {
        return if (hasBody()) CjTokens.OPEN_KEYWORD else CjTokens.ABSTRACT_KEYWORD
    }
    return null
}

fun CjDeclaration.hasBody() = when (this) {
    is CjFunction -> hasBody()
//    is CjProperty -> hasBody()
    else -> false
}

fun CjDeclaration.implicitModality(): CjModifierKeywordToken? {
    var predictedModality = predictImplicitModality()
    val bindingContext = safeAnalyzeNonSourceRootCode(BodyResolveMode.PARTIAL)
    val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this] ?: return predictedModality
    val containingDescriptor = descriptor.containingDeclaration ?: return predictedModality

    val extensions = DeclarationAttributeAltererExtension.getInstances(this.project)
    for (extension in extensions) {
        val newModality = extension.refineDeclarationModality(
            this,
            descriptor as? ClassDescriptor,
            containingDescriptor,
            mapModalityToken(predictedModality),
            isImplicitModality = true
        )

        if (newModality != null) {
            predictedModality = mapModality(newModality)
        }
    }

    return predictedModality
}

fun mapModality(accurateModality: Modality): CjModifierKeywordToken? = when (accurateModality) {
    Modality.FINAL -> null
    Modality.SEALED -> CjTokens.SEALED_KEYWORD
    Modality.OPEN -> CjTokens.OPEN_KEYWORD
    Modality.ABSTRACT -> CjTokens.ABSTRACT_KEYWORD
}

private fun mapModalityToken(modalityToken: IElementType?): Modality = when (modalityToken) {
    null -> Modality.FINAL
    CjTokens.SEALED_KEYWORD -> Modality.SEALED
    CjTokens.OPEN_KEYWORD -> Modality.OPEN
    CjTokens.ABSTRACT_KEYWORD -> Modality.ABSTRACT
    else -> error("Unexpected modality keyword $modalityToken")
}
