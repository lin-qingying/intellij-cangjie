package com.huawei.cangjie.types.util

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.psi.CjBlockExpression
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import com.huawei.cangjie.resolve.constants.IntegerValueTypeConstructor
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.checker.CangJieTypeChecker
import com.huawei.cangjie.types.checker.CangJieTypeChecker.DEFAULT
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.checker.NewTypeVariableConstructor
import com.huawei.cangjie.types.error.ErrorScopeKind
import com.huawei.cangjie.types.error.ErrorType
import com.huawei.cangjie.types.error.ErrorTypeKind
import com.huawei.cangjie.types.model.TypeArgumentMarker
import com.huawei.cangjie.types.model.TypeVariableTypeConstructorMarker
import com.huawei.cangjie.utils.SmartSet
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun CangJieType.replaceAnnotations(newAnnotations: Annotations): CangJieType {
    if (annotations.isEmpty() && newAnnotations.isEmpty()) return this
    return unwrap().replaceAttributes(attributes.replaceAnnotations(newAnnotations))
}

fun CangJieType.unwrapEnhancement(): CangJieType = getEnhancement() ?: this
fun CangJieType.supertypes(): Collection<CangJieType> = TypeUtils.getAllSupertypes(this)

@JvmOverloads // For binary compatibility
fun CangJieType.approximateFlexibleTypes(
    preferNotNull: Boolean = false,
    preferStarForRaw: Boolean = false,
    preferUpperBoundsForCollections: Boolean = false,
): CangJieType {
    if (isDynamic()) return this
    if (isDefinitelyNotNullType) return this
    return unwrapEnhancement().approximateNonDynamicFlexibleTypes(
        preferNotNull,
        preferStarForRaw,
        preferUpperBoundsForCollections
    )
}

enum class TypeNullability {
    NOT_NULL,
    NULLABLE,
    FLEXIBLE
}

fun CangJieType.nullability(): TypeNullability {
    return when {
        isNullabilityFlexible() -> TypeNullability.FLEXIBLE
        TypeUtils.isNullableType(this) -> TypeNullability.NULLABLE
        else -> TypeNullability.NOT_NULL
    }
}

private fun CangJieType.approximateNonDynamicFlexibleTypes(
    preferNotNull: Boolean = false,
    preferStarForRaw: Boolean = false,
    preferUpperBoundsForCollections: Boolean = false,
): SimpleType {
    if (this is ErrorType) return this

    if (isFlexible()) {
        val flexible = asFlexibleType()
        val lowerBound = flexible.lowerBound
        val upperBound = flexible.upperBound
        val lowerClass = lowerBound.constructor.declarationDescriptor as? ClassDescriptor?
        val isCollection = lowerClass != null
        // (Mutable)Collection<T>! -> MutableCollection<T>?
        // Foo<(Mutable)Collection<T>!>! -> Foo<Collection<T>>?
        // Foo! -> Foo?
        // Foo<Bar!>! -> Foo<Bar>?
        var approximation =
            if (isCollection) {
                // (Mutable)Collection<T>!
                val bound = if (preferUpperBoundsForCollections) upperBound else lowerBound
                if (lowerBound.isMarkedNullable != upperBound.isMarkedNullable)
                    bound.makeNullableAsSpecified(!preferNotNull)
                else
                    bound
            } else {
                if (this is RawType && preferStarForRaw)
                    upperBound.makeNullableAsSpecified(!preferNotNull)
                else
                    if (preferNotNull) lowerBound else upperBound
            }

        approximation = approximation.approximateNonDynamicFlexibleTypes()

        approximation =
            if (nullability() == TypeNullability.NOT_NULL) approximation.makeNullableAsSpecified(false) else approximation

        if (approximation.isMarkedNullable && !lowerBound
                .isMarkedNullable && TypeUtils.isTypeParameter(approximation) && TypeUtils.hasNullableSuperType(
                approximation
            )
        ) {
            approximation = approximation.makeNullableAsSpecified(false)
        }

        return approximation
    }

    (unwrap() as? AbbreviatedType)?.let {
        return AbbreviatedType(it.expandedType, it.abbreviation.approximateNonDynamicFlexibleTypes(preferNotNull))
    }
    return CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope(
        annotations.toDefaultAttributes(),
        constructor,
        arguments.map { it.substitute { type -> type.approximateFlexibleTypes(preferNotNull = true) } },
        isMarkedNullable,
        ErrorUtils.createErrorScope(ErrorScopeKind.UNSUPPORTED_TYPE_SCOPE, true, constructor.toString())
    )
}

