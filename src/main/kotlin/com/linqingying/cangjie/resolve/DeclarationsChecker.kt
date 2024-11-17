package com.linqingying.cangjie.resolve

import com.intellij.psi.PsiElement
import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.AnnotationDescriptor
import com.linqingying.cangjie.descriptors.impl.PropertyAccessorDescriptor
import com.linqingying.cangjie.descriptors.macro.MacroDescriptor
import com.linqingying.cangjie.diagnostics.DiagnosticFactory0
import com.linqingying.cangjie.diagnostics.Errors.*
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.resolve.BindingContext.*
import com.linqingying.cangjie.resolve.DescriptorUtils.classCanHaveAbstractDeclaration
import com.linqingying.cangjie.resolve.calls.results.TypeSpecificityComparator
import com.linqingying.cangjie.resolve.descriptorUtil.isEffectivelyExternal
import com.linqingying.cangjie.resolve.source.CangJieSourceElement
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.checker.CangJieTypeChecker
import com.linqingying.cangjie.types.checker.SimpleClassicTypeSystemContext.isNothing
import com.linqingying.cangjie.types.util.TypeUtils
import com.linqingying.cangjie.types.util.constituentTypes
import com.linqingying.cangjie.types.util.contains
import com.linqingying.cangjie.types.util.isArrayOfNothing

