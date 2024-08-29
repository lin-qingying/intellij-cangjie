package com.huawei.cangjie.types

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.TypeParameterDescriptor

object SubstitutionUtils {
    // we use the mutability of the substitution map here
    private fun fillInDeepSubstitutor(
        context:  CangJieType,
        substitutor:  TypeSubstitutor,
        substitution: MutableMap< TypeConstructor,  TypeProjection>,
        typeParameterMapping: Multimap< TypeParameterDescriptor,  TypeProjection>?
    ) {
        val parameters: List< TypeParameterDescriptor> =
            context.constructor.getParameters()
        val arguments: List< TypeProjection> = context.arguments

        check(parameters.size == arguments.size)

        for (i in arguments.indices) {
            val argument:  TypeProjection = arguments[i]
            val parameter:  TypeParameterDescriptor = parameters[i]

            val substitute:  TypeProjection = checkNotNull(substitutor.substitute(argument))
            substitution[parameter.getTypeConstructor()] = substitute
            typeParameterMapping?.put(parameter, substitute)
        }
        if (CangJieBuiltIns.isNothing (context)) return
        for (supertype in context.constructor.getSupertypes()) {
            fillInDeepSubstitutor(supertype, substitutor, substitution, typeParameterMapping)
        }
    }
    /**
     * For each supertype of a given type, we map type parameters to type arguments.
     *
     * For instance, we have the following class hierarchy:
     * trait Iterable< T>
     * trait Collection< E>: Iterable<E>
     * trait MyFooCollection<F>: Collection<Foo></Foo><F>>
     *
     * For MyFooCollection<out CharSequence>, the following multimap will be returned:
     * T declared in Iterable -> Foo<out CharSequence>
     * E declared in Collection -> Foo<out CharSequence>
     * F declared in MyFooCollection -> out CharSequence
    </out></out></out></F></F></E></out></out> */
    fun buildDeepSubstitutionMultimap(type: CangJieType): Multimap<TypeParameterDescriptor, TypeProjection> {
        val fullSubstitution: Multimap<TypeParameterDescriptor, TypeProjection> =
            LinkedHashMultimap.create<TypeParameterDescriptor, TypeProjection>()
        val substitution =
            HashMap<TypeConstructor, TypeProjection>()
        val typeSubstitutor: TypeSubstitutor =
            TypeSubstitutor.create(substitution)
        // we use the mutability of the map here
        SubstitutionUtils.fillInDeepSubstitutor(
            type,
            typeSubstitutor,
            substitution,
            fullSubstitution
        )
        return fullSubstitution
    }

}
