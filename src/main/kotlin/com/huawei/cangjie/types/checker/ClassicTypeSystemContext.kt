package com.huawei.cangjie.types.checker

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.StandardNames.FqNames
import com.huawei.cangjie.builtins.functionTypeKind
import com.huawei.cangjie.builtins.functions.FunctionTypeKind
import com.huawei.cangjie.builtins.getFunctionDescriptor
import com.huawei.cangjie.builtins.isBuiltinExtensionFunctionalType
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.TypeAliasDescriptor
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.impl.AbstractTypeParameterDescriptor
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.SpecialNames
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.calls.inference.CapturedType
import com.huawei.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import com.huawei.cangjie.resolve.descriptorUtil.classId
import com.huawei.cangjie.resolve.scopes.SubstitutingScope
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.error.ErrorTypeKind
import com.huawei.cangjie.types.model.*
import com.huawei.cangjie.types.util.*
import com.huawei.cangjie.utils.firstIsInstanceOrNull
import com.intellij.util.containers.addIfNotNull
import com.huawei.cangjie.types.util.isSignedOrUnsignedNumberType as classicIsSignedOrUnsignedNumberType
import com.huawei.cangjie.types.util.isStubType as isSimpleTypeStubType
import com.huawei.cangjie.types.util.isStubTypeForBuilderInference as isSimpleTypeStubTypeForBuilderInference
import com.huawei.cangjie.types.util.isStubTypeForVariableInSubtyping as isSimpleTypeStubTypeForVariableInSubtyping

@Suppress("NOTHING_TO_INLINE")
private inline fun Any.errorMessage(): String {
    return "ClassicTypeSystemContext couldn't handle: $this, ${this::class}"
}

private fun errorSupportedOnlyInTypeInference(): Nothing {
    error("supported only in type inference context")
}

interface ClassicTypeSystemContext : TypeSystemInferenceExtensionContext, TypeSystemCommonBackendContext {


    val builtIns: CangJieBuiltIns
        get() = throw UnsupportedOperationException("Not supported")


    override fun TypeConstructorMarker.isDenotable(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return this.isDenotable
    }

