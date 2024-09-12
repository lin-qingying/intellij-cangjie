package com.huawei.cangjie.resolve

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.diagnostics.Errors.*
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.BindingContext.TYPE
import com.huawei.cangjie.resolve.calls.results.TypeSpecificityComparator
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.SubstitutionUtils
import com.huawei.cangjie.types.checker.CangJieTypeChecker
import com.intellij.psi.PsiElement

class DeclarationsChecker(
    private val descriptorResolver: DescriptorResolver,
    modifiersChecker: ModifiersChecker,
    private val annotationChecker: AnnotationChecker,
    private val identifierChecker: IdentifierChecker,
    private val trace: BindingTrace,
    private val languageVersionSettings: LanguageVersionSettings,
    typeSpecificityComparator: TypeSpecificityComparator,
//    private val diagnosticSuppressor: PlatformDiagnosticSuppressor,
    private val upperBoundChecker: UpperBoundChecker
) {
//    private val exposedChecker = ExposedVisibilityChecker(languageVersionSettings, trace)

    private val modifiersChecker = modifiersChecker.withTrace(trace)
    private fun checkClass(classDescriptor: ClassDescriptorWithResolutionScopes, typeStatement: CjTypeStatement) {
        checkSupertypesForConsistency(classDescriptor, typeStatement)
//        checkLocalAnnotation(classDescriptor, classOrObject)
        checkTypesInClassHeader(typeStatement)

        when (typeStatement) {
            is CjClass, is CjInterface, is CjStruct, is CjExtend,is CjEnum -> {
//
                descriptorResolver.checkNamesInConstraints(
                    typeStatement, classDescriptor, classDescriptor.scopeForClassHeaderResolution, trace
                )
            }


        }

//        checkPrimaryConstructor(classOrObject, classDescriptor)

//        checkExpectDeclarationModifiers(classOrObject, classDescriptor)
    }

    private fun checkSupertypesForConsistency(classifier: ClassifierDescriptor, sourceElement: PsiElement) {
        if (classifier is TypeParameterDescriptor) {
            val immediateUpperBounds = classifier.upperBounds.map { it.constructor }
            if (immediateUpperBounds.size != immediateUpperBounds.toSet().size) {
                // If there are duplicate type constructors among the _immediate_ upper bounds,
                // then the REPEATED_BOUNDS diagnostic would be already reported for those bounds of this type parameter
                return
            }
        }

        val multiMap = SubstitutionUtils.buildDeepSubstitutionMultimap(classifier.defaultType)
        for ((typeParameterDescriptor, projections) in multiMap.asMap()) {
            if (projections.size <= 1) continue

            // Immediate arguments of supertypes cannot be projected
            val conflictingTypes = projections.map { it.type }.toMutableSet()
            removeDuplicateTypes(conflictingTypes)
            if (conflictingTypes.size <= 1) continue

            val containingDeclaration = typeParameterDescriptor.containingDeclaration as? ClassDescriptor
                ?: throw AssertionError("Not a class descriptor: " + typeParameterDescriptor.containingDeclaration)
            if (sourceElement is CjTypeStatement) {
                val delegationSpecifierList = sourceElement.getSuperTypeList() ?: continue
                trace.report(
                    INCONSISTENT_TYPE_PARAMETER_VALUES.on(
                        delegationSpecifierList, typeParameterDescriptor, containingDeclaration, conflictingTypes
                    )
                )
            } else if (sourceElement is CjTypeParameter) {
                trace.report(
                    INCONSISTENT_TYPE_PARAMETER_BOUNDS.on(
                        sourceElement, typeParameterDescriptor, containingDeclaration, conflictingTypes
                    )
                )
            }
        }
    }

    private fun checkTypesInClassHeader(classOrObject: CjTypeStatement) {
        fun CjTypeReference.type(): CangJieType? = trace.bindingContext.get(TYPE, this)

        for (delegationSpecifier in classOrObject.superTypeListEntries) {
            val typeReference = delegationSpecifier.typeReference ?: continue
            typeReference.type()
                ?.let { upperBoundChecker.checkBoundsInSupertype(typeReference, it, trace, languageVersionSettings) }
        }

        if (classOrObject !is CjClass) return

        val upperBoundCheckRequests = ArrayList<DescriptorResolver.UpperBoundCheckRequest>()

        for (typeParameter in classOrObject.typeParameters) {
            val typeReference = typeParameter.extendsBound ?: continue
            val type = typeReference.type() ?: continue
            upperBoundCheckRequests.add(
                DescriptorResolver.UpperBoundCheckRequest(
                    typeParameter.nameAsName,
                    typeReference,
                    type
                )
            )
        }

        for (constraint in classOrObject.typeConstraints) {
            val typeReference = constraint.boundTypeReference ?: continue
            val type = typeReference.type() ?: continue
            val name = constraint.subjectTypeParameterName?.getReferencedNameAsName() ?: continue
            upperBoundCheckRequests.add(DescriptorResolver.UpperBoundCheckRequest(name, typeReference, type))
        }

        DescriptorResolver.checkUpperBoundTypes(trace, upperBoundCheckRequests, false)

        for (request in upperBoundCheckRequests) {
            upperBoundChecker.checkBoundsInSupertype(
                request.upperBound,
                request.upperBoundType,
                trace,
                languageVersionSettings
            )
        }
    }

    private fun checkModifiersAndAnnotationsInPackageDirective(file: CjFile) {
        val packageDirective = file.packageDirective ?: return
        val modifierList = packageDirective.modifierList ?: return

        for (annotationEntry in modifierList.annotationEntries) {
            val calleeExpression = annotationEntry.calleeExpression
            if (calleeExpression != null) {
                calleeExpression.constructorReferenceExpression?.let { trace.report(UNRESOLVED_REFERENCE.on(it, it)) }
            }
        }
        annotationChecker.check(packageDirective, trace, null)
        ModifierCheckerCore.check(
            packageDirective,
            trace,
            descriptor = null,
            languageVersionSettings = languageVersionSettings
        )
    }

    fun process(bodiesResolveContext: BodiesResolveContext) {
        for (file in bodiesResolveContext.files) {
            checkModifiersAndAnnotationsInPackageDirective(file)
//            annotationChecker.check(file, trace, null)
        }

        for ((classOrObject, classDescriptor) in bodiesResolveContext.declaredClasses.entries) {
            checkClass(classDescriptor, classOrObject)
            modifiersChecker.checkModifiersForDeclaration(classOrObject, classDescriptor)
            identifierChecker.checkDeclaration(classOrObject, trace)
//            exposedChecker.checkClassHeader(classOrObject, classDescriptor)
        }

//        for ((function, functionDescriptor) in bodiesResolveContext.functions.entries) {
//            checkFunction(function, functionDescriptor)
//            modifiersChecker.checkModifiersForDeclaration(function, functionDescriptor)
//            identifierChecker.checkDeclaration(function, trace)
//        }
//
//        for ((property, propertyDescriptor) in bodiesResolveContext.properties.entries) {
//            checkProperty(property, propertyDescriptor)
//            modifiersChecker.checkModifiersForDeclaration(property, propertyDescriptor)
//            identifierChecker.checkDeclaration(property, trace)
//        }
//
//        val destructuringDeclarations = bodiesResolveContext.destructuringDeclarationEntries.entries
//            .map { (entry, _) -> entry.parent }
//            .filterIsInstance<CjDestructuringDeclaration>()
//            .distinct()
//
//        for (multiDeclaration in destructuringDeclarations) {
//            modifiersChecker.checkModifiersForDestructuringDeclaration(multiDeclaration)
//            identifierChecker.checkDeclaration(multiDeclaration, trace)
//        }
//
//        for ((declaration, constructorDescriptor) in bodiesResolveContext.secondaryConstructors.entries) {
//            checkConstructorDeclaration(constructorDescriptor, declaration)
//            exposedChecker.checkFunction(declaration, constructorDescriptor)
//        }
//
//        for ((declaration, typeAliasDescriptor) in bodiesResolveContext.typeAliases.entries) {
//            checkTypeAliasDeclaration(declaration, typeAliasDescriptor)
//            modifiersChecker.checkModifiersForDeclaration(declaration, typeAliasDescriptor)
//            exposedChecker.checkTypeAlias(declaration, typeAliasDescriptor)
//        }
    }

    companion object {
        private fun removeDuplicateTypes(conflictingTypes: MutableSet<CangJieType>) {
            val iterator = conflictingTypes.iterator()
            while (iterator.hasNext()) {
                val type = iterator.next()
                for (otherType in conflictingTypes) {
                    val subtypeOf = CangJieTypeChecker.DEFAULT.equalTypes(type, otherType)
                    if (type !== otherType && subtypeOf) {
                        iterator.remove()
                        break
                    }
                }
            }
        }

    }
}
