/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.calls.results


import it.unimi.dsi.fastutil.Hash
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.synthetic.SyntheticMemberDescriptor
import org.cangnova.cangjie.psi.ValueArgument
import org.cangnova.cangjie.resolve.DescriptorEquivalenceForOverrides
import org.cangnova.cangjie.resolve.OverridingUtil
import org.cangnova.cangjie.resolve.calls.context.CheckArgumentTypesMode
import org.cangnova.cangjie.resolve.calls.inference.ecs.ExistentialConstraintSystem
import org.cangnova.cangjie.resolve.calls.model.DefaultValueArgument
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.model.VariableAsFunctionResolvedCallImpl
import org.cangnova.cangjie.resolve.isTypeRefinementEnabled
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.utils.CancellationChecker

@JvmName("createWithConvertedTypes")
fun <T> FlatSignature.Companion.create(
    origin: T,
    descriptor: CallableDescriptor,
    numDefaults: Int,
    parameterTypes: List<CangJieType?>
): FlatSignature<T> {

    return FlatSignature(
        origin,
        descriptor.typeParameters,
        valueParameterTypes =   parameterTypes,
        hasVarargs = descriptor.valueParameters.any { it.varargElementType != null },
        numDefaults = numDefaults,
        isSyntheticMember = descriptor is SyntheticMemberDescriptor<*>
    )
}


fun <RC : ResolvedCall<*>> RC.createFlatSignature(): FlatSignature<RC> {
    val originalDescriptor = candidateDescriptor.original
    val originalValueParameters = originalDescriptor.valueParameters

    var numDefaults = 0
    val valueArgumentToParameterType = HashMap<ValueArgument, CangJieType>()
    for ((valueParameter, resolvedValueArgument) in valueArguments.entries) {
        if (resolvedValueArgument is DefaultValueArgument) {
            numDefaults++
        } else {
            val originalValueParameter = originalValueParameters[valueParameter.index]
            val parameterType = originalValueParameter.argumentValueType
            for (valueArgument in resolvedValueArgument.arguments) {
                valueArgumentToParameterType[valueArgument] = parameterType
            }
        }
    }

    return FlatSignature.create(
        this,
        originalDescriptor,
        numDefaults,
        call.valueArguments.map { valueArgumentToParameterType[it] })
}

val ValueParameterDescriptor.argumentValueType get() = type

fun createOverloadingConflictResolver(
    builtIns: CangJieBuiltIns,
    module: ModuleDescriptor,
    specificityComparator: TypeSpecificityComparator,
    platformOverloadsSpecificityComparator: PlatformOverloadsSpecificityComparator,
    cancellationChecker: CancellationChecker,
    cangjieTypeRefiner: CangJieTypeRefiner,
) = AbstractOverloadingConflictResolver(
    builtIns,
    module,
    specificityComparator,
    platformOverloadsSpecificityComparator,
    cancellationChecker,
    ResolvedCall<*>::resultingDescriptor,
    ResolvedCall<*>::createFlatSignature,
    { (it as? VariableAsFunctionResolvedCallImpl)?.variableCall },
    { DescriptorToSourceUtils.descriptorToDeclaration(it) != null },
    null,
    cangjieTypeRefiner
)

