package com.huawei.cangjie.types

import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.utils.newHashMapWithExpectedSize
import com.huawei.cangjie.utils.newHashSetWithExpectedSize
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
