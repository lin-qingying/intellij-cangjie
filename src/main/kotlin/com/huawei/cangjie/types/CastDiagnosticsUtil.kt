package com.huawei.cangjie.types

import com.google.common.collect.Maps
import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.PlatformToCangJieClassMapper
import com.huawei.cangjie.builtins.isExtensionFunctionType
import com.huawei.cangjie.builtins.isFunctionType
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.types.checker.CangJieTypeChecker
import com.huawei.cangjie.types.checker.TypeCheckingProcedure
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.types.util.makeNotNullable

object CastDiagnosticsUtil {
    private fun isUpcast(candidateType: CangJieType, targetType: CangJieType): Boolean {
        if (!CangJieTypeChecker.DEFAULT.isSubtypeOf(candidateType, targetType)) return false

        if (candidateType.isFunctionType && targetType.isFunctionType) {
            return candidateType.isExtensionFunctionType == targetType.isExtensionFunctionType
        }

        return true
    }

    // It is a warning "useless cast" for `as` and a warning "redundant is" for `is`
    fun isRefinementUseless(
        possibleTypes: Collection<CangJieType>,
        targetType: CangJieType,
        shouldCheckForExactType: Boolean
    ): Boolean {
        val intersectedType = TypeIntersector.intersectTypes(possibleTypes.map { it.upperIfFlexible() }) ?: return false

        return if (shouldCheckForExactType)
            isExactTypeCast(intersectedType, targetType)
        else
            isUpcast(intersectedType, targetType)
    }
    private fun isExactTypeCast(candidateType: CangJieType, targetType: CangJieType): Boolean {
        return candidateType == targetType && candidateType.isExtensionFunctionType == targetType.isExtensionFunctionType
    }
    // As this method produces a warning, it must be _complete_ (not sound), i.e. every time it says "cast impossible",
    // it must be really impossible
    @JvmStatic
    fun isCastPossible(
        lhsType: CangJieType,
        rhsType: CangJieType,
        platformToCangJieClassMapper: PlatformToCangJieClassMapper,
//        platformSpecificCastChecker: PlatformSpecificCastChecker
    ): Boolean {
        val typeConstructor = lhsType.constructor
        if (typeConstructor is IntersectionTypeConstructor) {
            return typeConstructor.supertypes.any { isCastPossible(it, rhsType, platformToCangJieClassMapper,/* platformSpecificCastChecker*/) }
        }
        val rhsNullable = TypeUtils.isNullableType(rhsType)
        val lhsNullable = TypeUtils.isNullableType(lhsType)
        if (CangJieBuiltIns.isNothing(lhsType)) return true
        if (CangJieBuiltIns.isNullableNothing(lhsType) && !rhsNullable) return false
        if (CangJieBuiltIns.isNothing(rhsType)) return false
        if (CangJieBuiltIns.isNullableNothing(rhsType)) return lhsNullable
        if (lhsNullable && rhsNullable) return true
        if (lhsType.isError) return true
        if (isRelated(lhsType, rhsType, platformToCangJieClassMapper)) return true
        // This is an oversimplification (which does not render the method incomplete):
        // we consider any type parameter capable of taking any value, which may be made more precise if we considered bounds
        if (TypeUtils.isTypeParameter(lhsType) || TypeUtils.isTypeParameter(rhsType)) return true
//        if (platformSpecificCastChecker.isCastPossible(lhsType, rhsType)) return true

        return false
    }

    /**
     * Two types are related, roughly, when one of them is a subtype of the other constructing class
     *
     * Note that some types have platform-specific counterparts, i.e. kotlin.String is mapped to java.lang.String,
     * such types (and all their sub- and supertypes) are related too.
     *
     * Due to limitations in PlatformToCangJieClassMap, we only consider mapping of platform classes to CangJie classed
     * (i.e. java.lang.String -> kotlin.String) and ignore mappings that go the other way.
     */
    private fun isRelated(a: CangJieType, b: CangJieType, platformToCangJieClassMapper: PlatformToCangJieClassMapper): Boolean {
        val aClasses = mapToPlatformIndependentClasses(a, platformToCangJieClassMapper)
        val bClasses = mapToPlatformIndependentClasses(b, platformToCangJieClassMapper)

        return aClasses.any { DescriptorUtils.isSubtypeOfClass(b, it) } || bClasses.any { DescriptorUtils.isSubtypeOfClass(a, it) }
    }

