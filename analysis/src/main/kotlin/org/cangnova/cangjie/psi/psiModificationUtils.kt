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

package org.cangnova.cangjie.psi

import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.codeinsight.ShortenReferences
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.rendering.IdeDescriptorRenderers
import org.cangnova.cangjie.extensions.DeclarationAttributeAltererExtension
import org.cangnova.cangjie.lexer.*
import org.cangnova.cangjie.psi.psiUtil.containingTypeStatement
import org.cangnova.cangjie.psi.psiUtil.parents
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.caches.resolveToDescriptorIfAny
import org.cangnova.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import org.cangnova.cangjie.resolve.calls.util.languageVersionSettings
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import org.cangnova.cangjie.resolve.toKeywordToken
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isError
import org.cangnova.cangjie.utils.match
import org.cangnova.cangjie.utils.safeAs


fun CjCallableDeclaration.setType(type: CangJieType, shortenReferences: Boolean = true) {
    if (type.isError()) return
    setType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type), shortenReferences)
}

fun CjCallableDeclaration.setType(typeString: String, shortenReferences: Boolean = true) {
    val typeReference = CjPsiFactory(project).createType(typeString)
    setTypeReference(typeReference)
    if (shortenReferences) {
        ShortenReferences.DEFAULT.process(typeReference)
    }
}


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
                  CjTokens.PROTECTED_KEYWORD


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
        // 与编译器保持一致：Interface 默认为 OPEN (天然可被实现)
        if (this is CjInterface) return CjTokens.OPEN_KEYWORD
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
