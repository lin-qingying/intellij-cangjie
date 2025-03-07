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

package cn.cangnova.cangjie.types

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.descriptors.SupertypeLoopChecker
import cn.cangnova.cangjie.descriptors.TypeParameterDescriptor
import cn.cangnova.cangjie.storage.StorageManager
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.types.checker.refineTypes

abstract class AbstractTypeConstructor(storageManager: StorageManager) : ClassifierBasedTypeConstructor() {
    override fun getSupertypes() = supertypes().supertypesWithoutCycles

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor =
        ModuleViewTypeConstructor(cangjieTypeRefiner)

    override fun getExtendSupertypes(extendId: String?): Collection<CangJieType> = computeExtendSuperTypes(extendId)

    @TypeRefinement
    private inner class ModuleViewTypeConstructor(
        private val cangjieTypeRefiner: CangJieTypeRefiner
    ) : TypeConstructor {
        /* NB: it is important to use PUBLICATION here instead of 'storageManager.createLazyValue { ... }'

        The reason is that 'storageManager' can be a storage manager from DefaultBuiltIns (e.g. is this type constructor
        is type constructor of some built-in class like 'Int'). Therefore, call to refined supertypes would result in
        the following order of acquiring locks: DefaultBuiltIns lock -> Sources lock

        Obviously, a lot of code acquires locks in different order (sources lock first, then built-ins lock), so that would
        result in deadlock
         */
        private val refinedSupertypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
            @OptIn(TypeRefinement::class)
            cangjieTypeRefiner.refineTypes(this@AbstractTypeConstructor.getSupertypes())
        }

        override fun getParameters(): List<TypeParameterDescriptor> = this@AbstractTypeConstructor.parameters

        //
        override fun getSupertypes(): List<CangJieType> = refinedSupertypes

        //
        override fun isFinal(): Boolean = this@AbstractTypeConstructor.isFinal
        override fun isDenotable(): Boolean = this@AbstractTypeConstructor.isDenotable

        override fun getDeclarationDescriptor() = this@AbstractTypeConstructor.declarationDescriptor

        override fun getBuiltIns(): CangJieBuiltIns = this@AbstractTypeConstructor.builtIns

        override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor =
            this@AbstractTypeConstructor.refine(cangjieTypeRefiner)

        override fun equals(other: Any?) = this@AbstractTypeConstructor.equals(other)
        override fun hashCode() = this@AbstractTypeConstructor.hashCode()
        override fun toString() = this@AbstractTypeConstructor.toString()
    }

    // In current version diagnostic about loops in supertypes is reported on each vertex (supertype reference) that lies on the cycle.
    // To achieve that we store both versions of supertypes --- before and after loops disconnection.
    // The first one is used for computation of neighbours in supertypes graph (see Companion.computeNeighbours)
    private class Supertypes(val allSupertypes: Collection<CangJieType>) {
        // initializer is only needed as a stub for case when 'getSupertypes' is called while 'supertypes' are being calculated
        var supertypesWithoutCycles: List<CangJieType> = listOf(ErrorUtils.errorTypeForLoopInSupertypes)
    }

    private val supertypes = storageManager.createLazyValueWithPostCompute(
        { Supertypes(computeSupertypes()) },
        {
            Supertypes(listOf(ErrorUtils.errorTypeForLoopInSupertypes))
        },
        { supertypes ->
            // It's important that loops disconnection begins in post-compute phase, because it guarantees that
            // when we start calculation supertypes of supertypes (for computing neighbours), they start their disconnection loop process
            // either, and as we want to report diagnostic about loops on all declarations they should see consistent version of 'allSupertypes'
            var resultWithoutCycles =
                supertypeLoopChecker.findLoopsInSupertypesAndDisconnect(
                    this, supertypes.allSupertypes,
                    { it.computeNeighbours(useCompanions = false) },
                    { reportSupertypeLoopError(it) }
                )

            if (resultWithoutCycles.isEmpty()) {
                resultWithoutCycles = defaultSupertypeIfEmpty()?.let { listOf(it) }.orEmpty()
            }

            // We also check if there are a loop with additional edges going from owner of companion to
            // the companion itself.
            // Note that we use already disconnected types to not report two diagnostics on cyclic supertypes
            if (shouldReportCyclicScopeWithCompanionWarning) {
                supertypeLoopChecker.findLoopsInSupertypesAndDisconnect(
                    this, resultWithoutCycles,
                    { it.computeNeighbours(useCompanions = true) },
                    { reportScopesLoopError(it) }
                )
            }

            supertypes.supertypesWithoutCycles =
                processSupertypesWithoutCycles(
                    resultWithoutCycles as? List<CangJieType> ?: resultWithoutCycles.toList()
                )
        })

    private fun TypeConstructor.computeNeighbours(useCompanions: Boolean): Collection<CangJieType> =
        (this as? AbstractTypeConstructor)?.let { abstractClassifierDescriptor ->
            abstractClassifierDescriptor.supertypes().allSupertypes +
                    abstractClassifierDescriptor.getAdditionalNeighboursInSupertypeGraph(useCompanions)
        } ?: supertypes

    protected abstract fun computeSupertypes(): Collection<CangJieType>
    protected abstract fun computeExtendSuperTypes(extendId: String?): Collection<CangJieType>
    protected abstract val supertypeLoopChecker: SupertypeLoopChecker
    protected open fun reportSupertypeLoopError(type: CangJieType) {}

    protected open fun processSupertypesWithoutCycles(supertypes: List<@JvmSuppressWildcards CangJieType>): List<CangJieType> =
        supertypes

    // TODO: overload in AbstractTypeParameterDescriptor?
    protected open fun reportScopesLoopError(type: CangJieType) {}
    protected open val shouldReportCyclicScopeWithCompanionWarning: Boolean = false

    protected open fun getAdditionalNeighboursInSupertypeGraph(useCompanions: Boolean): Collection<CangJieType> =
        emptyList()

    protected open fun defaultSupertypeIfEmpty(): CangJieType? = null

    // Only for debugging
    fun renderAdditionalDebugInformation(): String = "supertypes=${supertypes.renderDebugInformation()}"

}
