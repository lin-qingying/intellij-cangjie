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

package cn.cangnova.cangjie.types

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.descriptors.ClassifierDescriptor
import cn.cangnova.cangjie.resolve.scopes.MemberScope
import cn.cangnova.cangjie.types.CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope
import cn.cangnova.cangjie.types.TypeUtils.getDefaultPrimitiveNumberType
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.types.error.ErrorTypeKind
import cn.cangnova.cangjie.utils.SmartSet

val CangJieType.builtIns: CangJieBuiltIns
    get() = constructor.builtIns

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

        return CangJieTypeFactory.simpleType(
            stub.attributes,
            stub.constructor,
            arguments,
            true,
            null
        )

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
        literalTypeConstructor: FloatLiteralTypeConstructor,
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
        return type === NO_EXPECTED_TYPE || type === UNIT_EXPECTED_TYPE
    }

    //    fun makeStarProjection(parameterDescriptor:  TypeParameterDescriptor):  TypeProjection {
//        return  StarProjectionImpl(parameterDescriptor)
//    }
    @JvmStatic

    fun makeUnsubstitutedType(
        typeConstructor: TypeConstructor,
        unsubstitutedMemberScope: MemberScope,
        refinedTypeFactory: (CangJieTypeRefiner) -> SimpleType?
    ): SimpleType {
        val arguments: List<TypeProjection> =
            getDefaultTypeProjections(typeConstructor.parameters)
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
        refinedTypeFactory: (CangJieTypeRefiner) -> SimpleType?
    ): SimpleType {
        if (ErrorUtils.isError(classifierDescriptor)) {
            return ErrorUtils.createErrorType(
                ErrorTypeKind.UNABLE_TO_SUBSTITUTE_TYPE,
                classifierDescriptor.toString()
            )
        }
        val typeConstructor: TypeConstructor = classifierDescriptor.typeConstructor
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
//    @JvmField
//    val EXPRESSION_TYPE: SimpleType =
//        SpecialType("EXPRESSION_TYPE")

    //    表示没有指定类型，需要推断，用于需要返回值的地方
    @JvmField
    val NO_EXPECTED_TYPE: SimpleType =
        SpecialType("NO_EXPECTED_TYPE")

}