fun CangJieType.isTypeParameter(): Boolean = TypeUtils.isTypeParameter(this)

fun TypeProjection.substitute(doSubstitute: (CangJieType) -> CangJieType): TypeProjection {
    return if (isStarProjection)
        this
    else TypeProjectionImpl(projectionKind, doSubstitute(type))
}

fun CangJieType.makeNullable() = TypeUtils.makeNullable(this)
fun CangJieType.makeNotNullable() = TypeUtils.makeNotNullable(this)

fun CangJieType.isInterface(): Boolean =
    (constructor.declarationDescriptor as? ClassDescriptor)?.kind == ClassKind.INTERFACE

fun CangJieType.isEnum(): Boolean = (constructor.declarationDescriptor as? ClassDescriptor)?.kind == ClassKind.ENUM

//fun CangJieType.containsTypeProjectionsInTopLevelArguments(): Boolean {
//    if (isError) return false
//    val possiblyInnerType = buildPossiblyInnerType() ?: return false
//    return possiblyInnerType.arguments.any { it.isStarProjection || it.projectionKind != Variance.INVARIANT }
//}

fun UnwrappedType.unCapture(): UnwrappedType = when (this) {

    is AbbreviatedType -> unCapture()
    is SimpleType -> unCapture()
    is FlexibleType -> unCapture()

}

fun CangJieType.expandIntersectionTypeIfNecessary(): Collection<CangJieType> {
    if (constructor !is IntersectionTypeConstructor) return listOf(this)
    val types = constructor.supertypes
    return if (isMarkedNullable) {
        types.map { it.makeNullable() }
    } else {
        types
    }
}


//fun CangJieType?.shouldBeUpdated() =
//    this == null || contains { it is StubTypeForBuilderInference || it.constructor is TypeVariableTypeConstructorMarker || it.isError }
fun CangJieType?.shouldBeUpdated() =
    this == null || contains { it is StubTypeForBuilderInference || it.constructor is TypeVariableTypeConstructorMarker || it.isError }

val CangJieType.builtIns: CangJieBuiltIns
    get() = constructor.builtIns

fun CangJieType.contains(predicate: (UnwrappedType) -> Boolean) = TypeUtils.contains(this, predicate)
fun CangJieType.isSignedOrUnsignedNumberType(): Boolean = isPrimitiveNumberType() /*|| isUnsignedNumberType()*/
fun CangJieType.isStubTypeForBuilderInference(): Boolean =
    this is StubTypeForBuilderInference || isDefNotNullStubType<StubTypeForBuilderInference>()

fun CangJieType.isPrimitiveNumberType(): Boolean = CangJieBuiltIns.isPrimitiveType(this) && !isBoolean()
fun CangJieType.isBoolean(): Boolean = CangJieBuiltIns.isBoolean(this)
//fun CangJieType.isUnsignedNumberType(): Boolean = UnsignedTypes.isUnsignedType(this)

fun CangJieType.asTypeProjection(): TypeProjection = TypeProjectionImpl(this)
fun CangJieType.isStubType() = this is AbstractStubType || isDefNotNullStubType<AbstractStubType>()
private inline fun <reified S : AbstractStubType> CangJieType.isDefNotNullStubType() =
    this is DefinitelyNotNullType && this.original is S

fun CangJieType.isStubTypeForVariableInSubtyping(): Boolean =
    this is StubTypeForTypeVariablesInSubtyping || isDefNotNullStubType<StubTypeForTypeVariablesInSubtyping>()