    override fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return this is IntegerLiteralTypeConstructor
    }

    override fun createTypeWithUpperBoundForIntersectionResult(
        firstCandidate: CangJieTypeMarker,
        secondCandidate: CangJieTypeMarker,
    ): CangJieTypeMarker {
        require(firstCandidate is CangJieType, this::errorMessage)
        require(secondCandidate is CangJieType, this::errorMessage)

        (firstCandidate.constructor as? IntersectionTypeConstructor)?.let { intersectionConstructor ->
            val intersectionTypeWithAlternative = intersectionConstructor.setAlternative(secondCandidate).createType()
            return if (firstCandidate.isMarkedOption) intersectionTypeWithAlternative.makeNullableAsSpecified(true)
            else intersectionTypeWithAlternative

        } ?: error("Expected intersection type, found $firstCandidate")
    }

    override fun TypeConstructorMarker.isIntegerLiteralConstantTypeConstructor(): Boolean {
        return isIntegerLiteralTypeConstructor()
    }

    override fun TypeConstructorMarker.isIntegerConstantOperatorTypeConstructor(): Boolean {
        return false
    }

    override fun TypeConstructorMarker.isLocalType(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return declarationDescriptor?.classId?.isLocal == true
    }

    override fun captureFromExpression(type: CangJieTypeMarker): CangJieTypeMarker? {
        return captureFromExpressionInternal(type as UnwrappedType)
    }

    override fun TypeConstructorMarker.isAnonymous(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return declarationDescriptor?.classId?.shortClassName == SpecialNames.ANONYMOUS
    }

    override val TypeVariableTypeConstructorMarker.typeParameter: TypeParameterMarker?
        get() {
            require(this is NewTypeVariableConstructor, this::errorMessage)
            return this.originalTypeParameter
        }

    override fun SimpleTypeMarker.possibleIntegerTypes(): Collection<CangJieTypeMarker> {
        val typeConstructor = typeConstructor()
        require(typeConstructor is IntegerLiteralTypeConstructor, this::errorMessage)
        return typeConstructor.possibleTypes
    }

    override fun SimpleTypeMarker.withNullability(nullable: Boolean): SimpleTypeMarker {
        require(this is SimpleType, this::errorMessage)
        return this.makeNullableAsSpecified(nullable)
    }

    override fun CangJieTypeMarker.isError(): Boolean {
        require(this is CangJieType, this::errorMessage)
        return this.isError
    }

    override fun TypeConstructorMarker.toErrorType(): SimpleTypeMarker {
        require(this is TypeConstructor && ErrorUtils.isError(declarationDescriptor), this::errorMessage)
        return ErrorUtils.createErrorType(ErrorTypeKind.RESOLUTION_ERROR_TYPE, "from type constructor $this")
    }

    override fun CangJieTypeMarker.isUninferredParameter(): Boolean {
        require(this is CangJieType, this::errorMessage)
        return ErrorUtils.isUninferredTypeVariable(this)
    }

    override fun SimpleTypeMarker.isStubType(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return this.isSimpleTypeStubType()
    }

    override fun SimpleTypeMarker.isStubTypeForVariableInSubtyping(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return this.isSimpleTypeStubTypeForVariableInSubtyping()
    }

    override fun SimpleTypeMarker.isStubTypeForBuilderInference(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return this.isSimpleTypeStubTypeForBuilderInference()
    }

    override fun TypeConstructorMarker.unwrapStubTypeVariableConstructor(): TypeConstructorMarker {
        return this
    }

    override fun StubTypeMarker.getOriginalTypeVariable(): TypeVariableTypeConstructorMarker {
        require(this is AbstractStubType, this::errorMessage)
        return this.originalTypeVariable as TypeVariableTypeConstructorMarker
    }

    override fun CapturedTypeMarker.lowerType(): CangJieTypeMarker? {
        // TODO: https://youtrack.jetbrains.com/issue/KT-54196 (old captured type here)
        require(this is NewCapturedType, this::errorMessage)
        return this.lowerType
    }

    override fun TypeConstructorMarker.isIntersection(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return this is IntersectionTypeConstructor
    }

    override fun identicalArguments(a: SimpleTypeMarker, b: SimpleTypeMarker): Boolean {
        require(a is SimpleType, a::errorMessage)
        require(b is SimpleType, b::errorMessage)
        return a.arguments === b.arguments
    }

    override fun CangJieTypeMarker.asSimpleType(): SimpleTypeMarker? {
        require(this is CangJieType, this::errorMessage)
        return this.unwrap() as? SimpleType
    }

    override fun CangJieTypeMarker.asFlexibleType(): FlexibleTypeMarker? {
        require(this is CangJieType, this::errorMessage)
        return this.unwrap() as? FlexibleType
    }

    override fun FlexibleTypeMarker.asDynamicType(): DynamicTypeMarker? {
        require(this is FlexibleType, this::errorMessage)
        return this as? DynamicType
    }

    override fun CangJieTypeMarker.isRawType(): Boolean {
        require(this is CangJieType, this::errorMessage)
        return this is RawType
    }

    override fun CangJieTypeMarker.convertToNonRaw(): CangJieTypeMarker {
        error("Is not expected to be called in K1")
    }

    override fun FlexibleTypeMarker.upperBound(): SimpleTypeMarker {
        require(this is FlexibleType, this::errorMessage)
        return this.upperBound
    }

    override fun FlexibleTypeMarker.lowerBound(): SimpleTypeMarker {
        require(this is FlexibleType, this::errorMessage)
        return this.lowerBound
    }

    override fun SimpleTypeMarker.asCapturedType(): CapturedTypeMarker? {
        require(this is SimpleType, this::errorMessage)
        return if (this is SimpleTypeWithEnhancement) origin.asCapturedType() else this as? NewCapturedType
    }

    override fun SimpleTypeMarker.asDefinitelyNotNullType(): DefinitelyNotNullTypeMarker? {
        require(this is SimpleType, this::errorMessage)
        return this as? DefinitelyNotNullType
    }

    @OptIn(ObsoleteTypeKind::class)
    override fun CangJieTypeMarker.isNotNullTypeParameter(): Boolean = this is NotNullTypeParameter

    override fun SimpleTypeMarker.isMarkedNullable(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return this.isMarkedOption
    }

    override fun SimpleTypeMarker.typeConstructor(): TypeConstructorMarker {
        require(this is SimpleType, this::errorMessage)
        return this.constructor
    }

    override fun CapturedTypeMarker.typeConstructor(): CapturedTypeConstructorMarker {
        require(this is NewCapturedType, this::errorMessage)
        return this.constructor
    }

    override fun CapturedTypeConstructorMarker.projection(): TypeArgumentMarker {
        require(this is NewCapturedTypeConstructor, this::errorMessage)
        return this.projection
    }

    override fun CangJieTypeMarker.argumentsCount(): Int {
        require(this is CangJieType, this::errorMessage)
        return this.arguments.size
    }

    override fun CangJieTypeMarker.getArgument(index: Int): TypeArgumentMarker {
        require(this is CangJieType, this::errorMessage)
        return this.arguments[index]
    }

    override fun CangJieTypeMarker.getArguments(): List<TypeArgumentMarker> {
        require(this is CangJieType, this::errorMessage)
        return this.arguments
    }

    override fun TypeArgumentMarker.isStarProjection(): Boolean {
        require(this is TypeProjection, this::errorMessage)
        return this.isStarProjection
    }

    override fun TypeArgumentMarker.getVariance(): TypeVariance {
        require(this is TypeProjection, this::errorMessage)
        return this.projectionKind.convertVariance()
    }

    override fun TypeArgumentMarker.replaceType(newType: CangJieTypeMarker): TypeArgumentMarker {
        require(this is TypeProjection, this::errorMessage)
        require(newType is CangJieType, this::errorMessage)
        return this.replaceType(newType)
    }

    override fun TypeArgumentMarker.getType(): CangJieTypeMarker {
        require(this is TypeProjection, this::errorMessage)
        return this.type.unwrap()
    }


    override fun TypeConstructorMarker.parametersCount(): Int {
        require(this is TypeConstructor, this::errorMessage)
        return this.parameters.size
    }

    override fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker {
        require(this is TypeConstructor, this::errorMessage)
        return this.parameters[index]
    }

    override fun TypeConstructorMarker.getParameters(): List<TypeParameterMarker> {
        require(this is TypeConstructor, this::errorMessage)
        return this.parameters
    }

    override fun TypeConstructorMarker.supertypes(): Collection<CangJieTypeMarker> {
        require(this is TypeConstructor, this::errorMessage)
        return this.supertypes
    }

    override fun TypeConstructorMarker.extendSupertypes(): Collection<CangJieTypeMarker> {
        require(this is TypeConstructor, this::errorMessage)
        return this.getExtendSupertypes(null)

    }

    override fun TypeConstructorMarker.supertypesAndExtend(): Collection<CangJieTypeMarker> {
        return supertypes() + extendSupertypes()
    }

    override fun TypeParameterMarker.getVariance(): TypeVariance {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.variance.convertVariance()
    }

    override fun TypeParameterMarker.upperBoundCount(): Int {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.upperBounds.size
    }

    override fun TypeParameterMarker.getUpperBound(index: Int): CangJieTypeMarker {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.upperBounds[index]
    }

    override fun TypeParameterMarker.getUpperBounds(): List<CangJieTypeMarker> {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.upperBounds
    }

    override fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.typeConstructor
    }

    override fun TypeParameterMarker.hasRecursiveBounds(selfConstructor: TypeConstructorMarker?): Boolean {
        require(this is TypeParameterDescriptor, this::errorMessage)
        require(selfConstructor is TypeConstructor?, this::errorMessage)

        return hasTypeParameterRecursiveBounds(this, selfConstructor)
    }

    override fun areEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
        require(c1 is TypeConstructor, c1::errorMessage)
        require(c2 is TypeConstructor, c2::errorMessage)
        return c1 == c2
    }

    override fun TypeConstructorMarker.isClassTypeConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return declarationDescriptor is ClassDescriptor
    }

    override fun TypeConstructorMarker.isInterface(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return DescriptorUtils.isInterface(declarationDescriptor)
    }

