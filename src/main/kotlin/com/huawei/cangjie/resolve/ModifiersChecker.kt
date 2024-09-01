package com.huawei.cangjie.resolve

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.extensions.DeclarationAttributeAltererExtension
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.*
import com.intellij.psi.PsiElement

class ModifiersChecker(


    val languageVersionSettings: LanguageVersionSettings
) {


    fun withTrace(trace: BindingTrace): ModifiersCheckingProcedure {
        return ModifiersCheckingProcedure(trace)
    }


    inner class ModifiersCheckingProcedure(val trace: BindingTrace) {
        fun checkModifiersForDeclaration(
            modifierListOwner: CjDeclaration,
            descriptor: MemberDescriptor
        ) {
            checkNestedClassAllowed(modifierListOwner, descriptor)
            checkTypeParametersModifiers(modifierListOwner)
            checkModifierListCommon(modifierListOwner, descriptor)
            checkIllegalHeader(modifierListOwner, descriptor)
        }
        fun checkModifiersForLocalDeclaration(
            modifierListOwner: CjDeclaration,
            descriptor:  DeclarationDescriptor
        ) {
            checkModifierListCommon(modifierListOwner, descriptor)
        }
        private fun checkIllegalHeader(
            modifierListOwner: CjModifierListOwner,
            descriptor: DeclarationDescriptor
        ) {
            // Most cases are already handled by ModifierCheckerCore, only check nested classes here
//            val modifierList:  CjModifierList = modifierListOwner.getModifierList()
//            var keyword: PsiElement? =
//                if (modifierList != null) modifierList.getModifier( CjTokens.HEADER_KEYWORD) else null
//            if (keyword != null &&
//                descriptor is  ClassDescriptor && descriptor.getContainingDeclaration() is  ClassDescriptor
//            ) {
//                trace.report(
//                    Errors.WRONG_MODIFIER_TARGET.on(
//                        keyword,
//                         CjTokens.HEADER_KEYWORD,
//                        "nested class"
//                    )
//                )
//            } else if (keyword == null && modifierList != null) {
//                keyword = modifierList.getModifier( CjTokens.EXPECT_KEYWORD)
//                if (keyword != null &&
//                    descriptor is  ClassDescriptor && descriptor.getContainingDeclaration() is  ClassDescriptor
//                ) {
//                    trace.report(
//                        Errors.WRONG_MODIFIER_TARGET.on(
//                            keyword,
//                             CjTokens.EXPECT_KEYWORD,
//                            "nested class"
//                        )
//                    )
//                }
//            }
        }

        fun checkTypeParametersModifiers(modifierListOwner: CjModifierListOwner) {
            if (modifierListOwner !is CjTypeParameterListOwner) return
            val typeParameters: List<CjTypeParameter> =
                modifierListOwner.getTypeParameters()
            for (typeParameter in typeParameters) {
                ModifierCheckerCore.check(
                    typeParameter,
                    trace,
                    null,
                    languageVersionSettings
                )
            }
        }

        fun runDeclarationCheckers(
            declaration: CjDeclaration,
            descriptor: DeclarationDescriptor
        ) {
//          val context: DeclarationCheckerContext = DeclarationCheckerContext(
//              trace, languageVersionSettings, deprecationResolver, moduleDescriptor, expectActualTracker,
//              missingSupertypesResolver
//          )
//          for (checker in declarationCheckers) {
//              ProgressManager.checkCanceled()
//              checker.check(declaration, descriptor, context)
//          }
//          OperatorModifierChecker.check(declaration, descriptor, trace, languageVersionSettings)
//          PublishedApiUsageChecker.check(declaration, descriptor, trace)
//          OptionalExpectationChecker.check(declaration, descriptor, trace)
        }

        private fun checkModifierListCommon(
            modifierListOwner: CjDeclaration,
            descriptor: DeclarationDescriptor
        ) {
//            AnnotationUseSiteTargetChecker.check(modifierListOwner, descriptor, trace, languageVersionSettings)
            runDeclarationCheckers(modifierListOwner, descriptor)
//            annotationChecker.check(modifierListOwner, trace, descriptor)
            ModifierCheckerCore.check(
                modifierListOwner,
                trace,
                descriptor,
                languageVersionSettings
            )
        }

        private fun checkNestedClassAllowed(
            declaration: CjDeclaration,
            descriptor: DeclarationDescriptor
        ) {
            if (declaration !is CjTypeStatement) return
            val typeStatemtnt: CjTypeStatement = declaration
            if (descriptor !is ClassDescriptor) return
            val classDescriptor: ClassDescriptor =
                descriptor
            val containingDeclaration: DeclarationDescriptor =
                descriptor.containingDeclaration as? ClassDescriptor
                    ?: return
            val containingClass: ClassDescriptor =
                containingDeclaration as ClassDescriptor

            val kind: DetailedClassKind =
                DetailedClassKind.getClassKind(classDescriptor)

//        if (kind == DetailedClassKind.ANONYMOUS_OBJECT || kind == DetailedClassKind.ENUM_ENTRY) return

            // Local enums / objects / companion objects are handled in different checks
            if ((kind == DetailedClassKind.ENUM || kind == DetailedClassKind.STRUCT) &&
                DescriptorUtils.isLocal(classDescriptor)
            ) {
                return
            }

            // Since 1.3, enum entries can contain inner classes only.
            // Companion objects are reported in ModifierCheckerCore.
//            if (DescriptorUtils.isEnumEntry(containingClass)   ) {
//                val diagnostic: DiagnosticFactory1<CjTypeStatement, String> =
//                    if (languageVersionSettings.supportsFeature( LanguageFeature.NestedClassesInEnumEntryShouldBeInner)
//                    )  Errors.NESTED_CLASS_NOT_ALLOWED
//                    else  Errors.NESTED_CLASS_DEPRECATED
//                trace.report(diagnostic.on(typeStatemtnt, kind.withCapitalFirstLetter))
//                return
//            }

//            if (   (containingClass.isInner() || DescriptorUtils.isLocal(
//                    containingClass
//                ))
//            ) {
//                trace.report(
//                    Errors.NESTED_CLASS_NOT_ALLOWED.on(
//                        typeStatemtnt,
//                        kind.withCapitalFirstLetter
//                    )
//                )
//            }
        }

    }

    companion object {

//        @JvmStatic

//        fun resolveVisibilityFormPackageOrImport(
//            modifierListOwner: CjModifierListOwner,
//            defaultVisibility: DescriptorVisibility
//        ): DescriptorVisibility {
//
//        }

        @JvmStatic
        fun resolveVisibilityFromModifiers(
            modifierListOwner: CjModifierListOwner,
            defaultVisibility: DescriptorVisibility
        ): DescriptorVisibility {
            return resolveVisibilityFromModifiers(
                modifierListOwner.getModifierList(),
                defaultVisibility
            )
        }

        @JvmStatic
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

        @JvmStatic
        fun resolveModalityFromModifiers(
            modifierListOwner: CjModifierListOwner?,
            defaultModality: Modality,
            bindingContext: BindingContext,
            containingDescriptor: DeclarationDescriptor?,
            allowSealed: Boolean
        ): Modality {
//            TODO()
            val modifierList =   if ((modifierListOwner != null)) modifierListOwner.modifierList else null
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

        @JvmStatic
        fun resolveVisibilityFromModifiers(
            modifierList: CjModifierList?,
            defaultVisibility: DescriptorVisibility
        ): DescriptorVisibility {
            if (modifierList == null) return defaultVisibility
            if (modifierList.hasModifier(CjTokens.PRIVATE_KEYWORD)) return DescriptorVisibilities.PRIVATE
            if (modifierList.hasModifier(CjTokens.PUBLIC_KEYWORD)) return DescriptorVisibilities.PUBLIC
            if (modifierList.hasModifier(CjTokens.PROTECTED_KEYWORD)) return DescriptorVisibilities.PROTECTED
            if (modifierList.hasModifier(CjTokens.INTERNAL_KEYWORD)) return DescriptorVisibilities.INTERNAL

            return defaultVisibility
        }
    }

    private enum class DetailedClassKind(val withCapitalFirstLetter: String) {

        ENUM_ENTRY("Enum entry"),

        INTERFACE("Interface"),
        ENUM("Enum"),

        CLASS("Class"),

        STRUCT("Struct")
        ;

        companion object {
            fun getClassKind(descriptor: ClassDescriptor): DetailedClassKind {
                if (DescriptorUtils.isEnumEntry(descriptor)) return ENUM_ENTRY


                if (DescriptorUtils.isInterface(descriptor)) return INTERFACE

                return CLASS
            }
        }
    }

}
