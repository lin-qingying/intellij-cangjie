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

import com.linqingying.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.types.util.TypeUtils
import com.linqingying.cangjie.utils.newHashMapWithExpectedSize
import com.linqingying.cangjie.utils.newHashSetWithExpectedSize
import java.util.function.Predicate

object TypeUnifier {


    interface UnificationResult {
        val isSuccess: Boolean

        val substitution: Map<TypeConstructor, TypeProjection>
    }

    private
    class UnificationResultImpl : UnificationResult {
        override var isSuccess: Boolean = true
            private set
        override val substitution: MutableMap<TypeConstructor, TypeProjection> =
            newHashMapWithExpectedSize(1)
        private val failedVariables: MutableSet<TypeConstructor> =
            newHashSetWithExpectedSize(0)

        fun fail() {
            isSuccess = false
        }


        fun put(key: TypeConstructor, value: TypeProjection) {
            if (failedVariables.contains(key)) return

            val oldValue: TypeProjection? = substitution.put(key, value)
            if (oldValue != null && oldValue != value) {
                substitution.remove(key)
                failedVariables.add(key)
                fail()
            }
        }
    }

    /**
     * Finds a substitution S that turns `projectWithVariables` to `knownProjection`.
     *
     * Example:
     * known = List<String>
     * withVariables = List<X>
     * variables = {X}
     *
     * result = X -> String
     *
     * Only types accepted by `isVariable` are considered variables.
    </X></String> */
    fun unify(
        knownProjection: TypeProjection,
        projectWithVariables: TypeProjection,
        isVariable: Predicate<TypeConstructor>
    ): UnificationResult {
        val result: UnificationResultImpl =
            UnificationResultImpl()
        doUnify(knownProjection, projectWithVariables, isVariable, result)
        return result
    }

    private fun doUnify(
        knownProjection: TypeProjection,
        projectWithVariables: TypeProjection,
        isVariable: Predicate<TypeConstructor>,
        result: UnificationResultImpl
    ) {
        val known: CangJieType = knownProjection.getType()
        val withVariables: CangJieType = projectWithVariables.getType()

        // in Foo ~ in X  =>  Foo ~ X
        val knownProjectionKind: Variance = knownProjection.getProjectionKind()
        val withVariablesProjectionKind: Variance = projectWithVariables.getProjectionKind()
        if (knownProjectionKind == withVariablesProjectionKind && knownProjectionKind != Variance.INVARIANT) {
            doUnify(
                TypeProjectionImpl(known),
                TypeProjectionImpl(withVariables),
                isVariable,
                result
            )
            return
        }

        // Foo? ~ X?  =>  Foo ~ X
        if (known.isMarkedOption && withVariables.isMarkedOption) {
            doUnify(
                TypeProjectionImpl(
                    knownProjectionKind,
                    TypeUtils.makeNotNullable(known)
                ),
                TypeProjectionImpl(
                    withVariablesProjectionKind,
                    TypeUtils.makeNotNullable(withVariables)
                ),
                isVariable,
                result
            )
            return
        }

        // in Foo ~ out X  => fail
        // in Foo ~ X  =>  may be OK
        if (knownProjectionKind != withVariablesProjectionKind && withVariablesProjectionKind != Variance.INVARIANT) {
            result.fail()
            return
        }

        // Foo ~ X? => fail
        if (!known.isMarkedOption && withVariables.isMarkedOption) {
            result.fail()
            return
        }

        // Foo ~ X  =>  x |-> Foo
        // * ~ X => x |-> *
        val maybeVariable: TypeConstructor = withVariables.constructor
        if (isVariable.test(maybeVariable)) {
            result.put(maybeVariable, knownProjection)
            return
        }

        // Foo? ~ Foo || in Foo ~ Foo || Foo ~ Bar
        val structuralMismatch =
            known.isMarkedOption != withVariables.isMarkedOption || knownProjectionKind != withVariablesProjectionKind || known.constructor != withVariables.constructor
        if (structuralMismatch) {
            result.fail()
            return
        }

        // Foo<A> ~ Foo<B, C>
        if (known.arguments.size != withVariables.arguments.size) {
            result.fail()
            return
        }

        // Foo ~ Foo
        if (known.arguments.isEmpty()) {
            return
        }

        // Foo<...> ~ Foo<...>
        val knownArguments: List<TypeProjection> = known.arguments
        val withVariablesArguments: List<TypeProjection> = withVariables.arguments
        for (i in knownArguments.indices) {
            val knownArg: TypeProjection = knownArguments[i]
            val withVariablesArg: TypeProjection = withVariablesArguments[i]

            doUnify(knownArg, withVariablesArg, isVariable, result)
        }
    }
}