//    override fun TypeConstructorMarker.isFinalClassConstructor(): Boolean {
//        require(this is TypeConstructor, this::errorMessage)
//        val classDescriptor = declarationDescriptor as? ClassDescriptor ?: return false
//        return classDescriptor.isFinalClass
//    }

//    override fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean {
//        require(this is TypeConstructor, this::errorMessage)
//        val classDescriptor = declarationDescriptor as? ClassDescriptor ?: return false
//        return classDescriptor.isFinalClass &&
//                classDescriptor.kind != ClassKind.ENUM_ENTRY &&
//                classDescriptor.kind != ClassKind.ANNOTATION_CLASS
//    }

//    override fun TypeConstructorMarker.isFinalClassOrEnumEntryOrAnnotationClassConstructor(): Boolean {
//        require(this is TypeConstructor, this::errorMessage)
//        val classDescriptor = declarationDescriptor
//        return classDescriptor is ClassDescriptor && classDescriptor.isFinalClass
//    }

    override fun SimpleTypeMarker.asArgumentList(): TypeArgumentListMarker {
        require(this is SimpleType, this::errorMessage)
        return this
    }

    override fun captureFromArguments(type: SimpleTypeMarker, status: CaptureStatus): SimpleTypeMarker? {
        require(type is SimpleType, type::errorMessage)
        return com.huawei.cangjie.types.checker.captureFromArguments(type, status)
    }

    override fun TypeConstructorMarker.isAnyConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return CangJieBuiltIns.isTypeConstructorForGivenClass(this, FqNames.any)
    }

    override fun TypeConstructorMarker.isNothingConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return CangJieBuiltIns.isTypeConstructorForGivenClass(this, FqNames.nothing)
    }

    override fun TypeConstructorMarker.isArrayConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return CangJieBuiltIns.isTypeConstructorForGivenClass(this, FqNames.array)
    }

    override fun CangJieTypeMarker.asTypeArgument(): TypeArgumentMarker {
        require(this is CangJieType, this::errorMessage)
        return this.asTypeProjection()
    }

    override fun TypeConstructorMarker.isUnitTypeConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return CangJieBuiltIns.isTypeConstructorForGivenClass(this, FqNames.unit)
    }

    /**
     *
     * SingleClassifierType is one of the following types:
     *  - classType
     *  - type for type parameter
     *  - captured type
     *
     * Such types can contain error types in our arguments, but type constructor isn't errorTypeConstructor
     */
    override fun SimpleTypeMarker.isSingleClassifierType(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return !isError &&
                constructor.declarationDescriptor !is TypeAliasDescriptor &&
                (constructor.declarationDescriptor != null || this is CapturedType || this is NewCapturedType || this is DefinitelyNotNullType || constructor is IntegerLiteralTypeConstructor || isSingleClassifierTypeWithEnhancement())
    }

    private fun SimpleTypeMarker.isSingleClassifierTypeWithEnhancement() =
        this is SimpleTypeWithEnhancement && origin.isSingleClassifierType()

    override fun CangJieTypeMarker.contains(predicate: (CangJieTypeMarker) -> Boolean): Boolean {
        require(this is CangJieType, this::errorMessage)
        return containsInternal(this, predicate)
    }

    //
    override fun SimpleTypeMarker.typeDepth(): Int {
        require(this is SimpleType, this::errorMessage)
        if (this is TypeUtils.SpecialType) return 0

        val maxInArguments = arguments.maxOfOrNull {
            if (it.isStarProjection) 1 else it.type.unwrap().typeDepth()
        } ?: 0

        return maxInArguments + 1
    }

    override fun intersectTypes(types: List<CangJieTypeMarker>): CangJieTypeMarker {
        @Suppress("UNCHECKED_CAST")
        return com.huawei.cangjie.types.checker.intersectTypes(types as List<UnwrappedType>)
    }

    override fun intersectTypes(types: List<SimpleTypeMarker>): SimpleTypeMarker {
        @Suppress("UNCHECKED_CAST")
        return com.huawei.cangjie.types.checker.intersectTypes(types as List<SimpleType>)
    }

    override fun Collection<CangJieTypeMarker>.singleBestRepresentative(): CangJieTypeMarker? {
        @Suppress("UNCHECKED_CAST")
        return singleBestRepresentative(this as Collection<CangJieType>)
    }

    override fun CangJieTypeMarker.isUnit(): Boolean {
        require(this is UnwrappedType, this::errorMessage)
        return CangJieBuiltIns.isUnit(this)
    }

