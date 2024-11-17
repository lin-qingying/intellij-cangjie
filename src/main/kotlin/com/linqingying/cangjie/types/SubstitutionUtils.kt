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

package com.linqingying.cangjie.types

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor

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
        fillInDeepSubstitutor(
            type,
            typeSubstitutor,
            substitution,
            fullSubstitution
        )
        return fullSubstitution
    }

}
