/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.types.util

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.builtins.StandardNames.FqNames.optionUFqName

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.FqNameUnsafe
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjBlockExpression
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import com.linqingying.cangjie.resolve.constants.IntegerValueTypeConstructor
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.resolve.source.getPsi
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope
import com.linqingying.cangjie.types.checker.*
import com.linqingying.cangjie.types.checker.CangJieTypeChecker.DEFAULT
import com.linqingying.cangjie.types.error.ErrorScopeKind
import com.linqingying.cangjie.types.error.ErrorType
import com.linqingying.cangjie.types.error.ErrorTypeKind
import com.linqingying.cangjie.types.model.TypeArgumentMarker
import com.linqingying.cangjie.types.model.TypeVariableTypeConstructorMarker
import com.linqingying.cangjie.types.util.TypeUtils.isSpecialType
import com.linqingying.cangjie.types.util.TypeUtils.makeProjection
import com.linqingying.cangjie.utils.SmartSet
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun CangJieType.replaceAnnotations(newAnnotations: Annotations): CangJieType {
    if (annotations.isEmpty() && newAnnotations.isEmpty()) return this
    return unwrap().replaceAttributes(attributes.replaceAnnotations(newAnnotations))
}

fun CangJieType.isUnit(): Boolean = CangJieBuiltIns.isUnit(this)

fun CangJieType.unCapture(): CangJieType = unwrap().unCapture()

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
                if (lowerBound.isMarkedOption != upperBound.isMarkedOption)
                    bound.makeOptionalAsSpecified(!preferNotNull)
                else
                    bound
            } else {
                if (this is RawType && preferStarForRaw)
                    upperBound.makeOptionalAsSpecified(!preferNotNull)
                else
                    if (preferNotNull) lowerBound else upperBound
            }

        approximation = approximation.approximateNonDynamicFlexibleTypes()

        approximation =
            if (nullability() == TypeNullability.NOT_NULL) approximation.makeOptionalAsSpecified(false) else approximation

        if (approximation.isMarkedOption && !lowerBound
                .isMarkedOption && TypeUtils.isTypeParameter(approximation) && TypeUtils.hasNullableSuperType(
                approximation
            )
        ) {
            approximation = approximation.makeOptionalAsSpecified(false)
        }

        return approximation
    }

    (unwrap() as? AbbreviatedType)?.let {
        return AbbreviatedType(it.expandedType, it.abbreviation.approximateNonDynamicFlexibleTypes(preferNotNull))
    }
    return simpleTypeWithNonTrivialMemberScope(
        annotations.toDefaultAttributes(),
        constructor,
        arguments.map { it.substitute { type -> type.approximateFlexibleTypes(preferNotNull = true) } },
        isMarkedOption,
        ErrorUtils.createErrorScope(ErrorScopeKind.UNSUPPORTED_TYPE_SCOPE, true, constructor.toString())
    )
}

fun CangJieType.isTypeParameter(): Boolean = TypeUtils.isTypeParameter(this)

fun TypeProjection.substitute(doSubstitute: (CangJieType) -> CangJieType): TypeProjection {

    return TypeProjectionImpl(projectionKind, doSubstitute(type))
}

fun CangJieType.makeOptional() = TypeUtils.makeOptional(this)
fun CangJieType.makeNotNullable() = TypeUtils.makeNotNullable(this)

fun CangJieType.isInterface(): Boolean =
    (constructor.declarationDescriptor as? ClassDescriptor)?.kind == ClassKind.INTERFACE

fun CangJieType.isStruct(): Boolean = (constructor.declarationDescriptor as? ClassDescriptor)?.kind == ClassKind.STRUCT

fun CangJieType.isEnum(): Boolean = (constructor.declarationDescriptor as? ClassDescriptor)?.kind == ClassKind.ENUM
fun CangJieType.isEnumEntry(): Boolean =
    (constructor.declarationDescriptor as? ClassDescriptor)?.kind == ClassKind.ENUM_ENTRY

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

fun FlexibleType.unCapture(): FlexibleType {
    val unCapturedLowerBound = when (val unCaptured = lowerBound.unCapture()) {
        is SimpleType -> unCaptured
        is FlexibleType -> unCaptured.lowerBound
    }

    val unCapturedUpperBound = when (val unCaptured = upperBound.unCapture()) {
        is SimpleType -> unCaptured
        is FlexibleType -> unCaptured.upperBound
    }

    return FlexibleTypeImpl(unCapturedLowerBound, unCapturedUpperBound)
}