//    override fun CangJieTypeMarker.isBuiltinFunctionTypeOrSubtype(): Boolean {
//        require(this is UnwrappedType, this::errorMessage)
//        return isBuiltinFunctionalTypeOrSubtype
//    }

    override fun createFlexibleType(lowerBound: SimpleTypeMarker, upperBound: SimpleTypeMarker): CangJieTypeMarker {
        require(lowerBound is SimpleType, this::errorMessage)
        require(upperBound is SimpleType, this::errorMessage)
        return CangJieTypeFactory.flexibleType(lowerBound, upperBound)
    }

    override fun CangJieTypeMarker.withNullability(nullable: Boolean): CangJieTypeMarker {
        return when (this) {
            is SimpleTypeMarker -> this.withNullability(nullable)
            is FlexibleTypeMarker -> createFlexibleType(
                lowerBound().withNullability(nullable),
                upperBound().withNullability(nullable)
            )

            else -> error("sealed")
        }
    }


    override fun newTypeCheckerState(
        errorTypesEqualToAnything: Boolean,
        stubTypesEqualToAnything: Boolean
    ): TypeCheckerState {
        return createClassicTypeCheckerState(
            errorTypesEqualToAnything,
            stubTypesEqualToAnything,
            typeSystemContext = this
        )
    }
//
//    override fun nullableNothingType(): SimpleTypeMarker {
//        return builtIns.nullableNothingType
//    }

//    override fun nullableAnyType(): SimpleTypeMarker {
//        return builtIns.nullableAnyType
//    }

    override fun nothingType(): SimpleTypeMarker {
        return builtIns.nothingType
    }

    override fun anyType(): SimpleTypeMarker {
        return builtIns.anyType
    }


    override fun CangJieTypeMarker.makeDefinitelyNotNullOrNotNull(): CangJieTypeMarker {
        require(this is UnwrappedType, this::errorMessage)
        return makeDefinitelyNotNullOrNotNullInternal(this)
    }


    override fun SimpleTypeMarker.makeSimpleTypeDefinitelyNotNullOrNotNull(): SimpleTypeMarker {
        require(this is SimpleType, this::errorMessage)
        return makeSimpleTypeDefinitelyNotNullOrNotNullInternal(this)
    }

    override fun CangJieTypeMarker.removeAnnotations(): CangJieTypeMarker {
        require(this is UnwrappedType, this::errorMessage)
        return this.replaceAnnotations(Annotations.EMPTY)
    }

//    override fun CangJieTypeMarker.removeExactAnnotation(): CangJieTypeMarker {
//        require(this is UnwrappedType, this::errorMessage)
//        val annotationsWithoutExact = this.annotations.filterNot(AnnotationDescriptor::isExactAnnotation)
//        return this.replaceAnnotations(Annotations.create(annotationsWithoutExact))
//    }

