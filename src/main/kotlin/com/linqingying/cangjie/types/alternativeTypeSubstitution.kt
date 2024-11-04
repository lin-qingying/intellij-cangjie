package com.linqingying.cangjie.types

import com.linqingying.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.linqingying.cangjie.types.util.contains

fun substituteAlternativesInPublicType(type: CangJieType): UnwrappedType {
    val substitutor = object : NewTypeSubstitutor {
        override fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType? {
            if (constructor is IntersectionTypeConstructor) {
                constructor.getAlternativeType()?.let { alternative ->
                    return safeSubstitute(alternative.unwrap())
                }
            }

            return null
        }

        override val isEmpty: Boolean by lazy {
            !type.contains { it.constructor is IntersectionTypeConstructor }
        }
    }

    return substitutor.safeSubstitute(type.unwrap())
}
