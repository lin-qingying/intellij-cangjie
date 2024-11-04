package com.linqingying.cangjie.types

import com.linqingying.cangjie.descriptors.annotations.Annotations

class DisjointKeysUnionTypeSubstitution private constructor(
    private val first: TypeSubstitution,
    private val second: TypeSubstitution
) : TypeSubstitution() {
    companion object {
        @JvmStatic fun create(first: TypeSubstitution, second: TypeSubstitution): TypeSubstitution {
            if (first.isEmpty()) return second
            if (second.isEmpty()) return first

            return DisjointKeysUnionTypeSubstitution(first, second)
        }
    }

    override fun get(key: CangJieType) = first[key] ?: second[key]
    override fun prepareTopLevelType(topLevelType: CangJieType, position: Variance) =
        second.prepareTopLevelType(first.prepareTopLevelType(topLevelType, position), position)

    override fun isEmpty() = false

    override fun approximateCapturedTypes() = first.approximateCapturedTypes() || second.approximateCapturedTypes()
    override fun approximateContravariantCapturedTypes() = first.approximateContravariantCapturedTypes() || second.approximateContravariantCapturedTypes()

    override fun filterAnnotations(annotations: Annotations) = second.filterAnnotations(first.filterAnnotations(annotations))
}