//    override fun CangJieTypeMarker.hasExactAnnotation(): Boolean {
//        require(this is UnwrappedType, this::errorMessage)
//        return hasExactInternal(this)
//    }
//
//    override fun CangJieTypeMarker.hasNoInferAnnotation(): Boolean {
//        require(this is UnwrappedType, this::errorMessage)
//        return hasNoInferInternal(this)
//    }

    override fun TypeVariableMarker.freshTypeConstructor(): TypeConstructorMarker {
        errorSupportedOnlyInTypeInference()
    }

    override fun CapturedTypeMarker.typeConstructorProjection(): TypeArgumentMarker {
        return when (this) {
            is NewCapturedType -> this.constructor.projection
            is CapturedType -> this.typeProjection
            else -> error("Unsupported captured type")
        }
    }

    override fun CapturedTypeMarker.withNotNullProjection(): CangJieTypeMarker {
        require(this is NewCapturedType, this::errorMessage)

        return NewCapturedType(
            captureStatus,
            constructor,
            lowerType,
            attributes,
            isMarkedOption,
            isProjectionNotNull = true
        )
    }

    override fun CapturedTypeMarker.isProjectionNotNull(): Boolean {
        require(this is NewCapturedType, this::errorMessage)
        return this.isProjectionNotNull
    }

    override fun CapturedTypeMarker.typeParameter(): TypeParameterMarker? {
        require(this is NewCapturedType, this::errorMessage)
        return this.constructor.typeParameter
    }

    override fun CapturedTypeMarker.captureStatus(): CaptureStatus {
        require(this is NewCapturedType, this::errorMessage)
        return this.captureStatus
    }

    override fun CapturedTypeMarker.isOldCapturedType(): Boolean = this is CapturedType

    override fun CapturedTypeMarker.hasRawSuperType(): Boolean {
        error("Is not expected to be called in K1")
    }

    override fun CangJieTypeMarker.isNullableType(): Boolean {
        require(this is CangJieType, this::errorMessage)
        return TypeUtils.isNullableType(this)
    }

    override fun createSimpleType(
        constructor: TypeConstructorMarker,
        arguments: List<TypeArgumentMarker>,
        nullable: Boolean,
        isExtensionFunction: Boolean,
        attributes: List<AnnotationMarker>?
    ): SimpleTypeMarker {
        require(constructor is TypeConstructor, constructor::errorMessage)

        val ourAnnotations = attributes?.firstIsInstanceOrNull<AnnotationsTypeAttribute>()?.annotations?.toList()

        fun createExtensionFunctionAnnotation() =
            BuiltInAnnotationDescriptor(builtIns, FqNames.extensionFunctionType, emptyMap())

        val resultingAnnotations = when {
            ourAnnotations.isNullOrEmpty() && isExtensionFunction -> Annotations.create(
                listOf(
                    createExtensionFunctionAnnotation()
                )
            )

            !ourAnnotations.isNullOrEmpty() && !isExtensionFunction -> Annotations.create(ourAnnotations.filter { it.fqName != FqNames.extensionFunctionType })
            !ourAnnotations.isNullOrEmpty() && isExtensionFunction -> Annotations.create(ourAnnotations + createExtensionFunctionAnnotation())
            else -> Annotations.EMPTY
        }

        @Suppress("UNCHECKED_CAST")
        return CangJieTypeFactory.simpleType(
            DefaultTypeAttributeTranslator.toAttributes(resultingAnnotations),
            constructor,
            arguments as List<TypeProjection>,
            nullable
        )
    }

    override fun createTypeArgument(type: CangJieTypeMarker, variance: TypeVariance): TypeArgumentMarker {
        require(type is CangJieType, type::errorMessage)
        return TypeProjectionImpl(variance.convertVariance(), type)
    }

    override fun createStarProjection(typeParameter: TypeParameterMarker): TypeArgumentMarker {
        require(typeParameter is TypeParameterDescriptor, typeParameter::errorMessage)
        return StarProjectionImpl(typeParameter)
    }

    override fun CangJieTypeMarker.canHaveUndefinedNullability(): Boolean {
        require(this is UnwrappedType, this::errorMessage)
        return constructor is NewTypeVariableConstructor ||
                constructor.declarationDescriptor is TypeParameterDescriptor ||
                this is NewCapturedType
    }

    override fun SimpleTypeMarker.isExtensionFunction(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return this.hasAnnotation(FqNames.extensionFunctionType)
    }

    override fun SimpleTypeMarker.replaceArguments(newArguments: List<TypeArgumentMarker>): SimpleTypeMarker {
        require(this is SimpleType, this::errorMessage)
        @Suppress("UNCHECKED_CAST")
        return this.replace(newArguments as List<TypeProjection>)
    }

    override fun SimpleTypeMarker.replaceArguments(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): SimpleTypeMarker {
        require(this is SimpleType, this::errorMessage)
        return this.replaceArgumentsByExistingArgumentsWith(replacement)
    }

    override fun DefinitelyNotNullTypeMarker.original(): SimpleTypeMarker {
        require(this is DefinitelyNotNullType, this::errorMessage)
        return this.original
    }

    override fun createCapturedType(
        constructorProjection: TypeArgumentMarker,
        constructorSupertypes: List<CangJieTypeMarker>,
        lowerType: CangJieTypeMarker?,
        captureStatus: CaptureStatus
    ): CapturedTypeMarker {
        errorSupportedOnlyInTypeInference()
    }

    override fun typeSubstitutorByTypeConstructor(map: Map<TypeConstructorMarker, CangJieTypeMarker>): TypeSubstitutorMarker {
        errorSupportedOnlyInTypeInference()
    }

    override fun createEmptySubstitutor(): TypeSubstitutorMarker {
        errorSupportedOnlyInTypeInference()
    }

    @COnly
    override fun createSubstitutionFromSubtypingStubTypesToTypeVariables(): TypeSubstitutorMarker {
        error("Only for K2")
    }

    override fun TypeSubstitutorMarker.safeSubstitute(type: CangJieTypeMarker): CangJieTypeMarker {
        require(type is UnwrappedType, type::errorMessage)
        require(this is TypeSubstitutor, this::errorMessage)
        return safeSubstitute(type, Variance.INVARIANT)
    }

    override fun TypeVariableMarker.defaultType(): SimpleTypeMarker {
        errorSupportedOnlyInTypeInference()
    }

    override fun createStubTypeForBuilderInference(typeVariable: TypeVariableMarker): StubTypeMarker {
        errorSupportedOnlyInTypeInference()
    }

    override fun createStubTypeForTypeVariablesInSubtyping(typeVariable: TypeVariableMarker): StubTypeMarker {
        errorSupportedOnlyInTypeInference()
    }

    override fun CangJieTypeMarker.isSpecial(): Boolean {
        require(this is CangJieType)
        return this is TypeUtils.SpecialType
    }

    override fun TypeConstructorMarker.isTypeVariable(): Boolean {
        errorSupportedOnlyInTypeInference()
    }

    override fun TypeVariableTypeConstructorMarker.isContainedInInvariantOrContravariantPositions(): Boolean {
        errorSupportedOnlyInTypeInference()
    }

    override fun CangJieTypeMarker.isSignedOrUnsignedNumberType(): Boolean {
        require(this is CangJieType)
        return classicIsSignedOrUnsignedNumberType() || constructor is IntegerLiteralTypeConstructor
    }

    override fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<SimpleTypeMarker>): SimpleTypeMarker? {
        @Suppress("UNCHECKED_CAST")
        explicitSupertypes as List<SimpleType>
        return IntegerLiteralTypeConstructor.findCommonSuperType(explicitSupertypes)
    }

    override fun unionTypeAttributes(types: List<CangJieTypeMarker>): List<AnnotationMarker> {
        @Suppress("UNCHECKED_CAST")
        return (types as List<CangJieType>).map {
            it.unwrap().attributes

        }.reduce { x, y ->
            x.union(y)
        }.toList()


    }

    override fun CangJieTypeMarker.replaceCustomAttributes(newAttributes: List<AnnotationMarker>): CangJieTypeMarker {
        require(this is CangJieType)
        @Suppress("UNCHECKED_CAST")
        val attributes =
            (newAttributes as List<TypeAttribute<*>>).filterNot { it is AnnotationsTypeAttribute }.toMutableList()
        attributes.addIfNotNull(this.attributes.annotationsAttribute)
        return this.unwrap().replaceAttributes(
            TypeAttributes.create(attributes)
        )
    }

    override fun TypeConstructorMarker.isError(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return ErrorUtils.isError(declarationDescriptor)
    }

    override fun TypeConstructorMarker.getApproximatedIntegerLiteralType(): CangJieTypeMarker {
        require(this is IntegerLiteralTypeConstructor, this::errorMessage)
        return this.getApproximatedType().unwrap()
    }

    override fun SimpleTypeMarker.isPrimitiveType(): Boolean {
        require(this is CangJieType, this::errorMessage)
        return CangJieBuiltIns.isPrimitiveType(this)
    }