    private fun mapToPlatformIndependentClasses(
        type: CangJieType,
        platformToCangJieClassMapper: PlatformToCangJieClassMapper
    ): List<ClassDescriptor> {
        val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return listOf()

        return platformToCangJieClassMapper.mapPlatformClass(descriptor) + descriptor
    }
    /**
     * Check if cast from supertype to subtype is erased.
     * It is an error in "is" statement and warning in "as".
     */
    @JvmStatic
    fun isCastErased(supertype: CangJieType, subtype: CangJieType, typeChecker: CangJieTypeChecker): Boolean {
        val isNonReifiedTypeParameter = TypeUtils.isNonReifiedTypeParameter(subtype)
        val isUpcast = typeChecker.isSubtypeOf(supertype, subtype)

        // here we want to restrict cases such as `x is T` for x = T?, when T might have nullable upper bound
        if (isNonReifiedTypeParameter && !isUpcast) {
            // hack to save previous behavior in case when `x is T`, where T is not nullable, see IsErasedNullableTasT.kt
            val nullableToDefinitelyNotNull = !TypeUtils.isNullableType(subtype) && supertype.makeNotNullable() == subtype
            if (!nullableToDefinitelyNotNull) {
                return true
            }
        }

        // cast between T and T? is always OK
        if (supertype.isMarkedOption || subtype.isMarkedOption) {
            return isCastErased(TypeUtils.makeNotNullable(supertype), TypeUtils.makeNotNullable(subtype), typeChecker)
        }

        // if it is a upcast, it's never erased
        if (isUpcast) return false

        // downcasting to a non-reified type parameter is always erased
        if (isNonReifiedTypeParameter) return true



        val staticallyKnownSubtype = findStaticallyKnownSubtype(supertype, subtype.constructor).resultingType ?: return true

        // If the substitution failed, it means that the result is an impossible type, e.g. something like Out<in Foo>
        // In this case, we can't guarantee anything, so the cast is considered to be erased

        // If the type we calculated is a subtype of the cast target, it's OK to use the cast target instead.
        // If not, it's wrong to use it
        return !typeChecker.isSubtypeOf(staticallyKnownSubtype, subtype)
    }

    @JvmStatic
    fun findStaticallyKnownSubtype(supertype: CangJieType, subtypeConstructor: TypeConstructor): TypeReconstructionResult {
        assert(!supertype.isMarkedOption) { "This method only makes sense for non-nullable types" }

        // Assume we are casting an expression of type Collection<Foo> to List<Bar>
        // First, let's make List<T>, where T is a type variable
        val descriptor = subtypeConstructor.declarationDescriptor ?: error("Can't create default type for " + subtypeConstructor)
        val subtypeWithVariables = descriptor.defaultType

        // Now, let's find a supertype of List<T> that is a Collection of something,
        // in this case it will be Collection<T>
        val supertypeWithVariables = TypeCheckingProcedure.findCorrespondingSupertype(subtypeWithVariables, supertype)

        val variables = subtypeWithVariables.constructor.parameters
        val variableConstructors = variables.map(TypeParameterDescriptor::getTypeConstructor).toSet()

        val substitution: MutableMap<TypeConstructor, TypeProjection> = if (supertypeWithVariables != null) {
            // Now, let's try to unify Collection<T> and Collection<Foo> solution is a map from T to Foo
            val solution = TypeUnifier.unify(
                TypeProjectionImpl(supertype), TypeProjectionImpl(supertypeWithVariables), variableConstructors::contains
            )
            Maps.newHashMap(solution.substitution)
        } else {
            // If there's no corresponding supertype, no variables are determined
            // This may be OK, e.g. in case 'Any as List<*>'
            Maps.newHashMapWithExpectedSize<TypeConstructor, TypeProjection>(variables.size)
        }

        // If some of the parameters are not determined by unification, it means that these parameters are lost,
        // let's put stars instead, so that we can only cast to something like List<*>, e.g. (a: Any) as List<*>
        var allArgumentsInferred = true
        for (variable in variables) {
            val value = substitution[variable.typeConstructor]
            if (value == null) {
                substitution[variable.typeConstructor] =  TypeUtils.makeProjection(variable)
                allArgumentsInferred = false
            }
        }

        // At this point we have values for all type parameters of List
        // Let's make a type by substituting them: List<T> -> List<Foo>
        val substituted = TypeSubstitutor.create(substitution).substitute(subtypeWithVariables, Variance.INVARIANT)

        return TypeReconstructionResult(substituted, allArgumentsInferred)
    }
}