class DeclarationsChecker(
    private val builtins: CangJieBuiltIns,
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
    private val exposedChecker = ExposedVisibilityChecker(languageVersionSettings, trace)
    private val shadowedExtensionChecker = ShadowedExtensionChecker(typeSpecificityComparator, trace)

    private val modifiersChecker = modifiersChecker.withTrace(trace)
    private fun checkClass(classDescriptor: ClassDescriptorWithResolutionScopes, typeStatement: CjTypeStatement) {
        checkSupertypesForConsistency(classDescriptor, typeStatement)
//        checkLocalAnnotation(classDescriptor, classOrObject)
        checkTypesInClassHeader(typeStatement)

        when (typeStatement) {
            is CjClass, is CjInterface, is CjStruct, is CjExtend, is CjEnum -> {
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
//    private val shadowedExtensionChecker = ShadowedExtensionChecker(typeSpecificityComparator, trace)
//private val exposedChecker = ExposedVisibilityChecker(languageVersionSettings, trace)

    private fun hasConstraints(typeParameter: CjTypeParameter, constraints: List<CjTypeConstraint>): Boolean {
        if (typeParameter.name == null) return false
        return constraints.any { it.subjectTypeParameterName?.text == typeParameter.name }
    }


    private fun checkOnlyOneTypeParameterBound(
        descriptor: TypeParameterDescriptor, declaration: CjTypeParameter, owner: CjTypeParameterListOwner
    ) {
        val upperBounds = descriptor.upperBounds
        val (boundsWhichAreTypeParameters, otherBounds) = upperBounds
            .map(CangJieType::constructor)
            .partition { constructor -> constructor.declarationDescriptor is TypeParameterDescriptor }
            .let { pair -> pair.first.toSet() to pair.second.toSet() }
        if (boundsWhichAreTypeParameters.size > 1 || (boundsWhichAreTypeParameters.size == 1 && otherBounds.isNotEmpty())) {
            val reportOn = if (boundsWhichAreTypeParameters.size + otherBounds.size == 2) {
                // If there's only one problematic bound (either 2 type parameter bounds, or 1 type parameter bound + 1 other bound),
                // report the diagnostic on that bound

                val allBounds: List<Pair<CjTypeReference, CangJieType?>> =
                    owner.typeConstraints
                        .filter { constraint ->
                            constraint.subjectTypeParameterName?.getReferencedNameAsName() == declaration.nameAsName
                        }
                        .mapNotNull { constraint -> constraint.boundTypeReference }
                        .map { typeReference -> typeReference to trace.bindingContext.get(TYPE, typeReference) }

                val problematicBound =
                    allBounds.firstOrNull { bound -> bound.second?.constructor != boundsWhichAreTypeParameters.first() }

                problematicBound?.first ?: declaration
            } else {
                // Otherwise report the diagnostic on the type parameter declaration
                declaration
            }

            trace.report(BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER.on(reportOn))
        }
    }

    private fun checkAbstractFunctionReturnType(function: CjNamedFunction, descriptor: FunctionDescriptor) {
        if (!function.hasBody() && function.typeReference == null && (descriptor.modality == Modality.ABSTRACT || descriptor.modality == Modality.OPEN)) {
            trace.report(ABSTRACT_FUNCTION_WITHOUT_RETURN_TYPE.on(function))
        }
    }

    private fun checkTypeParameterConstraints(typeParameterListOwner: CjTypeParameterListOwner) {
        val constraints = typeParameterListOwner.typeConstraints
        if (constraints.isEmpty()) return

        for (typeParameter in typeParameterListOwner.typeParameters) {
            if (typeParameter.extendsBound != null && hasConstraints(typeParameter, constraints)) {
                trace.report(MISPLACED_TYPE_PARAMETER_CONSTRAINTS.on(typeParameter))
            }
            val typeParameterDescriptor = trace[TYPE_PARAMETER, typeParameter] ?: continue
            checkSupertypesForConsistency(typeParameterDescriptor, typeParameter)
            checkOnlyOneTypeParameterBound(typeParameterDescriptor, typeParameter, typeParameterListOwner)
        }

//        for (constraint in constraints) {
//            constraint.annotationEntries.forEach {
//                trace.report(ANNOTATION_IN_WHERE_CLAUSE_WARNING.on(it))
//            }
//        }
    }

    private fun checkImplicitCallableType(declaration: CjCallableDeclaration, descriptor: CallableDescriptor) {
        descriptor.returnType?.unwrap()?.let {
            val target = declaration.nameIdentifier ?: declaration
            if (declaration.typeReference == null) {
                if (it.isNothing() && !declaration.hasModifier(CjTokens.OVERRIDE_KEYWORD)) {
                    trace.report(
                        (if (declaration is CjProperty) IMPLICIT_NOTHING_PROPERTY_TYPE else IMPLICIT_NOTHING_RETURN_TYPE).on(
                            target
                        )
                    )
                }
                if (it.contains { type -> type.constructor is IntersectionTypeConstructor }) {
                    trace.report(IMPLICIT_INTERSECTION_TYPE.on(target, it))
                }
            } else if (it.isNothing() && it is AbbreviatedType) {
                trace.report(
                    (if (declaration is CjProperty) ABBREVIATED_NOTHING_PROPERTY_TYPE else ABBREVIATED_NOTHING_RETURN_TYPE).on(
                        target
                    )
                )
            }
        }
    }

    private fun checkVarargParameters(trace: BindingTrace, callableDescriptor: CallableDescriptor) {

    }

    fun checkMacro(macro: CjMacroDeclaration, macroDeclaration: MacroDescriptor) {
//检查返回值类型，检查参数数量，检查参数类型

        val returnType = macroDeclaration.returnType
        if (returnType == null || !CangJieTypeChecker.DEFAULT.equalTypes(returnType, builtins.tokensType)) {
            trace.report(
                INVALID_MACRO_TYPE.on(
                    macro.typeReference ?: macro.colon ?: macro,
                    "return",
                    builtins.tokensType
                )
            )
        }
//        参数数量
        val valueParameters = macroDeclaration.valueParameters
        if (valueParameters.size > 2) {
            trace.report(EXCESSIVE_MACRO_PARAMS.on(macro.valueParameterList))
        }
//        检查参数类型


        macro.valueParameters.forEachIndexed { index, valueParameter ->
            if (index < valueParameters.size) {
                if (!CangJieTypeChecker.DEFAULT.equalTypes(valueParameters[index].type, builtins.tokensType)) {
                    trace.report(
                        INVALID_MACRO_TYPE.on(
                            valueParameter,
                            "parameter",
                            builtins.tokensType
                        )
                    )
                }
            }
        }


    }

    fun checkFunction(function: CjNamedFunction, functionDescriptor: SimpleFunctionDescriptor) {
//        val typeParameterList = function.typeParameterList
//        val nameIdentifier = function.nameIdentifier
//        if (typeParameterList != null && nameIdentifier != null &&
//            typeParameterList.textRange.startOffset > nameIdentifier.textRange.startOffset
//        ) {
//            trace.report(DEPRECATED_TYPE_PARAMETER_SYNTAX.on(typeParameterList))
//        }

        checkAbstractFunctionReturnType(function, functionDescriptor)
        checkTypeParameterConstraints(function)
        checkImplicitCallableType(function, functionDescriptor)
//        exposedChecker.checkFunction(function, functionDescriptor)
        checkVarargParameters(trace, functionDescriptor)

        val containingDescriptor = functionDescriptor.containingDeclaration
        val hasAbstractModifier = function.hasModifier(CjTokens.ABSTRACT_KEYWORD)
        val hasExternalModifier = functionDescriptor.isEffectivelyExternal()

        if (containingDescriptor is ClassDescriptor) {
            val inInterface = containingDescriptor.kind == ClassKind.INTERFACE
            val isExpectClass = containingDescriptor.isExpect
            if (hasAbstractModifier && !classCanHaveAbstractDeclaration(containingDescriptor)) {
                trace.report(
                    ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS.on(
                        function,
                        functionDescriptor.name.asString(),
                        containingDescriptor
                    )
                )
            }
            val hasBody = function.hasBody()
            if (hasBody && hasAbstractModifier) {
                trace.report(ABSTRACT_FUNCTION_WITH_BODY.on(function, functionDescriptor))
            }
            if (!hasBody && inInterface) {
                if (function.hasModifier(CjTokens.PRIVATE_KEYWORD)) {
                    trace.report(PRIVATE_FUNCTION_WITH_NO_BODY.on(function, functionDescriptor))
                }
                if (!containingDescriptor.isExpect && !hasAbstractModifier && function.hasModifier(CjTokens.OPEN_KEYWORD)) {
                    trace.report(REDUNDANT_OPEN_IN_INTERFACE.on(function))
                }
            }
//            if (!hasBody && !hasAbstractModifier && !hasExternalModifier && !inInterface && !isExpectClass &&
//                diagnosticSuppressor.shouldReportNoBody(functionDescriptor)
//            ) {
//                trace.report(NON_ABSTRACT_FUNCTION_WITH_NO_BODY.on(function, functionDescriptor))
//            }
        } else /* top-level only */ {
//            if (!function.hasBody() && !hasAbstractModifier && !hasExternalModifier && !functionDescriptor.isExpect &&
//                diagnosticSuppressor.shouldReportNoBody(functionDescriptor)
//            ) {
//                trace.report(NON_MEMBER_FUNCTION_NO_BODY.on(function, functionDescriptor))
//            }
        }

        if (functionDescriptor.isExpect) {
            checkExpectedFunction(function, functionDescriptor)
        }

        shadowedExtensionChecker.checkDeclaration(function, functionDescriptor)
    }

    private fun checkExpectDeclarationModifiers(declaration: CjDeclaration, descriptor: MemberDescriptor) {
        if (!descriptor.isExpect) return

        if (DescriptorVisibilities.isPrivate(descriptor.visibility)) {
            trace.report(
                EXPECTED_PRIVATE_DECLARATION.on(
                    declaration.modifierList?.getModifier(CjTokens.PRIVATE_KEYWORD) ?: declaration
                )
            )
        }

        checkExpectDeclarationHasNoExternalModifier(declaration)
//        if (declaration is CjFunction && languageVersionSettings.supportsFeature(LanguageFeature.MultiplatformRestrictions)) {
//            declaration.modifierList?.getModifier(CjTokens.TAILREC_KEYWORD)?.let {
//                trace.report(EXPECTED_TAILREC_FUNCTION.on(it))
//            }
//        }
    }

    private fun checkExpectDeclarationHasNoExternalModifier(declaration: CjDeclaration) {
//        if (languageVersionSettings.supportsFeature(LanguageFeature.MultiplatformRestrictions)) {
//            declaration.modifierList?.getModifier(CjTokens.EXTERNAL_KEYWORD)?.let {
//                trace.report(EXPECTED_EXTERNAL_DECLARATION.on(it))
//            }
//        }
    }

    private fun checkExpectedFunction(function: CjNamedFunction, functionDescriptor: FunctionDescriptor) {
        if (function.hasBody()) {
            trace.report(EXPECTED_DECLARATION_WITH_BODY.on(function))
        }

        checkExpectDeclarationModifiers(function, functionDescriptor)
    }

    private fun checkMemberProperty(
        property: CjProperty,
        propertyDescriptor: PropertyDescriptor,
        classDescriptor: ClassDescriptor
    ) {
        val modifierList = property.modifierList

        if (modifierList != null) {
            if (modifierList.hasModifier(CjTokens.ABSTRACT_KEYWORD)) {
                //has abstract modifier
                if (!classCanHaveAbstractDeclaration(classDescriptor)) {
                    trace.report(
                        ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS.on(
                            property,
                            property.name ?: "",
                            classDescriptor
                        )
                    )
                    return
                }
            } else if (classDescriptor.kind == ClassKind.INTERFACE &&
                modifierList.hasModifier(CjTokens.OPEN_KEYWORD) &&
                propertyDescriptor.modality == Modality.ABSTRACT
            ) {
                trace.report(REDUNDANT_OPEN_IN_INTERFACE.on(property))
            }
        }

        if (propertyDescriptor.modality == Modality.ABSTRACT) {

            val getter = property.getter
            if (getter != null && getter.hasBody()) {
                trace.report(ABSTRACT_PROPERTY_WITH_GETTER.on(getter))
            }
            val setter = property.setter
            if (setter != null && setter.hasBody()) {
                trace.report(ABSTRACT_PROPERTY_WITH_SETTER.on(setter))
            }
        }
    }

    private fun checkBackingField(property: CjProperty) {
//        property.fieldDeclaration?.let {
//            trace.report(EXPLICIT_BACKING_FIELDS_UNSUPPORTED.on(it))
//        }
    }

    private fun checkAccessors(property: CjProperty, propertyDescriptor: PropertyDescriptor) {
        for (accessorDescriptor in propertyDescriptor.accessors) {
            val accessor = if (accessorDescriptor is PropertyGetterDescriptor) property.getter else property.setter
            if (accessor != null) {
                modifiersChecker.checkModifiersForDeclaration(accessor, accessorDescriptor)
                identifierChecker.checkDeclaration(accessor, trace)
            } else {
                modifiersChecker.runDeclarationCheckers(property, accessorDescriptor)
            }
        }
        checkAccessor(propertyDescriptor, property.getter, propertyDescriptor.getter)
        checkAccessor(propertyDescriptor, property.setter, propertyDescriptor.setter)
    }

    private fun checkAccessor(
        propertyDescriptor: PropertyDescriptor,
        accessor: CjPropertyAccessor?,
        accessorDescriptor: PropertyAccessorDescriptor?
    ) {
        if (accessor == null || accessorDescriptor == null) return
        if (propertyDescriptor.isExpect && accessor.hasBody()) {
            trace.report(EXPECTED_DECLARATION_WITH_BODY.on(accessor))
        }

//        val accessorModifierList = accessor.modifierList ?: return
//        val tokens = modifiersChecker.getTokensCorrespondingToModifiers(
//            accessorModifierList,
//            setOf(CjTokens.PUBLIC_KEYWORD, CjTokens.PROTECTED_KEYWORD, CjTokens.PRIVATE_KEYWORD, CjTokens.INTERNAL_KEYWORD)
//        )
//        if (accessor.isGetter) {
//            if (accessorDescriptor.visibility != propertyDescriptor.visibility) {
//                reportVisibilityModifierDiagnostics(tokens.values, GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY)
//            } else {
//                reportVisibilityModifierDiagnostics(tokens.values, REDUNDANT_MODIFIER_IN_GETTER)
//            }
//        } else {
//            if (propertyDescriptor.isOverridable
//                && accessorDescriptor.visibility == DescriptorVisibilities.PRIVATE
//                && propertyDescriptor.visibility != DescriptorVisibilities.PRIVATE
//            ) {
//                if (propertyDescriptor.modality == Modality.ABSTRACT) {
//                    reportVisibilityModifierDiagnostics(tokens.values, PRIVATE_SETTER_FOR_ABSTRACT_PROPERTY)
//                } else {
//                    reportVisibilityModifierDiagnostics(tokens.values, PRIVATE_SETTER_FOR_OPEN_PROPERTY)
//                }
//            } else {
//                val compare = DescriptorVisibilities.compare(accessorDescriptor.visibility, propertyDescriptor.visibility)
//                if (compare == null || compare > 0) {
//                    reportVisibilityModifierDiagnostics(tokens.values, SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY)
//                }
//            }
//        }
        if (propertyDescriptor.isExpect) {
            checkExpectDeclarationHasNoExternalModifier(accessor)
        }
    }

    private fun checkProperty(property: CjProperty, propertyDescriptor: PropertyDescriptor) {
        val containingDeclaration = propertyDescriptor.containingDeclaration
        if (containingDeclaration is ClassDescriptor) {
            checkMemberProperty(property, propertyDescriptor, containingDeclaration)
        }


        checkAccessors(property, propertyDescriptor)
        checkTypeParameterConstraints(property)
//        exposedChecker.checkProperty(property, propertyDescriptor)
        shadowedExtensionChecker.checkDeclaration(property, propertyDescriptor)
//        checkPropertyTypeParametersAreUsedInReceiverType(propertyDescriptor)
        checkImplicitCallableType(property, propertyDescriptor)
        checkExpectDeclarationModifiers(property, propertyDescriptor)
        checkBackingField(property)
    }

    fun checkVariable(variable: CjVariable, variableDescriptor: VariableDescriptor) {
        val containingDeclaration = variableDescriptor.containingDeclaration
        if (containingDeclaration is ClassDescriptor) {
            checkMemberVariable(variable, variableDescriptor, containingDeclaration)
        }
        checkVariableInitializer(variable, variableDescriptor)
        shadowedExtensionChecker.checkDeclaration(variable, variableDescriptor)

    }

    private fun checkVariableInitializer(variable: CjVariable, variableDescriptor: VariableDescriptor) {

        val containingDeclaration = variableDescriptor.containingDeclaration
        val inInterface = DescriptorUtils.isInterface(containingDeclaration)
//        if (variableDescriptor.modality == Modality.ABSTRACT) {
//
//            if (inInterface && variable.hasModifier(CjTokens.PRIVATE_KEYWORD) && !variable.hasModifier(CjTokens.ABSTRACT_KEYWORD)) {
//                trace.report(PRIVATE_PROPERTY_IN_INTERFACE.on(variable))
//            }
//            return
//        }
        val initializer = variable.initializer
        val isExpect = variableDescriptor.isExpect

        if (initializer != null) {
            when {
                inInterface -> trace.report(VARIABLE_INITIALIZER_IN_INTERFACE.on(initializer))
                isExpect -> trace.report(EXPECTED_VARIABLE_INITIALIZER.on(initializer))

                variable.receiverTypeReference != null -> trace.report(
                    EXTENSION_VARIABLE_WITH_BACKING_FIELD.on(
                        initializer
                    )
                )

                variable.contextReceivers.isNotEmpty() -> trace.report(
                    CONTEXT_RECEIVERS_WITH_BACKING_FIELD.on(
                        initializer
                    )
                )
            }
        } else {
            val isUninitialized = trace.bindingContext.get(IS_UNINITIALIZED, variableDescriptor) ?: false
            val isExternal = variableDescriptor.isEffectivelyExternal()
            if (!inInterface && !isExpect && isUninitialized && !isExternal) {


                reportMustBeInitialized(
                    variableDescriptor,
                    containingDeclaration,
                    false,
                    variable,
                    false,
                    languageVersionSettings,
                    trace
                )

            }/* else if (variable.typeReference == null && !languageVersionSettings.supportsFeature(LanguageFeature.ShortSyntaxForPropertyGetters)) {
                trace.report(
                    UNSUPPORTED_FEATURE.on(
                        variable,
                        LanguageFeature.ShortSyntaxForPropertyGetters to languageVersionSettings
                    )
                )
            } */


        }
    }

    private fun reportMustBeInitialized(
        variableDescriptor: VariableDescriptor,
        containingDeclaration: DeclarationDescriptor,
        hasAnyAccessorImplementation: Boolean,
        variable: CjVariable,
        isOpenValDeferredInitDeprecationWarning: Boolean,
        languageVersionSettings: LanguageVersionSettings,
        trace: BindingTrace,
    ) {


        val factory = when {

            else -> MUST_BE_INITIALIZED
        }
        trace.report(
            when (isOpenValDeferredInitDeprecationWarning) {
                true -> factory.deprecationWarning
                false -> factory
            }.on(variable)
        )
    }

    private val DiagnosticFactory0<CjVariable>.deprecationWarning: DiagnosticFactory0<CjVariable>
        get() = when (this) {
//            MUST_BE_INITIALIZED -> MUST_BE_INITIALIZED_WARNING
//            MUST_BE_INITIALIZED_OR_BE_ABSTRACT -> MUST_BE_INITIALIZED_OR_BE_ABSTRACT_WARNING
//            MUST_BE_INITIALIZED_OR_BE_FINAL -> MUST_BE_INITIALIZED_OR_BE_FINAL_WARNING
//            MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT -> MUST_BE_INITIALIZED_OR_FINAL_OR_ABSTRACT_WARNING
            else -> error("Only MUST_BE_INITIALIZED is supported")
        }

    private fun checkMemberVariable(
        variable: CjVariable,
        propertyDescriptor: VariableDescriptor,
        classDescriptor: ClassDescriptor
    ) {
        val modifierList = variable.modifierList

        if (modifierList != null) {
            if (modifierList.hasModifier(CjTokens.ABSTRACT_KEYWORD)) {
                //has abstract modifier
                if (!classCanHaveAbstractDeclaration(classDescriptor)) {
                    trace.report(
                        ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS.on(
                            variable,
                            variable.name ?: "",
                            classDescriptor
                        )
                    )
                    return
                }
            } else if (classDescriptor.kind == ClassKind.INTERFACE &&
                modifierList.hasModifier(CjTokens.OPEN_KEYWORD) &&
                propertyDescriptor.modality == Modality.ABSTRACT
            ) {
                trace.report(REDUNDANT_OPEN_IN_INTERFACE.on(variable))
            }
        }

        if (propertyDescriptor.modality == Modality.ABSTRACT) {
            variable.initializer?.let { trace.report(ABSTRACT_PROPERTY_WITH_INITIALIZER.on(it)) }
        }
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

        for ((function, functionDescriptor) in bodiesResolveContext.functions.entries) {
            checkFunction(function, functionDescriptor)
            modifiersChecker.checkModifiersForDeclaration(function, functionDescriptor)
            identifierChecker.checkDeclaration(function, trace)
        }
        for ((function, macroDescriptor) in bodiesResolveContext.macros.entries) {
            checkMacro(function, macroDescriptor)
            modifiersChecker.checkModifiersForDeclaration(function, macroDescriptor)
            identifierChecker.checkDeclaration(function, trace)
        }
        for ((variable, variableDescriptor) in bodiesResolveContext.variables.entries) {
            checkVariable(variable, variableDescriptor)
            modifiersChecker.checkModifiersForDeclaration(variable, variableDescriptor)
            identifierChecker.checkDeclaration(variable, trace)
        }
        for ((variable, variableDescriptors) in bodiesResolveContext.variablesByPattern.entries) {
            variableDescriptors.forEach {
                checkVariable(variable, it)
                modifiersChecker.checkModifiersForDeclaration(variable, it)
                identifierChecker.checkDeclaration(variable, trace)
            }

        }
        for ((property, propertyDescriptor) in bodiesResolveContext.properties.entries) {
            checkProperty(property, propertyDescriptor)
            modifiersChecker.checkModifiersForDeclaration(property, propertyDescriptor)
            identifierChecker.checkDeclaration(property, trace)
        }
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
        for ((declaration, typeAliasDescriptor) in bodiesResolveContext.typeAliases.entries) {
            checkTypeAliasDeclaration(declaration, typeAliasDescriptor)
            modifiersChecker.checkModifiersForDeclaration(declaration, typeAliasDescriptor)
            exposedChecker.checkTypeAlias(declaration, typeAliasDescriptor)
        }
    }

    private class TypeAliasDeclarationCheckingReportStrategy(
        private val trace: BindingTrace,
        typeAliasDescriptor: TypeAliasDescriptor,
        declaration: CjTypeAlias,
        val upperBoundChecker: UpperBoundChecker
    ) : TypeAliasExpansionReportStrategy {
        private val typeReference = declaration.getTypeReference()
            ?: throw AssertionError("Incorrect type alias declaration for $typeAliasDescriptor")

        override fun wrongNumberOfTypeArguments(typeAlias: TypeAliasDescriptor, numberOfParameters: Int) {
            // Do nothing: this should've been reported during type resolution.
        }

        override fun conflictingProjection(
            typeAlias: TypeAliasDescriptor,
            typeParameter: TypeParameterDescriptor?,
            substitutedArgument: CangJieType
        ) {
            trace.report(CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION.on(typeReference, substitutedArgument))
        }

        override fun recursiveTypeAlias(typeAlias: TypeAliasDescriptor) {
            trace.report(RECURSIVE_TYPEALIAS_EXPANSION.on(typeReference, typeAlias))
        }

        override fun boundsViolationInSubstitution(
            substitutor: TypeSubstitutor,
            unsubstitutedArgument: CangJieType,
            argument: CangJieType,
            typeParameter: TypeParameterDescriptor
        ) {
            upperBoundChecker.checkBounds(null, argument, typeParameter, substitutor, trace, typeReference)
        }

        override fun repeatedAnnotation(annotation: AnnotationDescriptor) {
            val annotationEntry = (annotation.source as? CangJieSourceElement)?.psi as? CjAnnotationEntry ?: return
            trace.report(REPEATED_ANNOTATION.on(annotationEntry))
        }
    }

    private fun checkTypeAliasExpansion(declaration: CjTypeAlias, typeAliasDescriptor: TypeAliasDescriptor) {
        val typeAliasExpansion = TypeAliasExpansion.createWithFormalArguments(typeAliasDescriptor)
        val reportStrategy =
            TypeAliasDeclarationCheckingReportStrategy(trace, typeAliasDescriptor, declaration, upperBoundChecker)
        TypeAliasExpander(reportStrategy, true).expandWithoutAbbreviation(typeAliasExpansion, TypeAttributes.Empty)
    }

    private fun checkTypeAliasDeclaration(declaration: CjTypeAlias, typeAliasDescriptor: TypeAliasDescriptor) {
        val typeReference = declaration.getTypeReference() ?: return

        checkTypeAliasExpansion(declaration, typeAliasDescriptor)

        val expandedType = typeAliasDescriptor.expandedType
        if (expandedType.isError) return

        val expandedClassifier = expandedType.constructor.declarationDescriptor

        if (expandedType.isDynamic() || expandedClassifier is TypeParameterDescriptor) {
            trace.report(TYPEALIAS_SHOULD_EXPAND_TO_CLASS.on(typeReference, expandedType))
        }

        if (TypeUtils.contains(expandedType) { it.isArrayOfNothing() }) {
            trace.report(
                TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE.on(
                    typeReference,
                    expandedType,
                    "Array<Nothing> is illegal"
                )
            )
        }

        val usedTypeAliasParameters: Set<TypeParameterDescriptor> =
            getUsedTypeAliasParameters(expandedType, typeAliasDescriptor)
        for (typeParameter in typeAliasDescriptor.declaredTypeParameters) {
            if (typeParameter !in usedTypeAliasParameters) {
                val source = DescriptorToSourceUtils.descriptorToDeclaration(typeParameter) as? CjTypeParameter
                    ?: throw AssertionError("No source element for type parameter $typeParameter of $typeAliasDescriptor")
                trace.report(UNUSED_TYPEALIAS_PARAMETER.on(source, typeParameter, expandedType))
            }
        }


    }


    private fun getUsedTypeAliasParameters(
        type: CangJieType,
        typeAlias: TypeAliasDescriptor
    ): Set<TypeParameterDescriptor> =
        type.constituentTypes().mapNotNullTo(HashSet()) { cangJieType ->
            val descriptor = cangJieType.constructor.declarationDescriptor as? TypeParameterDescriptor
            descriptor?.takeIf { it.containingDeclaration == typeAlias }
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

private fun VariableDescriptor.getEffectiveModality(): Modality =
    when (modality == Modality.OPEN && (containingDeclaration as? ClassDescriptor)?.modality == Modality.FINAL) {
        true -> Modality.FINAL
        false -> modality
    }