//    override fun CangJieTypeMarker.getAttributes(): List<AnnotationMarker> {
//        require(this is CangJieType, this::errorMessage)
//        return this.attributes.toList()
//    }

//    override fun CangJieTypeMarker.hasCustomAttributes(): Boolean {
//        require(this is CangJieType, this::errorMessage)
//        return !this.attributes.isEmpty() && this.getCustomAttributes().size > 0
//    }
//
//    override fun CangJieTypeMarker.getCustomAttributes(): List<AnnotationMarker> {
//        require(this is CangJieType, this::errorMessage)
//        return this.attributes.filterNot { it is AnnotationsTypeAttribute }
//    }

//    override fun captureFromExpression(type: CangJieTypeMarker): CangJieTypeMarker? {
//        return captureFromExpressionInternal(type as UnwrappedType)
//    }

    override fun createErrorType(debugName: String, delegatedType: SimpleTypeMarker?): SimpleTypeMarker {
        return ErrorUtils.createErrorType(ErrorTypeKind.RESOLUTION_ERROR_TYPE, debugName)
    }

    override fun createUninferredType(constructor: TypeConstructorMarker): CangJieTypeMarker {
        return ErrorUtils.createErrorType(
            ErrorTypeKind.UNINFERRED_TYPE_VARIABLE,
            constructor as TypeConstructor,
            constructor.toString()
        )
    }

    override fun TypeConstructorMarker.isCapturedTypeConstructor(): Boolean {
        return this is NewCapturedTypeConstructor
    }

