package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.extensions.DeclarationAttributeAltererExtension
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.CjModifierList
import com.huawei.cangjie.psi.CjModifierListOwner
import com.intellij.psi.PsiElement

class ModifiersChecker {

    fun withTrace(trace: BindingTrace): ModifiersCheckingProcedure {
        return ModifiersCheckingProcedure(trace)
    }


    class ModifiersCheckingProcedure(trace: BindingTrace)

    companion object {

        fun resolveVisibilityFromModifiers(
            modifierListOwner: CjModifierListOwner,
            defaultVisibility: DescriptorVisibility
        ): DescriptorVisibility {
            return resolveVisibilityFromModifiers(
                modifierListOwner.getModifierList(),
                defaultVisibility
            )
        }

        fun resolveMemberModalityFromModifiers(
            modifierListOwner: CjModifierListOwner?,
            defaultModality: Modality,
            bindingContext: BindingContext,
            containingDescriptor: DeclarationDescriptor?
        ): Modality {
            return resolveModalityFromModifiers(
                modifierListOwner, defaultModality,
                bindingContext, containingDescriptor,  /* allowSealed = */false
            )
        }

        private fun resolveModalityFromModifiers(
            containingDescriptor: DeclarationDescriptor?,
            modifierList: CjModifierList?,
            defaultModality: Modality,
            allowSealed: Boolean
        ): Modality {

            if (modifierList == null) return defaultModality
            val hasAbstractModifier = modifierList.hasModifier(CjTokens.ABSTRACT_KEYWORD)
            val hasOverrideModifier = modifierList.hasModifier(CjTokens.OVERRIDE_KEYWORD)

            if (allowSealed && modifierList.hasModifier(CjTokens.SEALED_KEYWORD)) {
                return Modality.SEALED
            }
            if (modifierList.hasModifier(CjTokens.OPEN_KEYWORD)) {
                if (containingDescriptor is ClassDescriptor) {
                    val classOrInterface: ClassDescriptor =
                        containingDescriptor
                    if (classOrInterface.getKind() == ClassKind.INTERFACE/* && classOrInterface.isExpect()*/) {
                        return Modality.OPEN
                    }
                }
                if (hasAbstractModifier || defaultModality == Modality.ABSTRACT) {
                    return Modality.ABSTRACT
                }
                return Modality.OPEN
            }
            if (hasAbstractModifier) {
                return Modality.ABSTRACT
            }
//            val hasFinalModifier = modifierList.hasModifier(CjTokens.FINAL_KEYWORD)
            if (hasOverrideModifier /*&& !hasFinalModifier*/ && defaultModality != Modality.ABSTRACT) {
                return Modality.OPEN
            }
//            if (hasFinalModifier) {
//                return Modality.FINAL
//            }
            return defaultModality

        }

        fun resolveModalityFromModifiers(
            modifierListOwner: CjModifierListOwner?,
            defaultModality: Modality,
            bindingContext: BindingContext,
            containingDescriptor: DeclarationDescriptor?,
            allowSealed: Boolean
        ): Modality {
//            TODO()
            val modifierList =
                if ((modifierListOwner != null)) modifierListOwner.modifierList else null
            var modality =
                resolveModalityFromModifiers(
                    containingDescriptor,
                    modifierList,
                    defaultModality,
                    allowSealed
                )

            if (modifierListOwner != null) {
                val extensions: Collection<DeclarationAttributeAltererExtension> =
                    DeclarationAttributeAltererExtension.getInstances(
                        modifierListOwner.project
                    )

                val descriptor =
                    bindingContext.get<PsiElement, DeclarationDescriptor>(
                        BindingContext.DECLARATION_TO_DESCRIPTOR,
                        modifierListOwner
                    )
                for (extension in extensions) {
                    val newModality = extension.refineDeclarationModality(
                        modifierListOwner, descriptor, containingDescriptor, modality, false
                    )

                    if (newModality != null) {
                        modality = newModality
                        break
                    }
                }
            }
//
            return modality
        }

        fun resolveVisibilityFromModifiers(
            modifierList: CjModifierList?,
            defaultVisibility: DescriptorVisibility
        ): DescriptorVisibility {
            if (modifierList == null) return defaultVisibility
            if (modifierList.hasModifier(CjTokens.PRIVATE_KEYWORD)) return DescriptorVisibilities.PRIVATE
            if (modifierList.hasModifier(CjTokens.PUBLIC_KEYWORD)) return DescriptorVisibilities.PUBLIC
            if (modifierList.hasModifier(CjTokens.PROTECTED_KEYWORD)) return DescriptorVisibilities.PROTECTED

            return defaultVisibility
        }
    }

    private enum class DetailedClassKind(val withCapitalFirstLetter: String) {

        ENUM_ENTRY("Enum entry"),

        INTERFACE("Interface"),

        CLASS("Class");

        companion object {
            fun getClassKind(descriptor: ClassDescriptor): DetailedClassKind {
                if (DescriptorUtils.isEnumEntry(descriptor)) return ENUM_ENTRY


                if (DescriptorUtils.isInterface(descriptor)) return INTERFACE

                return CLASS
            }
        }
    }

}