@JvmOverloads
fun hasTypeParameterRecursiveBounds(
    typeParameter: TypeParameterDescriptor,
    selfConstructor: TypeConstructor? = null,
    visitedTypeParameters: Set<TypeParameterDescriptor>? = null
): Boolean =
    typeParameter.upperBounds.any { upperBound ->
        upperBound.containsSelfTypeParameter(typeParameter.defaultType.constructor, visitedTypeParameters)
                && (selfConstructor == null || upperBound.constructor == selfConstructor)
    }

private fun CangJieType.containsSelfTypeParameter(
    baseConstructor: TypeConstructor,
    visitedTypeParameters: Set<TypeParameterDescriptor>?
): Boolean {
    if (this.constructor == baseConstructor) return true

    val typeParameters =
        (constructor.declarationDescriptor as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters
    return arguments.withIndex().any { (i, argument) ->
        val typeParameter = typeParameters?.getOrNull(i)
        val isTypeParameterVisited =
            typeParameter != null && visitedTypeParameters != null && typeParameter in visitedTypeParameters
        if (isTypeParameterVisited || argument.isStarProjection) return@any false
        argument.type.containsSelfTypeParameter(baseConstructor, visitedTypeParameters)
    }
}

inline fun SimpleType.replaceArgumentsByExistingArgumentsWith(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): SimpleType {
    if (arguments.isEmpty()) return this
    return replace(newArguments = arguments.map { replacement(it) as TypeProjection })
}


fun createBasicType(
    builtIns: CangJieBuiltIns,
    name: String
): BasicType {
    val classDescriptor = builtIns.getBuiltInBasicTypeByName(name)


    return CangJieTypeFactory.basicType(classDescriptor)
}

fun CangJieType.containsTypeAliasParameters(): Boolean =
    contains {
        it.constructor.declarationDescriptor?.isTypeAliasParameter() ?: false
    }

fun ClassifierDescriptor.isTypeAliasParameter(): Boolean =
    this is TypeParameterDescriptor && containingDeclaration is TypeAliasDescriptor

fun CangJieType.containsTypeAliases(): Boolean =
    contains {
        it.constructor.declarationDescriptor is TypeAliasDescriptor
    }

@OptIn(ExperimentalContracts::class)
fun isUnresolvedType(type: CangJieType): Boolean {
    contract {
        returns(true) implies (type is ErrorType)
    }
    return type is ErrorType && type.kind.isUnresolved
}

fun CangJieTypeChecker.equalTypesOrNulls(type1: CangJieType?, type2: CangJieType?): Boolean {
    if (type1 === type2) return true
    if (type1 == null || type2 == null) return false
    return equalTypes(type1, type2)
}

object TypeUtils {

    val DONT_CARE: SimpleType = ErrorUtils.createErrorType(ErrorTypeKind.DONT_CARE)

    /**
     * Differs from `isNullableType` only by treating type parameters: acceptsNullable(T) <=> T has nullable lower bound
     * Semantics should be the same as `isSubtype(Nothing?, T)`
     * @return true if `null` can be assigned to storage of this type
     */
    fun acceptsNullable(type: CangJieType): Boolean {
        if (type.isMarkedNullable) {
            return true
        }
        if (type.isFlexible() && acceptsNullable(type.asFlexibleType().upperBound)) {
            return true
        }
        return false
    }
    fun hasNullableSuperType(type:CangJieType): Boolean {
        if (type.constructor.getDeclarationDescriptor() is  ClassDescriptor) {
            // A class/trait cannot have a nullable supertype
            return false
        }

        for (supertype in  getImmediateSupertypes(type)) {
            if ( isNullableType(supertype)) return true
        }

        return false
    }
    fun isTypeParameter(type: CangJieType): Boolean {
        return getTypeParameterDescriptorOrNull(type) != null || type.constructor is NewTypeVariableConstructor
    }

    fun getTypeParameterDescriptorOrNull(type: CangJieType): TypeParameterDescriptor? {
        if (type.constructor.getDeclarationDescriptor() is TypeParameterDescriptor) {
            return type.constructor.getDeclarationDescriptor() as TypeParameterDescriptor
        }
        return null
    }

    fun createSubstitutedSupertype(
        subType: CangJieType,
        superType: CangJieType,
        substitutor: TypeSubstitutor
    ): CangJieType? {
        val substitutedType: CangJieType? = substitutor.substitute(superType, Variance.INVARIANT)
        if (substitutedType != null) {
            return makeNullableIfNeeded(substitutedType, subType.isMarkedNullable)
        }
        return null
    }

    private fun lowerThanBound(
        typeChecker: CangJieTypeChecker,
        argument: CangJieType,
        parameterDescriptor: TypeParameterDescriptor
    ): Boolean {
        for (bound in parameterDescriptor.getUpperBounds()) {
            if (typeChecker.isSubtypeOf(argument, bound)) {
                if (argument.constructor != bound.constructor) {
                    return true
                }
            }
        }
        return false
    }

    @JvmStatic
    fun canHaveSubtypes(
        typeChecker: CangJieTypeChecker,
        type: CangJieType
    ): Boolean {
        if (type.isMarkedNullable) {
            return true
        }
//        if (!type.constructor.isFinal()) {
//            return true
//        }

        val parameters: List<TypeParameterDescriptor> =
            type.constructor.getParameters()
        val arguments: List<TypeProjection> = type.arguments
        var i = 0
        val parametersSize = parameters.size
        while (i < parametersSize) {
            val parameterDescriptor: TypeParameterDescriptor = parameters[i]
            val typeProjection: TypeProjection = arguments[i]
            if (typeProjection.isStarProjection()) return true

            val projectionKind: Variance = typeProjection.getProjectionKind()
            val argument: CangJieType = typeProjection.getType()

            when (parameterDescriptor.getVariance()) {
                Variance.INVARIANT -> when (projectionKind) {
                    Variance.INVARIANT -> if (lowerThanBound(
                            typeChecker,
                            argument,
                            parameterDescriptor
                        ) || canHaveSubtypes(typeChecker, argument)
                    ) {
                        return true
                    }

                    Variance.IN_VARIANCE -> if (lowerThanBound(
                            typeChecker,
                            argument,
                            parameterDescriptor
                        )
                    ) {
                        return true
                    }

                    Variance.OUT_VARIANCE -> if (canHaveSubtypes(typeChecker, argument)) {
                        return true
                    }
                }

                Variance.IN_VARIANCE -> if (projectionKind != Variance.OUT_VARIANCE) {
                    if (lowerThanBound(
                            typeChecker,
                            argument,
                            parameterDescriptor
                        )
                    ) {
                        return true
                    }
                } else {
                    if (canHaveSubtypes(typeChecker, argument)) {
                        return true
                    }
                }

                Variance.OUT_VARIANCE -> if (projectionKind != Variance.IN_VARIANCE) {
                    if (canHaveSubtypes(typeChecker, argument)) {
                        return true
                    }
                } else {
                    if (lowerThanBound(
                            typeChecker,
                            argument,
                            parameterDescriptor
                        )
                    ) {
                        return true
                    }
                }
            }
            i++
        }
        return false
    }


    @JvmStatic
    fun getClassDescriptor(type: CangJieType): ClassDescriptor? {
        val declarationDescriptor =
            type.constructor.getDeclarationDescriptor()
        if (declarationDescriptor is ClassDescriptor) {
            return declarationDescriptor
        }
        return null
    }

    fun isDontCarePlaceholder(type: CangJieType?): Boolean {
        return type != null && type.constructor === DONT_CARE.constructor
    }

    fun getImmediateSupertypes(type: CangJieType): List<CangJieType> {
        val substitutor: TypeSubstitutor = TypeSubstitutor.create(type)
        val originalSupertypes: Collection<CangJieType> = type.constructor.getSupertypes()
        val result: MutableList<CangJieType> = ArrayList<CangJieType>(originalSupertypes.size)
        for (supertype in originalSupertypes) {
            val substitutedType =
                createSubstitutedSupertype(type, supertype, substitutor)
            if (substitutedType != null) {
                result.add(substitutedType)
            }
        }
        return result
    }

    private fun collectAllSupertypes(type: CangJieType, result: MutableSet<CangJieType>) {
        val immediateSupertypes: List<CangJieType> = getImmediateSupertypes(type)
        result.addAll(immediateSupertypes)
        for (supertype in immediateSupertypes) {
            collectAllSupertypes(supertype, result)
        }
    }

    fun getAllSupertypes(type: CangJieType): Set<CangJieType> {
        // 15 is obtained by experimentation: JDK classes like ArrayList tend to have so many supertypes,
        // the average number is lower
        val result = LinkedHashSet<CangJieType>(15)
        collectAllSupertypes(type, result)
        return result
    }

    @JvmStatic
    fun equalTypes(a: CangJieType, b: CangJieType): Boolean {
        return DEFAULT.equalTypes(a, b)
    }

    @JvmStatic

    fun getDefaultPrimitiveNumberType(supertypes: Collection<CangJieType>): CangJieType? {
        if (supertypes.isEmpty()) {
            return null
        }

//        val builtIns:  CangJieBuiltIns =
//            supertypes.iterator().next().constructor.getBuiltIns()
//        val doubleType:  CangJieType = builtIns.getDoubleType()
//        if (supertypes.contains(doubleType)) {
//            return doubleType
//        }
//        val intType:  CangJieType = builtIns.getIntType()
//        if (supertypes.contains(intType)) {
//            return intType
//        }
//        val longType:  CangJieType = builtIns.getLongType()
//        if (supertypes.contains(longType)) {
//            return longType
//        }
//
//        val uIntType:  CangJieType =
//             TypeUtils.findByFqName(supertypes, uIntFqName)
//        if (uIntType != null) return uIntType
//
//        val uLongType:  CangJieType =
//             TypeUtils.findByFqName(supertypes, uLongFqName)
//        if (uLongType != null) return uLongType

        return null
    }

    private fun findByFqName(
        supertypes: Collection<CangJieType>,
        fqName: FqName
    ): CangJieType? {
        for (supertype in supertypes) {
            val descriptor: ClassifierDescriptor =
                supertype.constructor.getDeclarationDescriptor()
                    ?: continue

            val descriptorFqName: FqNameUnsafe =
                DescriptorUtils.getFqName(descriptor)
            if (descriptorFqName == fqName.toUnsafe()) {
                return supertype
            }
        }
        return null
    }

    @JvmStatic

    fun getDefaultPrimitiveNumberType(numberValueTypeConstructor: IntegerValueTypeConstructor): CangJieType {
        val type =
            getDefaultPrimitiveNumberType(numberValueTypeConstructor.getSupertypes())
                ?: error(
                    "Strange number value type constructor: " + numberValueTypeConstructor + ". " +
                            "Super types doesn't contain double, int or long: " + numberValueTypeConstructor.getSupertypes()
                )
        return type
    }

    @JvmStatic

    fun getPrimitiveNumberType(
        literalTypeConstructor: IntegerLiteralTypeConstructor,
        expectedType: CangJieType
    ): CangJieType {
        if (noExpectedType(expectedType) || expectedType.isError) {
            return literalTypeConstructor.getApproximatedType()
        }

        // If approximated type does not match expected type then expected type is very
        //  specific type (e.g. Comparable<Byte>), so only one of possible types could match it
        val approximatedType: CangJieType = literalTypeConstructor.getApproximatedType()
        if (DEFAULT.isSubtypeOf(approximatedType, expectedType)) {
            return approximatedType
        }

        for (primitiveNumberType in literalTypeConstructor.possibleTypes) {
            if (DEFAULT.isSubtypeOf(
                    primitiveNumberType,
                    expectedType
                )
            ) {
                return primitiveNumberType
            }
        }
        return literalTypeConstructor.getApproximatedType()
    }

    @JvmStatic
    fun getPrimitiveNumberType(
        numberValueTypeConstructor: IntegerValueTypeConstructor,
        expectedType: CangJieType
    ): CangJieType {
        if (noExpectedType(expectedType) || expectedType.isError) {
            return getDefaultPrimitiveNumberType(numberValueTypeConstructor)
        }
        for (primitiveNumberType in numberValueTypeConstructor.getSupertypes()) {
            if (DEFAULT.isSubtypeOf(
                    primitiveNumberType,
                    expectedType
                )
            ) {
                return primitiveNumberType
            }
        }
        return getDefaultPrimitiveNumberType(numberValueTypeConstructor)
    }

    @JvmStatic
    fun makeNullable(type: CangJieType): CangJieType {
        return makeNullableAsSpecified(type, true)
    }

    @JvmStatic
    fun makeNotNullable(type: CangJieType): CangJieType {
        return makeNullableAsSpecified(type, false)
    }

    @JvmStatic
    fun makeStarProjection(parameterDescriptor: TypeParameterDescriptor): TypeProjection {
        return StarProjectionImpl(parameterDescriptor)
    }

    @JvmStatic
    fun makeNullableAsSpecified(
        type: CangJieType,
        nullable: Boolean
    ): CangJieType {
        return type.unwrap().makeNullableAsSpecified(nullable)
    }

    @JvmStatic
    fun makeNullableIfNeeded(
        type: CangJieType,
        nullable: Boolean
    ): CangJieType {
        if (nullable) {
            return makeNullable(type)
        }
        return type
    }

    @JvmStatic
    fun makeNullableIfNeeded(
        type: SimpleType,
        nullable: Boolean
    ): SimpleType {
        if (nullable) {
            return type.makeNullableAsSpecified(true)
        }
        return type
    }

    @JvmStatic
    fun getDefaultTypeProjections(parameters: List<TypeParameterDescriptor>): List<TypeProjection> {
        val result: MutableList<TypeProjection> = mutableListOf()
        for (parameterDescriptor in parameters) {
            result.add(TypeProjectionImpl(parameterDescriptor.getDefaultType()))
        }
        return result.toList()
    }

    @JvmStatic
    fun noExpectedType(type: CangJieType): Boolean {
        return type === NO_EXPECTED_TYPE || type === UNIT_EXPECTED_TYPE
    }

    //    fun makeStarProjection(parameterDescriptor:  TypeParameterDescriptor):  TypeProjection {
//        return  StarProjectionImpl(parameterDescriptor)
//    }
    @JvmStatic

    fun makeUnsubstitutedType(
        typeConstructor: TypeConstructor,
        unsubstitutedMemberScope: MemberScope,
        refinedTypeFactory: Function1<CangJieTypeRefiner, SimpleType?>
    ): SimpleType {
        val arguments: List<TypeProjection> =
            getDefaultTypeProjections(typeConstructor.getParameters())
        return CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope(
            TypeAttributes.Empty,
            typeConstructor,
            arguments,
            false,
            unsubstitutedMemberScope,
            refinedTypeFactory
        )
    }

    @JvmStatic

    fun makeUnsubstitutedType(
        classifierDescriptor: ClassifierDescriptor,
        unsubstitutedMemberScope: MemberScope,
        refinedTypeFactory: (CangJieTypeRefiner?) -> SimpleType?
    ): SimpleType {
        if (ErrorUtils.isError(classifierDescriptor)) {
            return ErrorUtils.createErrorType(
                ErrorTypeKind.UNABLE_TO_SUBSTITUTE_TYPE,
                classifierDescriptor.toString()
            )
        }
        val typeConstructor: TypeConstructor = classifierDescriptor.getTypeConstructor()
        return makeUnsubstitutedType(
            typeConstructor,
            unsubstitutedMemberScope,
            refinedTypeFactory
        )
    }

    private fun contains(
        type: CangJieType?,
        isSpecialType: Function1<UnwrappedType, Boolean>,
        visited: SmartSet<CangJieType>?
    ): Boolean {
        var visited: SmartSet<CangJieType>? = visited
        if (type == null) return false

        val unwrappedType: UnwrappedType = type.unwrap()

        if (noExpectedType(type)) return isSpecialType.invoke(unwrappedType)
        if (visited != null && visited.contains(type)) return false
        if (isSpecialType.invoke(unwrappedType)) return true

        if (visited == null) {
            visited = SmartSet.create()
        }
        visited.add(type)
//
//        val flexibleType:  FlexibleType? =
//            if (unwrappedType is  FlexibleType) unwrappedType as  FlexibleType else null
//        if (flexibleType != null
//            && (contains(flexibleType.lowerBound, isSpecialType, visited)
//                    || contains(flexibleType.upperBound, isSpecialType, visited))
//        ) {
//            return true
//        }

//        if (unwrappedType is  DefinitelyNotNullType &&
//            contains(
//                (unwrappedType as  DefinitelyNotNullType).original,
//                isSpecialType,
//                visited
//            )
//        ) {
//            return true
//        }

        val typeConstructor: TypeConstructor = type.constructor
//        if (typeConstructor is  IntersectionTypeConstructor) {
//            val intersectionTypeConstructor:  IntersectionTypeConstructor =
//                typeConstructor as  IntersectionTypeConstructor
//            for (supertype in intersectionTypeConstructor.getSupertypes()) {
//                if (contains(supertype, isSpecialType, visited)) return true
//            }
//            return false
//        }

        for (projection in type.arguments) {
            if (projection.isStarProjection()) continue
            if (contains(projection.getType(), isSpecialType, visited)) return true
        }
        return false
    }

    fun contains(
        type: CangJieType?,
        isSpecialType: Function1<UnwrappedType, Boolean>
    ): Boolean {
        return contains(type, isSpecialType, null)
    }

    fun contains(type: CangJieType?, specialType: CangJieType): Boolean {
        return contains(type) { type: UnwrappedType? -> specialType.equals(type) }
    }

    val CANNOT_INFER_FUNCTION_PARAM_TYPE: SimpleType =
        ErrorUtils.createErrorType(ErrorTypeKind.UNINFERRED_LAMBDA_PARAMETER_TYPE)

    @JvmField
    val UNIT_EXPECTED_TYPE: SimpleType =
        SpecialType("UNIT_EXPECTED_TYPE")


    /**
     * A work-around of the generic nullability problem in the type checker
     * Semantics should be the same as `!isSubtype(T, Any)`
     * @return true if a value of this type can be null
     */
    fun isNullableType(type: CangJieType): Boolean {


        return false
    }

    open class SpecialType(private val name: String) : DelegatingSimpleType() {
        override val delegate: SimpleType
            get() {
                throw IllegalStateException(name)
            }

        //
//        override fun replaceAttributes(newAttributes:  TypeAttributes):  SimpleType {
//            throw IllegalStateException(name)
//        }
//
        override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType {
            throw IllegalStateException(name)
        }

        override fun toString(): String {
            return name
        }

        @TypeRefinement
        override fun replaceDelegate(delegate: SimpleType): DelegatingSimpleType {
            throw IllegalStateException(name)
        }

        override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType {
            throw IllegalStateException(name)

        }


//        override val arguments: List<TypeProjection>
//            get() = TODO("Not yet implemented")
//        override val attributes: TypeAttributes
//            get() = TODO("Not yet implemented")

//        @TypeRefinement
//        override fun refine(CangJieTypeRefiner:  checker.CangJieTypeRefiner): SpecialType {
//            return this
//        }
    }

    @JvmField
    val NO_EXPECTED_TYPE: SimpleType =
        SpecialType("NO_EXPECTED_TYPE")

}

fun CangJieType.getSupertypeRepresentative(): CangJieType =
    (unwrap() as? SubtypingRepresentatives)?.superTypeRepresentative ?: this

fun CangJieType.isDefaultBound(): Boolean = CangJieBuiltIns.isDefaultBound(getSupertypeRepresentative())
fun List<CangJieType>.defaultProjections(): List<TypeProjection> = map(::TypeProjectionImpl)



/**
 * 块返回值类型推断
 */
fun CjBlockExpression.returnValueInferred(): CangJieType {


    TODO()
}