//    override fun CangJieTypeMarker.eraseContainingTypeParameters(): CangJieTypeMarker {
//        val eraser = TypeParameterUpperBoundEraser(
//            ErasureProjectionComputer(),
//            TypeParameterErasureOptions(leaveNonTypeParameterTypes = true, intersectUpperBounds = true)
//        )
//        val typeParameters = this.extractTypeParameters()
//            .map { it as TypeParameterDescriptor }
//            .associateWith {
//                TypeProjectionImpl(
//                    Variance.OUT_VARIANCE,
//                    eraser.getErasedUpperBound(it, ErasureTypeAttributes(TypeUsage.COMMON))
//                )
//            }
//        return TypeConstructorSubstitution.createByParametersMap(typeParameters).buildSubstitutor().safeSubstitute(this)
//    }

    override fun TypeConstructorMarker.isTypeParameterTypeConstructor(): Boolean {
        return this is ClassifierBasedTypeConstructor && this.declarationDescriptor is AbstractTypeParameterDescriptor
    }

    //    override fun arrayType(componentType: CangJieTypeMarker): SimpleTypeMarker {
//        require(componentType is CangJieType, this::errorMessage)
//        return builtIns.getArrayType(Variance.INVARIANT, componentType)
//    }
//
//    override fun CangJieTypeMarker.isArrayOrNullableArray(): Boolean {
//        require(this is CangJieType, this::errorMessage)
//        return CangJieBuiltIns.isArray(this)
//    }
//
    override fun CangJieTypeMarker.hasAnnotation(fqName: FqName): Boolean {
        require(this is CangJieType, this::errorMessage)
        return annotations.hasAnnotation(fqName)
    }
//
//    override fun CangJieTypeMarker.getAnnotationFirstArgumentValue(fqName: FqName): Any? {
//        require(this is CangJieType, this::errorMessage)
//        return annotations.findAnnotation(fqName)?.allValueArguments?.values?.firstOrNull()?.value
//    }

    override fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker? {
        require(this is TypeConstructor, this::errorMessage)
        return declarationDescriptor as? TypeParameterDescriptor
    }

//    override fun TypeConstructorMarker.isInlineClass(): Boolean {
//        require(this is TypeConstructor, this::errorMessage)
//        return (declarationDescriptor as? ClassDescriptor)?.valueClassRepresentation is InlineClassRepresentation
//    }
//
//    override fun TypeConstructorMarker.isMultiFieldValueClass(): Boolean {
//        require(this is TypeConstructor, this::errorMessage)
//        return (declarationDescriptor as? ClassDescriptor)?.valueClassRepresentation is MultiFieldValueClassRepresentation
//    }
//
//    override fun TypeConstructorMarker.getValueClassProperties(): List<Pair<Name, SimpleTypeMarker>>? {
//        require(this is TypeConstructor, this::errorMessage)
//        return (declarationDescriptor as? ClassDescriptor)?.valueClassRepresentation?.underlyingPropertyNamesToTypes
//    }
//
//    override fun TypeConstructorMarker.isInnerClass(): Boolean {
//        require(this is TypeConstructor, this::errorMessage)
//        return (declarationDescriptor as? ClassDescriptor)?.isInner == true
//    }
//
//    override fun TypeParameterMarker.getRepresentativeUpperBound(): CangJieTypeMarker {
//        require(this is TypeParameterDescriptor, this::errorMessage)
//        return representativeUpperBound
//    }
//
//    override fun CangJieTypeMarker.getUnsubstitutedUnderlyingType(): CangJieTypeMarker? {
//        require(this is CangJieType, this::errorMessage)
//        return unsubstitutedUnderlyingType()
//    }
//
//    override fun CangJieTypeMarker.getSubstitutedUnderlyingType(): CangJieTypeMarker? {
//        require(this is CangJieType, this::errorMessage)
//        return substitutedUnderlyingType()
//    }
//
//    override fun TypeConstructorMarker.getPrimitiveType(): PrimitiveType? {
//        require(this is TypeConstructor, this::errorMessage)
//        return CangJieBuiltIns.getPrimitiveType(declarationDescriptor as ClassDescriptor)
//    }
//
//    override fun TypeConstructorMarker.getPrimitiveArrayType(): PrimitiveType? {
//        require(this is TypeConstructor, this::errorMessage)
//        return CangJieBuiltIns.getPrimitiveArrayType(declarationDescriptor as ClassDescriptor)
//    }
//
//    override fun TypeConstructorMarker.isUnderCangJiePackage(): Boolean {
//        require(this is TypeConstructor, this::errorMessage)
//        return declarationDescriptor?.let(CangJieBuiltIns::isUnderCangJiePackage) == true
//    }
//
//    override fun TypeConstructorMarker.getClassFqNameUnsafe(): FqNameUnsafe {
//        require(this is TypeConstructor, this::errorMessage)
//        return (declarationDescriptor as ClassDescriptor).fqNameUnsafe
//    }
//
//    override fun TypeParameterMarker.getName(): Name {
//        require(this is TypeParameterDescriptor, this::errorMessage)
//        return name
//    }
//
//    override fun TypeParameterMarker.isReified(): Boolean {
//        require(this is TypeParameterDescriptor, this::errorMessage)
//        return isReified
//    }
//
//    override fun CangJieTypeMarker.isInterfaceOrAnnotationClass(): Boolean {
//        require(this is CangJieType, this::errorMessage)
//        val descriptor = constructor.declarationDescriptor
//        return descriptor is ClassDescriptor && (descriptor.kind == ClassKind.INTERFACE || descriptor.kind == ClassKind.ANNOTATION_CLASS)
//    }

    override fun createTypeWithAlternativeForIntersectionResult(
        firstCandidate: CangJieTypeMarker,
        secondCandidate: CangJieTypeMarker,
    ): CangJieTypeMarker {
        require(firstCandidate is CangJieType, this::errorMessage)
        require(secondCandidate is CangJieType, this::errorMessage)

        (firstCandidate.constructor as? IntersectionTypeConstructor)?.let { intersectionConstructor ->
            val intersectionTypeWithAlternative = intersectionConstructor.setAlternative(secondCandidate).createType()
            return if (firstCandidate.isMarkedOption) intersectionTypeWithAlternative.makeNullableAsSpecified(true)
            else intersectionTypeWithAlternative

        } ?: error("Expected intersection type, found $firstCandidate")
    }

