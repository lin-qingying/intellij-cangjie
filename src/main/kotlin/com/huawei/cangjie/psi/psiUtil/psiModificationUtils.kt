package com.huawei.cangjie.psi.psiUtil

import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.extensions.DeclarationAttributeAltererExtension
import com.huawei.cangjie.ide.projectStructure.languageVersionSettings
import com.huawei.cangjie.lexer.CjModifierKeywordToken
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.OverridingUtil
import com.huawei.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.huawei.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.huawei.cangjie.resolve.toKeywordToken
import com.huawei.cangjie.utils.match
import com.huawei.cangjie.utils.safeAs
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
                ?: parents.match(CjClassBody::class, last = CjTypeStatement::class))
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
