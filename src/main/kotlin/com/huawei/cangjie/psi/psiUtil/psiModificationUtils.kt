package com.huawei.cangjie.psi.psiUtil

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.Modality
import com.huawei.cangjie.extensions.DeclarationAttributeAltererExtension
import com.huawei.cangjie.lexer.CjModifierKeywordToken
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjFunction
import com.huawei.cangjie.psi.CjInterface
import com.huawei.cangjie.psi.CjTypeStatement
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import com.huawei.cangjie.resolve.lazy.BodyResolveMode
import com.intellij.psi.tree.IElementType

private fun CjDeclaration.predictImplicitModality(): CjModifierKeywordToken? {
    if (this is CjTypeStatement) {
        if (this is CjInterface) return CjTokens.ABSTRACT_KEYWORD
        return null
    }
    val cclass = containingClassOrStruct ?: return null
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