//    override fun CangJieTypeMarker.isFunctionOrKFunctionWithAnySuspendability(): Boolean {
//        require(this is CangJieType, this::errorMessage)
//        return this.isFunctionOrKFunctionTypeWithAnySuspendability
//    }

    override fun CangJieTypeMarker.isExtensionFunctionType(): Boolean {
        require(this is CangJieType, this::errorMessage)
        return this.isBuiltinExtensionFunctionalType
    }

//    override fun CangJieTypeMarker.extractArgumentsForFunctionTypeOrSubtype(): List<CangJieTypeMarker> {
//        require(this is CangJieType, this::errorMessage)
//        return this.getPureArgumentsForFunctionalTypeOrSubtype()
//    }

//    override fun CangJieTypeMarker.getFunctionTypeFromSupertypes(): CangJieTypeMarker {
//        require(this is CangJieType)
//        return this.extractFunctionalTypeFromSupertypes()
//    }

    override fun CangJieTypeMarker.functionTypeKind(): FunctionTypeKind? {
        require(this is CangJieType)
        return this.functionTypeKind
    }

    override fun getNonReflectFunctionTypeConstructor(
        parametersNumber: Int,
        kind: FunctionTypeKind
    ): TypeConstructorMarker {
        return getFunctionDescriptor(
            builtIns,
            parametersNumber,
//            isSuspendFunction = kind.nonReflectKind() == FunctionTypeKind.SuspendFunction
        ).typeConstructor
    }

//    override fun getReflectFunctionTypeConstructor(
//        parametersNumber: Int,
//        kind: FunctionTypeKind
//    ): TypeConstructorMarker {
//        return getCFunctionDescriptor(
//            builtIns,
//            parametersNumber,
//            isSuspendFunction = kind.reflectKind() == FunctionTypeKind.CSuspendFunction
//        ).typeConstructor
//    }

    override fun createSubstitutorForSuperTypes(baseType: CangJieTypeMarker): TypeSubstitutorMarker? {
        require(baseType is CangJieType, baseType::errorMessage)
        return (baseType.memberScope as? SubstitutingScope)?.substitutor
    }

    override fun useRefinedBoundsForTypeVariableInFlexiblePosition(): Boolean = false

    override fun substitutionSupertypePolicy(type: SimpleTypeMarker): TypeCheckerState.SupertypesPolicy {
        require(type is SimpleType, type::errorMessage)
        val substitutor = TypeConstructorSubstitution.create(type).buildSubstitutor()

        return object : TypeCheckerState.SupertypesPolicy.DoCustomTransform() {
            override fun transformType(state: TypeCheckerState, type: CangJieTypeMarker): SimpleTypeMarker {
                return substitutor.safeSubstitute(
                    type.lowerBoundIfFlexible() as CangJieType,
                    Variance.INVARIANT
                ).asSimpleType()!!
            }
        }
    }

    override fun CangJieTypeMarker.isTypeVariableType(): Boolean {
        return this is UnwrappedType && constructor is NewTypeVariableConstructor
    }


}

private fun makeSimpleTypeDefinitelyNotNullOrNotNullInternal(type: SimpleType): SimpleType {
    return type.makeSimpleTypeDefinitelyNotNullOrNotNull()
}

private fun makeDefinitelyNotNullOrNotNullInternal(type: UnwrappedType): UnwrappedType {
    return type.makeDefinitelyNotNullOrNotNull()
}

//private fun hasExactInternal(type: UnwrappedType): Boolean {
//    return type.hasExactAnnotation()
//}

private fun singleBestRepresentative(collection: Collection<CangJieType>) = collection.singleBestRepresentative()

private fun containsInternal(type: CangJieType, predicate: (CangJieTypeMarker) -> Boolean): Boolean =
    type.contains(predicate)

fun TypeVariance.convertVariance(): Variance {
    return when (this) {
        TypeVariance.INV -> Variance.INVARIANT
        TypeVariance.IN -> Variance.IN_VARIANCE
        TypeVariance.OUT -> Variance.OUT_VARIANCE
    }
}

private fun captureFromExpressionInternal(type: UnwrappedType) = captureFromExpression(type)
