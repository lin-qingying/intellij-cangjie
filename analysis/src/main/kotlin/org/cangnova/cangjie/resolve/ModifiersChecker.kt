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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.extensions.DeclarationAttributeAltererExtension
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.DiagnosticFactory1
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.caches.DeclarationChecker
import org.cangnova.cangjie.resolve.caches.DeclarationCheckerContext
import org.cangnova.cangjie.resolve.check.UnderscoreChecker
import org.cangnova.cangjie.resolve.deprecation.DeprecationResolver

class ModifiersChecker(
    val declarationCheckers: Iterable<DeclarationChecker>,
    val moduleDescriptor: ModuleDescriptor,
    val deprecationResolver: DeprecationResolver,

//val expectActualTracker:ExpectActualTracker,
    val missingSupertypesResolver: MissingSupertypesResolver,
    val languageVersionSettings: LanguageVersionSettings
) {


    fun withTrace(trace: BindingTrace): ModifiersCheckingProcedure {
        return ModifiersCheckingProcedure(trace)
    }


    inner class ModifiersCheckingProcedure(val trace: BindingTrace) {

        fun checkParameterHasNoLetOrVar(
            parameter: CjLetVarKeywordOwner,
            diagnosticFactory: DiagnosticFactory1<PsiElement, CjKeywordToken>
        ) {
            val valOrVar = parameter.letOrVarKeyword
            if (valOrVar != null) {
                trace.report(
                    diagnosticFactory.on(
                        valOrVar,
                        (valOrVar.node.elementType as CjKeywordToken)
                    )
                )
            }
        }

        fun checkModifiersForDestructuringDeclaration(multiDeclaration: CjDestructuringDeclaration) {
//            annotationChecker.check(multiDeclaration, trace, null)
            ModifierCheckerCore.check(multiDeclaration, trace, null, languageVersionSettings)
            for (multiEntry in multiDeclaration.entries) {
//                annotationChecker.check(multiEntry, trace, null)
                ModifierCheckerCore.check(multiEntry, trace, null, languageVersionSettings)
                UnderscoreChecker.checkNamed(
                    multiEntry,
                    trace,
                    languageVersionSettings,  /* allowSingleUnderscore = */
                    true
                )
            }
        }

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
            descriptor: DeclarationDescriptor
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
                modifierListOwner.typeParameters
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
            val context: DeclarationCheckerContext = DeclarationCheckerContext(
                trace, languageVersionSettings, deprecationResolver, moduleDescriptor, /*expectActualTracker,*/
                missingSupertypesResolver
            )
            for (checker in declarationCheckers) {
                ProgressManager.checkCanceled()
                checker.check(declaration, descriptor, context)
            }
            OperatorModifierChecker.check(declaration, descriptor, trace, languageVersionSettings)
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
            if (modifierListOwner is CjMainFunction) return DescriptorVisibilities.PUBLIC
            return resolveVisibilityFromModifiers(
                modifierListOwner.modifierList,
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
            modifierListOwner: CjModifierListOwner?,
            defaultModality: Modality,
            allowSealed: Boolean
        ): Modality {
            val modifierList = modifierListOwner?.modifierList


            if (modifierListOwner is CjNamedFunction || modifierListOwner is CjProperty) {
                if (containingDescriptor is MemberDescriptor && containingDescriptor is ClassDescriptor) {

                    if (containingDescriptor.modality == Modality.ABSTRACT && containingDescriptor.kind == ClassKind.CLASS) {
                        if (modifierListOwner is CjProperty && !modifierListOwner.hasBody()) {

                            return Modality.ABSTRACT

                        } else if (modifierListOwner is CjNamedFunction && !modifierListOwner.hasBody()) {
                            return Modality.ABSTRACT

                        }
                    }
                }


            }
            if (modifierList == null) {
                return defaultModality
            }
            val hasAbstractModifier = modifierList.hasModifier(CjTokens.ABSTRACT_KEYWORD)
            val hasOverrideModifier = modifierList.hasModifier(CjTokens.OVERRIDE_KEYWORD)

            if (allowSealed && modifierList.hasModifier(CjTokens.SEALED_KEYWORD)) {
                return Modality.SEALED
            }
            if (modifierList.hasModifier(CjTokens.OPEN_KEYWORD)) {
                if (containingDescriptor is ClassDescriptor) {
                    val classOrInterface: ClassDescriptor =
                        containingDescriptor
                    if (classOrInterface.kind == ClassKind.INTERFACE/* && classOrInterface.isExpect()*/) {
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
//            if (hasOverrideModifier /*&& !hasFinalModifier*/ && defaultModality != Modality.ABSTRACT) {
//                return Modality.OPEN
//            }
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

            var modality =
                resolveModalityFromModifiers(
                    containingDescriptor,
                    modifierListOwner,
                    defaultModality,
                    allowSealed
                )

            if (modifierListOwner != null) {
                val extensions: Collection<DeclarationAttributeAltererExtension> =
                    DeclarationAttributeAltererExtension.EP_NAME.extensionList

                val descriptor =
                    bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, modifierListOwner]
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
            if (modifierList.hasModifier(CjTokens.SEALED_KEYWORD)) return DescriptorVisibilities.PUBLIC

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
