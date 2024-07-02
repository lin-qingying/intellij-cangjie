package com.huawei.cangjie.types

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.ClassifierDescriptor
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.types.ErrorUtils.createErrorType
import com.huawei.cangjie.types.ErrorUtils.isError
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.error.ErrorTypeKind
import com.huawei.cangjie.utils.SmartSet


//fun CangJieType?.shouldBeUpdated() =
//    this == null || contains { it is StubTypeForBuilderInference || it.constructor is TypeVariableTypeConstructorMarker || it.isError }

val CangJieType.builtIns: CangJieBuiltIns
    get() = constructor.builtIns

object TypeUtils {
    fun makeNullable(type: CangJieType): CangJieType {
        return makeNullableAsSpecified(type, true)
    }

    fun makeNullableAsSpecified(
        type: CangJieType,
        nullable: Boolean
    ): CangJieType {
        return type.unwrap().makeNullableAsSpecified(nullable)
    }

    fun makeNullableIfNeeded(
        type: CangJieType,
        nullable: Boolean
    ): CangJieType {
        if (nullable) {
            return makeNullable(type)
        }
        return type
    }

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
        refinedTypeFactory: Function1<CangJieTypeRefiner, SimpleType>
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
        refinedTypeFactory: Function1<CangJieTypeRefiner?, SimpleType>
    ): SimpleType {
        if (isError(classifierDescriptor)) {
            return createErrorType(
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

    val CANNOT_INFER_FUNCTION_PARAM_TYPE: SimpleType =
        createErrorType(ErrorTypeKind.UNINFERRED_LAMBDA_PARAMETER_TYPE)

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
//        override fun refine(kotlinTypeRefiner:  checker.CangJieTypeRefiner): SpecialType {
//            return this
//        }
    }

    @JvmField
    val NO_EXPECTED_TYPE: SimpleType =
        SpecialType("NO_EXPECTED_TYPE")

}