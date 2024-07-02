package com.huawei.cangjie.resolve.calls.inference.components

import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.resolve.calls.inference.model.TypeVariableFromCallableDescriptor
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.model.TypeSubstitutorMarker

interface NewTypeSubstitutor : TypeSubstitutorMarker {

    fun safeSubstitute(type: UnwrappedType): UnwrappedType =
        substitute(type, runCapturedChecks = true, keepAnnotation = true) ?: type

    fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType?
    val isEmpty: Boolean
    private fun substitute(type: UnwrappedType, keepAnnotation: Boolean, runCapturedChecks: Boolean): UnwrappedType? =
        when (type) {
            is SimpleType -> substitute(type, keepAnnotation, runCapturedChecks)
            is FlexibleType -> if (type is DynamicType /*|| type is RawType*/) {
                null
            } else {
                val lowerBound = substitute(type.lowerBound, keepAnnotation, runCapturedChecks)
                val upperBound = substitute(type.upperBound, keepAnnotation, runCapturedChecks)
                val enhancement = if (type is TypeWithEnhancement) {
                    substituteTypeEnhancement(type.enhancement, keepAnnotation, runCapturedChecks)
                } else null

                if (lowerBound == null && upperBound == null) {
                    null
                } else {
                    // todo discuss lowerIfFlexible and upperIfFlexible
                    CangJieTypeFactory.flexibleType(
                        lowerBound?.lowerIfFlexible() ?: type.lowerBound,
                        upperBound?.upperIfFlexible() ?: type.upperBound
                    ).wrapEnhancement(if (enhancement is TypeWithEnhancement) enhancement.enhancement else enhancement)
                }
            }
        }

    private fun substituteTypeEnhancement(
        enhancementType: CangJieType,
        keepAnnotation: Boolean,
        runCapturedChecks: Boolean
    ) = when (val type = enhancementType.unwrap()) {
        is SimpleType -> substitute(type, keepAnnotation, runCapturedChecks) ?: enhancementType
        is FlexibleType -> {
            val substitutedLowerBound =
                substitute(type.lowerBound, keepAnnotation, runCapturedChecks) ?: type.lowerBound
            val substitutedUpperBound =
                substitute(type.upperBound, keepAnnotation, runCapturedChecks) ?: type.upperBound
            CangJieTypeFactory.flexibleType(
                substitutedLowerBound.lowerIfFlexible(),
                substitutedUpperBound.upperIfFlexible()
            )
        }
    }
}

object EmptySubstitutor : NewTypeSubstitutor {
    override fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType? = null

    override val isEmpty: Boolean get() = true
}

class NewTypeSubstitutorByConstructorMap(val map: Map<TypeConstructor, UnwrappedType>) : NewTypeSubstitutor {
    override fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType? = map[constructor]

    override val isEmpty: Boolean get() = map.isEmpty()
}

class FreshVariableNewTypeSubstitutor(val freshVariables: List<TypeVariableFromCallableDescriptor>) :
    NewTypeSubstitutor {
    override fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType? {
        val indexProposal = (constructor.declarationDescriptor as? TypeParameterDescriptor)?.index ?: return null
        val typeVariable = freshVariables.getOrNull(indexProposal) ?: return null
        if (typeVariable.originalTypeParameter.typeConstructor != constructor) return null

        return typeVariable.defaultType
    }

    override val isEmpty: Boolean get() = freshVariables.isEmpty()

    companion object {
        val Empty = FreshVariableNewTypeSubstitutor(emptyList())
    }
}
