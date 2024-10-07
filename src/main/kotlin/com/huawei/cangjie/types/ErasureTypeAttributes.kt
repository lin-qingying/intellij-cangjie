package com.huawei.cangjie.types

import com.huawei.cangjie.descriptors.TypeParameterDescriptor

enum class TypeUsage {
    SUPERTYPE, COMMON
}

open class ErasureTypeAttributes(
    // we use it to prevent happening a recursion while compute type parameter's upper bounds
    open val howThisTypeIsUsed: TypeUsage,
    open val visitedTypeParameters: Set<TypeParameterDescriptor>? = null,
    open val defaultType: SimpleType? = null
) {
    open fun withDefaultType(type: SimpleType?) = ErasureTypeAttributes(howThisTypeIsUsed, visitedTypeParameters, defaultType = type)

    open fun withNewVisitedTypeParameter(typeParameter: TypeParameterDescriptor) =
        ErasureTypeAttributes(
            howThisTypeIsUsed,
            visitedTypeParameters = visitedTypeParameters?.let { it + typeParameter } ?: setOf(typeParameter),
            defaultType
        )

    override fun equals(other: Any?): Boolean {
        if (other !is ErasureTypeAttributes) return false
        return other.defaultType == this.defaultType && other.howThisTypeIsUsed == this.howThisTypeIsUsed
    }

    override fun hashCode(): Int {
        var result = defaultType.hashCode()
        result += 31 * result + howThisTypeIsUsed.hashCode()
        return result
    }
}
