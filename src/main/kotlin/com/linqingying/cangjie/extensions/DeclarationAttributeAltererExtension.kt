package com.linqingying.cangjie.extensions

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.descriptors.Modality
import com.linqingying.cangjie.psi.CjModifierListOwner

interface DeclarationAttributeAltererExtension {
    companion object : ProjectExtensionDescriptor<DeclarationAttributeAltererExtension>(
        "com.linqingying.cangjie.declarationAttributeAltererExtension",
        DeclarationAttributeAltererExtension::class.java
    )

    /**
     * Returns the new modality for the [declaration], or null if the [currentModality] is good enough.
     */
    fun refineDeclarationModality(
        modifierListOwner: CjModifierListOwner,
        declaration: DeclarationDescriptor?,
        containingDeclaration: DeclarationDescriptor?,
        currentModality: Modality,
        isImplicitModality: Boolean
    ): Modality? = null

    @Deprecated(
        "Use refineDeclarationModality(modifierListOwner, declaration, containingDeclaration, currentModality, bindingContext, isImplicitModality)",
        ReplaceWith("refineDeclarationModality(modifierListOwner, declaration, containingDeclaration, currentModality, bindingContext, false)")
    )
    fun refineDeclarationModality(
        modifierListOwner: CjModifierListOwner,
        declaration: DeclarationDescriptor?,
        containingDeclaration: DeclarationDescriptor?,
        currentModality: Modality
    ): Modality? {
        return refineDeclarationModality(modifierListOwner, declaration, containingDeclaration, currentModality, false)
    }

    fun shouldConvertFirstSAMParameterToReceiver(function: FunctionDescriptor): Boolean = false
}