fun unCaptureProjection(projection: TypeProjection): TypeProjection {
    val unCapturedProjection = (projection.type.constructor as? NewCapturedTypeConstructor)?.projection ?: projection
    if (unCapturedProjection.type is ErrorType) return unCapturedProjection

    val newArguments = unCapturedProjection.type.arguments.map(::unCaptureProjection)
    return TypeProjectionImpl(
        unCapturedProjection.projectionKind,
        unCapturedProjection.type.replace(newArguments)
    )
}

fun SimpleType.unCapture(): UnwrappedType {
    if (this is ErrorType) return this
    if (this is NewCapturedType)
        return unCaptureTopLevelType()

    val newArguments = arguments.map(::unCaptureProjection)
    return replace(newArguments).unwrap()
}

private fun NewCapturedType.unCaptureTopLevelType(): UnwrappedType {
    if (lowerType != null) return lowerType

    val supertypes = constructor.supertypes
    if (supertypes.isNotEmpty()) return intersectTypes(supertypes)

    return constructor.projection.type.unwrap()
}

fun AbbreviatedType.unCapture(): SimpleType {
    val newType = expandedType.unCapture()
    return AbbreviatedType(newType as? SimpleType ?: expandedType, abbreviation)
}

fun CangJieType.expandIntersectionTypeIfNecessary(): Collection<CangJieType> {
    if (constructor !is IntersectionTypeConstructor) return listOf(this)
    val types = constructor.supertypes
    return if (isMarkedOption) {
        types.map { it.makeOptional() }
    } else {
        types
    }
}


val CangJieType.builtIns: CangJieBuiltIns
    get() = constructor.builtIns

fun CangJieType.contains(predicate: (UnwrappedType) -> Boolean) = TypeUtils.contains(this, predicate)
fun CangJieType.isSignedOrUnsignedNumberType(): Boolean = isPrimitiveNumberType() /*|| isUnsignedNumberType()*/
fun CangJieType.isStubTypeForBuilderInference(): Boolean =
    this is StubTypeForBuilderInference || isDefNotNullStubType<StubTypeForBuilderInference>()

fun CangJieType.isPrimitiveNumberType(): Boolean = CangJieBuiltIns.isPrimitiveType(this) && !isBoolean()
fun CangJieType.isBoolean(): Boolean = CangJieBuiltIns.isBoolean(this)

//fun CangJieType.isNotNullThrowable(): Boolean = CangJieBuiltIns.isThrowableOrNullableThrowable(this) && !isMarkedNullable
fun CangJieType.isInt8() = CangJieBuiltIns.isInt8(this)
fun CangJieType.isRune() = CangJieBuiltIns.isRune(this)
fun CangJieType.isInt16() = CangJieBuiltIns.isInt16(this)
fun CangJieType.isInt32() = CangJieBuiltIns.isInt32(this)
fun CangJieType.isInt64() = CangJieBuiltIns.isInt64(this)
fun CangJieType.isFloat16() = CangJieBuiltIns.isFloat16(this)
fun CangJieType.isFloat32() = CangJieBuiltIns.isFloat32(this)
fun CangJieType.isFloat64() = CangJieBuiltIns.isFloat64(this)

//fun CangJieType.isUnsignedNumberType(): Boolean = UnsignedTypes.isUnsignedType(this)

fun CangJieType.asTypeProjection(): TypeProjection = TypeProjectionImpl(this)
fun CangJieType.isStubType() = this is AbstractStubType || isDefNotNullStubType<AbstractStubType>()
private inline fun <reified S : AbstractStubType> CangJieType.isDefNotNullStubType() =
    this is DefinitelyNotNullType && this.original is S

fun CangJieType.isStubTypeForVariableInSubtyping(): Boolean =
    this is StubTypeForTypeVariablesInSubtyping || isDefNotNullStubType<StubTypeForTypeVariablesInSubtyping>()

fun CangJieType?.shouldBeUpdated() =
    this == null || contains { it is StubTypeForBuilderInference || it.constructor is TypeVariableTypeConstructorMarker || it.isError }

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
        if (isTypeParameterVisited) return@any false
        argument.type.containsSelfTypeParameter(baseConstructor, visitedTypeParameters)
    }
}