open class AbstractOverloadingConflictResolver<C : Any>(
    private val builtIns: CangJieBuiltIns,
    private val module: ModuleDescriptor,
    private val specificityComparator: TypeSpecificityComparator,
    private val platformOverloadsSpecificityComparator: PlatformOverloadsSpecificityComparator,
    private val cancellationChecker: CancellationChecker,
    private val getResultingDescriptor: (C) -> CallableDescriptor,
    private val createFlatSignature: (C) -> FlatSignature<C>,
    private val getVariableCandidates: (C) -> C?, // for variable WithInvoke
    private val isFromSources: (CallableDescriptor) -> Boolean,
    private val hasSAMConversion: ((C) -> Boolean)?,
    private val cangjieTypeRefiner: CangJieTypeRefiner,
) {
    private val C.resultingDescriptor: CallableDescriptor get() = getResultingDescriptor(this)
    private val isTypeRefinementEnabled by lazy { module.isTypeRefinementEnabled() }

    /** ECS 实例，用于签名特异性比较 */
    private val ecs: ExistentialConstraintSystem by lazy {
        ExistentialConstraintSystem.create(builtIns, specificityComparator)
    }

    private val resolvedCallHashingStrategy = object : Hash.Strategy<C> {
        override fun equals(call1: C?, call2: C?): Boolean =
            if (call1 != null && call2 != null)
                call1.resultingDescriptor == call2.resultingDescriptor
            else
                call1 == call2

        override fun hashCode(call: C?): Int = call?.resultingDescriptor?.hashCode() ?: 0
    }

    private fun Collection<C>.setIfOneOrEmpty() = when (size) {
        0 -> emptySet()
        1 -> setOf(single())
        else -> null
    }

    private fun newResolvedCallSet(expectedSize: Int): MutableSet<C> =
        ObjectOpenCustomHashSet(expectedSize, resolvedCallHashingStrategy)

    // null means ambiguity between variables
    private fun findMaximallySpecificVariableAsFunctionCalls(candidates: Collection<C>): Set<C>? {
        val variableCalls = candidates.mapTo(newResolvedCallSet(candidates.size)) {
            getVariableCandidates(it) ?: throw AssertionError("Regular call among variable-as-function calls: $it")
        }

        val maxSpecificVariableCalls = chooseMaximallySpecificCandidates(
            variableCalls, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            discriminateGenerics = false
        )

        val maxSpecificVariableCall = maxSpecificVariableCalls.singleOrNull() ?: return null
        return candidates.filterTo(newResolvedCallSet(2)) {
            getVariableCandidates(it)!!.resultingDescriptor == maxSpecificVariableCall.resultingDescriptor
        }
    }

    private val CallableDescriptor.originalIfTypeRefinementEnabled get() = if (isTypeRefinementEnabled) original else this

    // Sometimes we should compare "copies" from sources and from binary files.
    // But we cannot compare return types for such copies, because it may lead us to recursive problem  .
    // Because of this we compare them without return type and choose descriptor from source if we found duplicate.
    fun filterOutEquivalentCalls(candidates: Collection<C>): Set<C> {
        candidates.setIfOneOrEmpty()?.let { return it }

        val fromSourcesGoesFirst = candidates.sortedBy { if (isFromSources(it.resultingDescriptor)) 0 else 1 }

        val result = LinkedHashSet<C>()
        outerLoop@ for (meD in fromSourcesGoesFirst) {
            cancellationChecker.check()
            for (otherD in result) {
                val me = meD.resultingDescriptor.originalIfTypeRefinementEnabled
                val other = otherD.resultingDescriptor.originalIfTypeRefinementEnabled
                val ignoreReturnType = isFromSources(me) != isFromSources(other)
                if (DescriptorEquivalenceForOverrides.areCallableDescriptorsEquivalent(
                        me,
                        other,
                        allowCopiesFromTheSameDeclaration = isTypeRefinementEnabled,
                        ignoreReturnType = ignoreReturnType,
                        cangjieTypeRefiner = cangjieTypeRefiner
                    )
                ) {
                    continue@outerLoop
                }
            }
            result.add(meD)
        }

        return result
    }

    // if result contains only one element -- it is maximally specific; otherwise we have ambiguity
    fun chooseMaximallySpecificCandidates(
        candidates: Collection<C>,
        checkArgumentsMode: CheckArgumentTypesMode,
        discriminateGenerics: Boolean
    ): Set<C> {
        candidates.setIfOneOrEmpty()?.let { return it }

        val fixedCandidates = if (getVariableCandidates(candidates.first()) != null) {
            findMaximallySpecificVariableAsFunctionCalls(candidates) ?: return LinkedHashSet(candidates)
        } else {
            candidates
        }

        val noEquivalentCalls = filterOutEquivalentCalls(fixedCandidates)
        val noOverrides = OverridingUtil.filterOverrides(
            noEquivalentCalls,
            isTypeRefinementEnabled,
            cancellationChecker::check
        ) { a, b ->
            val aDescriptor = a.resultingDescriptor
            val bDescriptor = b.resultingDescriptor
            // Here we'd like to handle situation when we have two synthetic descriptors as in syntheticSAMExtensions.cj

            // Without this, we'll pick all synthetic descriptors as they don't have overridden descriptors and
            // then report ambiguity, which isn't very convenient
            if (aDescriptor is SyntheticMemberDescriptor<*> && bDescriptor is SyntheticMemberDescriptor<*>) {
                val aBaseDescriptor = aDescriptor.baseDescriptorForSynthetic
                val bBaseDescriptor = bDescriptor.baseDescriptorForSynthetic
                if (aBaseDescriptor is CallableMemberDescriptor && bBaseDescriptor is CallableMemberDescriptor) {
                    return@filterOverrides Pair(aBaseDescriptor, bBaseDescriptor)
                }
            }
            Pair(aDescriptor, bDescriptor)
        }
        if (noOverrides.size == 1) {
            return noOverrides
        }

        val maximallySpecific = findMaximallySpecific(noOverrides, checkArgumentsMode, false)
        if (maximallySpecific != null ) {
            return setOf(maximallySpecific)
        }

        if (discriminateGenerics) {
            val maximallySpecificGenericsDiscriminated = findMaximallySpecific(noOverrides, checkArgumentsMode, true)
            if (maximallySpecificGenericsDiscriminated != null  ) {
                return setOf(maximallySpecificGenericsDiscriminated)
            }
        }

        return noOverrides
    }


    // Different smart casts may lead to the same candidate descriptor wrapped into different MutableResolvedCallImpl objects
    private fun uniquifyCandidatesSet(candidates: Collection<C>): Set<C> =
        ObjectOpenCustomHashSet(candidates.size, resolvedCallHashingStrategy).apply { addAll(candidates) }

    private inline fun <C> isDefinitelyMostSpecific(
        candidate: C,
        candidates: Collection<C>,
        isNotLessSpecific: (C, C) -> Boolean
    ): Boolean =
        candidates.all { other ->
            candidate === other ||
                    isNotLessSpecific(candidate, other) && !isNotLessSpecific(other, candidate)
        }

    /**
     * Returns `true` if `f` is definitely not less specific than `g`,
     * `false` if `f` is definitely less specific than `g`,
     * `null` if undecided.
     */
    private fun isNotLessSpecificCallableReferenceDescriptor(f: CallableDescriptor, g: CallableDescriptor): Boolean {
        if (f.valueParameters.size != g.valueParameters.size) return false
//        if (f.varargParameterPosition() != g.varargParameterPosition()) return false

        val fSignature = FlatSignature.createFromCallableDescriptor(f)
        val gSignature = FlatSignature.createFromCallableDescriptor(g)

        // 使用 ECS 进行特异性比较
        if (!ecs.isNotLessSpecific(fSignature, gSignature)) {
            return false
        }

//        if (f is CallableMemberDescriptor && g is CallableMemberDescriptor) {
//            if (!f.isExpect && g.isExpect) return true
//            if (f.isExpect && !g.isExpect) return false
//        }

        if (platformOverloadsSpecificityComparator.isMoreSpecificShape(g, f)) {
            return false
        }

        return true
    }

    private inline fun <C : Any> Collection<C>.exactMaxWith(isNotWorse: (C, C) -> Boolean): C? {
        var result: C? = null
        for (candidate in this) {
            if (result == null || isNotWorse(candidate, result)) {
                result = candidate
            }
        }
        if (result == null) return null
        if (any { it != result && isNotWorse(it, result) }) {
            return null
        }
        return result
    }

    private fun findMaximallySpecificCall(
        candidates: Set<C>,
        discriminateGenerics: Boolean,
        useOriginalSamTypes: Boolean = false
    ): C? {
        val filteredCandidates = uniquifyCandidatesSet(candidates)

        if (filteredCandidates.size <= 1) return filteredCandidates.singleOrNull()

        val conflictingCandidates = filteredCandidates.map { candidateCall ->
            createFlatSignature(candidateCall)
        }

        val bestCandidatesByParameterTypes = conflictingCandidates.filter { candidate ->
            cancellationChecker.check()
            isMostSpecific(candidate, conflictingCandidates) { call1, call2 ->
                isNotLessSpecificCallWithArgumentMapping(call1, call2, discriminateGenerics, useOriginalSamTypes)
            }
        }

        return bestCandidatesByParameterTypes.exactMaxWith { call1, call2 ->
            isOfNotLessSpecificShape(
                call1,
                call2
            )
        }?.origin
    }


    /**
     * Returns `true` if [call1] is definitely more or equally specific [call2],
     * `false` otherwise.
     */
    private fun compareCallsByUsedArguments(
        call1: FlatSignature<C>,
        call2: FlatSignature<C>,
        discriminateGenerics: Boolean,
        @Suppress("UNUSED_PARAMETER") useOriginalSamTypes: Boolean
    ): Boolean {
        if (discriminateGenerics) {
            val isGeneric1 = call1.isGeneric
            val isGeneric2 = call2.isGeneric
            // generic loses to non-generic
            if (isGeneric1 && !isGeneric2) return false
            if (!isGeneric1 && isGeneric2) return true
            // two generics are non-comparable
            if (isGeneric1 && isGeneric2) return false
        }

        // 使用 ECS 进行特异性比较
        return ecs.isNotLessSpecific(call1, call2)
    }

    /**
     * `call1` is not less specific than `call2`
     */
    private fun isNotLessSpecificCallWithArgumentMapping(
        call1: FlatSignature<C>,
        call2: FlatSignature<C>,
        discriminateGenerics: Boolean,
        useOriginalSamTypes: Boolean
    ): Boolean {
        return compareCallsByUsedArguments(
            call1,
            call2,
            discriminateGenerics,
            useOriginalSamTypes
        )
    }

    private fun FlatSignature<C>.candidateDescriptor() =
        origin.resultingDescriptor.original

    private fun isOfNotLessSpecificShape(
        call1: FlatSignature<C>,
        call2: FlatSignature<C>
    ): Boolean {
        val hasVarargs1 = call1.hasVarargs
        val hasVarargs2 = call2.hasVarargs
        if (hasVarargs1 && !hasVarargs2) return false

        if (call1.numDefaults > call2.numDefaults) {
            return false
        }

        if (platformOverloadsSpecificityComparator.isMoreSpecificShape(
                call2.candidateDescriptor(),
                call1.candidateDescriptor()
            )
        ) {
            return false
        }

        return true

    }

    private inline fun <C> isMostSpecific(
        candidate: C,
        candidates: Collection<C>,
        isNotLessSpecific: (C, C) -> Boolean
    ): Boolean =
        candidates.all { other ->
            candidate === other ||
                    isNotLessSpecific(candidate, other)
        }

    private fun isNotLessSpecificCallableReference(f: CallableDescriptor, g: CallableDescriptor): Boolean =
        // TODO should we "discriminate generic descriptors" for callable references?
        isNotLessSpecificCallableReferenceDescriptor(f, g)

    private fun findMaximallySpecific(
        candidates: Set<C>,
        checkArgumentsMode: CheckArgumentTypesMode,
        discriminateGenerics: Boolean
    ): C? =
        if (candidates.size <= 1)
            candidates.firstOrNull()
        else when (checkArgumentsMode) {
            CheckArgumentTypesMode.CHECK_CALLABLE_TYPE ->
                uniquifyCandidatesSet(candidates).singleOrNull {
                    isDefinitelyMostSpecific(it, candidates) { call1, call2 ->
                        isNotLessSpecificCallableReference(call1.resultingDescriptor, call2.resultingDescriptor)
                    }
                }

            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS ->
                // Attempt 1: general disambiguation
                findMaximallySpecificCall(candidates, discriminateGenerics)

                // Attempt 2: disambiguation excluding SAM converted candidates
                    ?: hasSAMConversion?.let { hasConversion ->
                        findMaximallySpecificCall(
                            candidates.filterNotTo(mutableSetOf(), hasConversion),
                            discriminateGenerics
                        )
                    }

                    // Attempt 3: disambiguation excluding synthetic candidates
                    ?: findMaximallySpecificCall(
                        candidates.filterNotTo(mutableSetOf()) { createFlatSignature(it).isSyntheticMember },
                        discriminateGenerics
                    )

                    // Attempt 4: disambiguation on original SAM-types
                    ?: hasSAMConversion?.let { hasConversion ->
                        findMaximallySpecificCall(
                            candidates.filterTo(mutableSetOf(), hasConversion),
                            discriminateGenerics, useOriginalSamTypes = true
                        )
                    }
        }
}