inline fun SimpleType.replaceArgumentsByExistingArgumentsWith(replacement: (TypeArgumentMarker) -> TypeArgumentMarker): SimpleType {
    if (arguments.isEmpty()) return this
    return replace(newArguments = arguments.map { replacement(it) as TypeProjection })
}


fun createBasicType(
    builtIns: CangJieBuiltIns,
    name: String,
    extendTypesDescriptor: Set<LazyExtendClassDescriptor> = emptySet()
): BasicType {
    val classDescriptor = builtIns.getBuiltInBasicTypeByName(name)
//    classDescriptor.extendClassDescriptor.addAll(extendTypesDescriptor)

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

fun CangJieType.isGenericArrayOfTypeParameter(): Boolean {
    if (!CangJieBuiltIns.isArray(this)) return false
    val argument0 = arguments[0]

    val argument0type = argument0.type
    return argument0type.isTypeParameter() ||
            argument0type.isGenericArrayOfTypeParameter()
}

/**
 * 如果是Option类型，获取原类型
 */
fun CangJieType.getOriginalType(): CangJieType {

    if (CangJieBuiltIns.isOptionType(this)) {
        return arguments[0].type

    }
    return this
}

fun CangJieTypeChecker.equalTypesOrNulls(type1: CangJieType?, type2: CangJieType?): Boolean {
    if (type1 === type2) return true
    if (type1 == null || type2 == null) return false
    return equalTypes(type1, type2)
}

fun CangJieType.isPrimitiveNumber(): Boolean =
    CangJieBuiltIns.isPrimitiveType(this) &&
            !CangJieBuiltIns.isBoolean(this) &&
            !CangJieBuiltIns.isRune(this)

fun CangJieType.isAny(): Boolean = CangJieBuiltIns.isAny(this)
fun CangJieType.containsError() = ErrorUtils.containsErrorType(this)
val TypeParameterDescriptor.representativeUpperBound: CangJieType
    get() {
        assert(upperBounds.isNotEmpty()) { "Upper bounds should not be empty: $this" }

        return upperBounds.firstOrNull {
            val classDescriptor = it.constructor.declarationDescriptor as? ClassDescriptor ?: return@firstOrNull false
            classDescriptor.kind != ClassKind.INTERFACE && classDescriptor.kind != ClassKind.ANNOTATION_CLASS
        } ?: upperBounds.first()
    }

inline fun CangJieType.replaceArgumentsByParametersWith(replacement: (TypeParameterDescriptor) -> TypeProjection): CangJieType {
    val unwrapped = unwrap()
    return when (unwrapped) {
        is FlexibleType -> CangJieTypeFactory.flexibleType(
            unwrapped.lowerBound.replaceArgumentsByParametersWith(replacement),
            unwrapped.upperBound.replaceArgumentsByParametersWith(replacement)
        )

        is SimpleType -> unwrapped.replaceArgumentsByParametersWith(replacement)
    }.inheritEnhancement(unwrapped)
}

fun CangJieType.containsTypeParameter(): Boolean = TypeUtils.contains(this) { t -> TypeUtils.isTypeParameter(t) }

inline fun SimpleType.replaceArgumentsByParametersWith(replacement: (TypeParameterDescriptor) -> TypeProjection): SimpleType {
    if (constructor.parameters.isEmpty() || constructor.declarationDescriptor == null) return this

    val newArguments = constructor.parameters.map(replacement)

    return replace(newArguments)
}

fun CangJieType.isBooleanOrNullableBoolean(): Boolean = CangJieBuiltIns.isBooleanOrNullableBoolean(this)
private fun CangJieType.extractTypeParametersFromUpperBounds(
    baseType: CangJieType,
    to: MutableSet<TypeParameterDescriptor>,
    visitedTypeParameters: Set<TypeParameterDescriptor>?
) {
    val declarationDescriptor = constructor.declarationDescriptor

    if (declarationDescriptor is TypeParameterDescriptor) {
        if (constructor != baseType.constructor) {
            to += declarationDescriptor
        } else {
            for (upperBound in declarationDescriptor.upperBounds) {
                upperBound.extractTypeParametersFromUpperBounds(baseType, to, visitedTypeParameters)
            }
        }
    } else {
        val typeParameters =
            (constructor.declarationDescriptor as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters
        for ((i, argument) in arguments.withIndex()) {
            val typeParameter = typeParameters?.getOrNull(i) // TODO: support inner classes' type parameters
            val isTypeParameterVisited =
                typeParameter != null && visitedTypeParameters != null && typeParameter in visitedTypeParameters
            if (isTypeParameterVisited) continue
            if (argument.type.constructor.declarationDescriptor in to || argument.type.constructor == baseType.constructor) continue
            argument.type.extractTypeParametersFromUpperBounds(baseType, to, visitedTypeParameters)
        }
    }
}

fun CangJieType.replaceArgumentsWithProjections() = replaceArgumentsByParametersWith(::makeProjection)

fun CangJieType.extractTypeParametersFromUpperBounds(visitedTypeParameters: Set<TypeParameterDescriptor>?): Set<TypeParameterDescriptor> =
    mutableSetOf<TypeParameterDescriptor>().also {
        extractTypeParametersFromUpperBounds(
            this,
            it,
            visitedTypeParameters
        )
    }

/**
 * 根据Name获取指定父类型 ，如果获取不到，则返回第一个父类型
 * @param name Name
 */
fun CangJieType.extractSuperType(name: Name): CangJieType {
    val superTypes = this.supertypes()
    for (superType in superTypes) {
        if (superType.constructor.declarationDescriptor?.name == name) {
            return superType
        }
    }
    return superTypes.first()

}

fun CangJieType.isAnyOrNullableAny(): Boolean = CangJieBuiltIns.isAnyOrNullableAny(this)
fun CangJieType.isNothing(): Boolean = CangJieBuiltIns.isNothing(this)

fun createProjection(
    type: CangJieType,
    projectionKind: Variance,
    typeParameterDescriptor: TypeParameterDescriptor?
): TypeProjection =
    TypeProjectionImpl(
        if (typeParameterDescriptor?.variance == projectionKind) Variance.INVARIANT else projectionKind,
        type
    )

fun CangJieType?.isArrayOfNothing(): Boolean {
    if (this == null || !CangJieBuiltIns.isArray(this)) return false
    val typeArg = arguments.firstOrNull()?.type
    return typeArg != null && CangJieBuiltIns.isNothingOrNullableNothing(typeArg)
}

fun CangJieType.constituentTypes(): Collection<CangJieType> =
    constituentTypes(listOf(this))

private fun constituentTypes(result: MutableSet<CangJieType>, types: Collection<CangJieType>) {
    result.addAll(types)
    for (type in types) {
        if (type.isFlexible()) {
            with(type.asFlexibleType()) { constituentTypes(result, setOf(lowerBound, upperBound)) }
        } else {
            constituentTypes(result, type.arguments.map { it.type })
        }
    }
}

fun constituentTypes(types: Collection<CangJieType>): Collection<CangJieType> {
    val result = hashSetOf<CangJieType>()
    constituentTypes(result, types)
    return result
}

val CangJieType.source: CjElement? get() = constructor.declarationDescriptor?.source?.getPsi() as? CjElement

object TypeUtils {

    val DONT_CARE: SimpleType = ErrorUtils.createErrorType(ErrorTypeKind.DONT_CARE)
    fun isNonReifiedTypeParameter(type: CangJieType): Boolean {
        val typeParameterDescriptor =
            getTypeParameterDescriptorOrNull(type)
        return typeParameterDescriptor != null
    }

    fun isSpecialType(type: CangJieType): Boolean {
        return type is SpecialType
    }

    fun makeProjection(parameterDescriptor: TypeParameterDescriptor): TypeProjection {
        return TypeProjectionImpl(parameterDescriptor.defaultType)
    }

    fun makeProjection(
        parameterDescriptor: TypeParameterDescriptor,
        attr: ErasureTypeAttributes
    ): TypeProjection {
//        return if (attr.howThisTypeIsUsed ==  TypeUsage.SUPERTYPE) {
        return TypeProjectionImpl(parameterDescriptor.projectionType())
//        } else {
//           StarProjectionImpl(parameterDescriptor)
//        }
    }

    /**
     * Differs from `isNullableType` only by treating type parameters: acceptsNullable(T) <=> T has nullable lower bound
     * Semantics should be the same as `isSubtype(Nothing?, T)`
     * @return true if `null` can be assigned to storage of this type
     */
    fun acceptsNullable(type: CangJieType): Boolean {
        if (type.isMarkedOption) {
            return true
        }
        if (type.isFlexible() && acceptsNullable(type.asFlexibleType().upperBound)) {
            return true
        }
        return false
    }

    fun hasNullableSuperType(type: CangJieType): Boolean {
        if (type.constructor.getDeclarationDescriptor() is ClassDescriptor) {
            // A class/trait cannot have a nullable supertype
            return false
        }

        for (supertype in getImmediateSupertypes(type)) {
            if (isNullableType(supertype)) return true
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


    /**
     * 将other添加到stub的泛型参数中
     */
    fun addTypeParameterToStub(stub: CangJieType, vararg other: CangJieType): CangJieType {

        if (stub.constructor.parameters.size != other.size) return stub
        val arguments = mutableListOf<TypeProjection>()
        other.forEach {
            arguments.add(TypeProjectionImpl(it))
        }
        return CangJieTypeFactory.optionType(
            CangJieTypeFactory.simpleType(
                stub.attributes,
                stub.constructor,
                arguments,
                true,
                null
            )
        )
//        return CangJieTypeFactory.simpleType(
//            stub.attributes,
//            stub.constructor,
//            arguments,
//            true,
//            null
//        )

    }

    fun createSubstitutedSupertype(
        subType: CangJieType,
        superType: CangJieType,
        substitutor: TypeSubstitutor
    ): CangJieType? {
        val substitutedType: CangJieType? = substitutor.substitute(superType, Variance.INVARIANT)
        if (substitutedType != null) {
            return makeOptionalIfNeeded(substitutedType, subType.isMarkedOption)
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
        if (type.isMarkedOption) {
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
        val result = ArrayList<CangJieType>(originalSupertypes.size)
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
    fun makeOptional(type: CangJieType): CangJieType {
        return makeOptionalAsSpecified(type, true)
    }

    @JvmStatic
    fun makeNotNullable(type: CangJieType): CangJieType {
        return makeOptionalAsSpecified(type, false)
    }


    @JvmStatic
    fun makeOptionalAsSpecified(
        type: CangJieType,
        optional: Boolean
    ): CangJieType {
        return type.unwrap().makeOptionalAsSpecified(optional)
    }

    @JvmStatic
    fun makeOptionalIfNeeded(
        type: CangJieType,
        optional: Boolean
    ): CangJieType {
        if (optional) {
            return makeOptional(type)
        }
        return type
    }

    @JvmStatic
    fun makeOptionalIfNeeded(
        type: SimpleType,
        optional: Boolean
    ): SimpleType {
        if (optional) {
            return type.makeOptionalAsSpecified(true)
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
        return type === NO_EXPECTED_TYPE || type === UNIT_EXPECTED_TYPE || type === EXPRESSION_TYPE
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
        return simpleTypeWithNonTrivialMemberScope(
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
    @JvmStatic
    fun isNullableType(type: CangJieType): Boolean {
        if (type.isMarkedOption) {
            return true
        }
        if (type.isFlexible() && isNullableType(type.asFlexibleType().upperBound)) {
            return true
        }
        if (type.isDefinitelyNotNullType) {
            return false
        }
        if (isTypeParameter(type)) {
            return hasNullableSuperType(type)
        }
        if (type is AbstractStubType) {
            val typeVariableConstructor: NewTypeVariableConstructor =
                type.originalTypeVariable
            val typeParameter =
                typeVariableConstructor.originalTypeParameter
            return typeParameter == null || hasNullableSuperType(typeParameter.getDefaultType())
        }
        val constructor: TypeConstructor = type.constructor
        if (constructor is IntersectionTypeConstructor) {
            for (supertype in constructor.getSupertypes()) {
                if (isNullableType(supertype)) return true
            }
        }

        return CangJieBuiltIns.isOptionType(type)


    }

    /**
     * 检查构造方法是否有参数
     */
    @JvmStatic
    fun checkConstructorsNotParameter(classDescriptor: ClassDescriptor): Boolean {
        val constructors = classDescriptor.constructors

        return !constructors.any {
            it.valueParameters.isEmpty()
        }

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
        override fun makeOptionalAsSpecified(newNullability: Boolean): SimpleType {
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

    //    表示不需要返回值的地方，一般用于if  try 之类的多结果表达式
    @JvmField
    val EXPRESSION_TYPE: SimpleType =
        SpecialType("EXPRESSION_TYPE")

    //    表示没有指定类型，需要推断，用于需要返回值的地方
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

fun isTypeConstructorForGivenClass(
    typeConstructor: TypeConstructor,
    fqName: FqNameUnsafe
): Boolean {

    val descriptor =
        typeConstructor.getDeclarationDescriptor()
    return descriptor is ClassDescriptor && classFqNameEquals(
        descriptor,
        fqName
    )
}


fun isNotNullConstructedFromGivenClass(
    type: CangJieType,
    fqName: FqNameUnsafe
): Boolean {
    if (isSpecialType(type)) return false

    return isTypeConstructorForGivenClass(type.constructor, fqName)
}

fun isConstructedFromGivenClass(
    type: CangJieType,
    fqName: FqNameUnsafe
): Boolean {
    if (isSpecialType(type)) return false
    return if (isTypeConstructorForGivenClass(type.constructor, fqName)) {
        true
    } else if (type.constructor.declarationDescriptor is ClassDescriptor && DescriptorUtils.getFqName(type.constructor.declarationDescriptor!!) == optionUFqName) {

        isConstructedFromGivenClass(type.arguments[0].type, fqName)
    } else {
        false
    }
}

fun classFqNameEquals(
    descriptor: ClassifierDescriptor,
    fqName: FqNameUnsafe
): Boolean {

    return descriptor.name == fqName.shortName() && fqName == DescriptorUtils.getFqName(
        descriptor
    )

}

fun CangJieType.isSubtypeOf(superType: CangJieType): Boolean = DEFAULT.isSubtypeOf(this, superType)

@JvmName("isSubtypeOfNull")
fun CangJieType?.isSubtypeOf(superType: CangJieType?): Boolean {
    if (this == null || superType == null) return false
    return DEFAULT.isSubtypeOf(this, superType)
}


//获取类型的classkind
val CangJieType.classKind: ClassKind
    get() {
        return when (val descriptor = this.constructor.declarationDescriptor) {
            is ClassDescriptor -> descriptor.kind
            else -> error("not a class: $this")
        }

    }

/**
 * 封装了一个类型替换的过程，其中一个 <code>CangJieType</code> 类型被另一个 <code>CangJieType</code> 类型替代。
 * 该类表示一个映射关系，将特定类型（forType）替换为新的类型（byType）。
 *
 * @property forType 被替换的原始类型。
 * @property byType 用来替代的新的类型。
 */
internal class CangJieTypeSubstitution(val forType: CangJieType, val byType: CangJieType)

fun CangJieType.substitute(byType: CangJieType): CangJieType {
    return substitute(CangJieTypeSubstitution(this, byType))
}

fun CangJieType.replaceArgument(vararg newType: CangJieType): CangJieType {

    val arguments = newType.map {
        TypeProjectionImpl(it)
    }
    return simpleTypeWithNonTrivialMemberScope(
        attributes,
        constructor,
        arguments,
        isMarkedOption,
        memberScope
    )
}

internal fun CangJieType.substitute(substitution: CangJieTypeSubstitution): CangJieType {
    val nullable = isMarkedOption
    val currentType = makeNotNullable()

    return if (DEFAULT.equalTypes(currentType, substitution.forType)
    ) {
        TypeUtils.makeOptionalAsSpecified(substitution.byType, nullable)
    } else {
        val newArguments = arguments.zip(constructor.parameters).map { pair ->
            val (projection, typeParameter) = pair
            TypeProjectionImpl(Variance.INVARIANT, projection.type.substitute(substitution))
        }
        simpleTypeWithNonTrivialMemberScope(
            annotations.toDefaultAttributes(),
            constructor,
            newArguments,
            isMarkedOption,
            memberScope
        )
    }
}

fun CangJieType.immediateSupertypes(): Collection<CangJieType> = TypeUtils.getImmediateSupertypes(this)


val CangJieType.deccriptorClass: ClassDescriptor?
    get() {

        return constructor.declarationDescriptor as? ClassDescriptor
    }
