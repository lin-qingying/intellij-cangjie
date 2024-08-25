package com.huawei.cangjie.resolve.calls.results

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.StandardNames
import com.huawei.cangjie.builtins.UnsignedTypes
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.CallableMemberDescriptor
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.findClassAcrossModuleDependencies
import com.huawei.cangjie.descriptors.synthetic.SyntheticMemberDescriptor
import com.huawei.cangjie.resolve.DescriptorEquivalenceForOverrides
import com.huawei.cangjie.resolve.OverridingUtil
import com.huawei.cangjie.resolve.calls.context.CheckArgumentTypesMode
import com.huawei.cangjie.resolve.descriptorUtil.isTypeRefinementEnabled
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.model.CangJieTypeMarker
import com.huawei.cangjie.types.model.requireOrDescribe
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.utils.CancellationChecker
import it.unimi.dsi.fastutil.Hash
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet


open class OverloadingConflictResolver<C : Any>(
    private val builtIns: CangJieBuiltIns,
    private val module: ModuleDescriptor,
    private val specificityComparator: TypeSpecificityComparator,
    private val platformOverloadsSpecificityComparator: PlatformOverloadsSpecificityComparator,
    private val cancellationChecker: CancellationChecker,
    private val getResultingDescriptor: (C) -> CallableDescriptor,
    private val createEmptyConstraintSystem: () -> SimpleConstraintSystem,
    private val createFlatSignature: (C) -> FlatSignature<C>,
    private val getVariableCandidates: (C) -> C?, // for variable WithInvoke
    private val isFromSources: (CallableDescriptor) -> Boolean,
    private val hasSAMConversion: ((C) -> Boolean)?,
    private val cangjieTypeRefiner: CangJieTypeRefiner,
) {
    private val C.resultingDescriptor: CallableDescriptor get() = getResultingDescriptor(this)
    private val isTypeRefinementEnabled by lazy { module.isTypeRefinementEnabled() }

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
    // But we cannot compare return types for such copies, because it may lead us to recursive problem (see KT-11995).
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
            // Here we'd like to handle situation when we have two synthetic descriptors as in syntheticSAMExtensions.kt

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
        if (maximallySpecific != null) {
            return setOf(maximallySpecific)
        }

        if (discriminateGenerics) {
            val maximallySpecificGenericsDiscriminated = findMaximallySpecific(noOverrides, checkArgumentsMode, true)
            if (maximallySpecificGenericsDiscriminated != null) {
                return setOf(maximallySpecificGenericsDiscriminated)
            }
        }

        return noOverrides
    }


    // Different smart casts may lead to the same candidate descriptor wrapped into different ResolvedCallImpl objects
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
        if (!createEmptyConstraintSystem().isSignatureNotLessSpecific(
                fSignature,
                gSignature,
                SpecificityComparisonWithNumerics,
                specificityComparator
            )
        ) {
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
        useOriginalSamTypes: Boolean
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

        if (!call1.isExpect && call2.isExpect) return true
        if (call1.isExpect && !call2.isExpect) return false

        if (call1.contextReceiverCount > call2.contextReceiverCount) return true
        if (call1.contextReceiverCount < call2.contextReceiverCount) return false

        return createEmptyConstraintSystem().isSignatureNotLessSpecific(
            call1,
            call2,
            SpecificityComparisonWithNumerics,
            specificityComparator,
            useOriginalSamTypes
        )
    }

    private val SpecificityComparisonWithNumerics = object : SpecificityComparisonCallbacks {
        override fun isNonSubtypeNotLessSpecific(specific: CangJieTypeMarker, general: CangJieTypeMarker): Boolean {
            requireOrDescribe(specific is CangJieType, specific)
            requireOrDescribe(general is CangJieType, general)

            val float64 = builtIns.float64Type
            val float32 = builtIns.float32Type
            val float16 = builtIns.float16Type

            val isSpecificUnsigned = UnsignedTypes.isUnsignedType(specific)
            val isGeneralUnsigned = UnsignedTypes.isUnsignedType(general)
            return when {
                isSpecificUnsigned && isGeneralUnsigned -> {
                    val uInt64 =
                        module.findClassAcrossModuleDependencies(StandardNames.FqNames.uInt64ClassId)?.defaultType
                            ?: return false
                    val uInt32 =
                        module.findClassAcrossModuleDependencies(StandardNames.FqNames.uInt32ClassId)?.defaultType
                            ?: return false
                    val uInt8 =
                        module.findClassAcrossModuleDependencies(StandardNames.FqNames.uInt8ClassId)?.defaultType
                            ?: return false
                    val uInt16 =
                        module.findClassAcrossModuleDependencies(StandardNames.FqNames.uInt16ClassId)?.defaultType
                            ?: return false

                    isNonSubtypeNotLessSpecific(
                        specific,
                        general,
                        float64,
                        float32,
                        float16,
                        uInt64,
                        uInt32,
                        uInt8,
                        uInt16
                    )
                }

                !isSpecificUnsigned && isGeneralUnsigned -> true

                else -> {
                    val Int64 = builtIns.int64Type
                    val Int32 = builtIns.int32Type
                    val Int8 = builtIns.int8Type
                    val Int16 = builtIns.int16Type

                    isNonSubtypeNotLessSpecific(specific, general, float64, float32, float16, Int64, Int32, Int8, Int16)
                }
            }

        }

        private fun isNonSubtypeNotLessSpecific(
            specific: CangJieType,
            general: CangJieType,
            float64: CangJieType,
            float32: CangJieType,
            float16: CangJieType,
            Int64: CangJieType,
            Int32: CangJieType,
            Int8: CangJieType,
            Int16: CangJieType
        ): Boolean {
            when {
                TypeUtils.equalTypes(specific, float64) && TypeUtils.equalTypes(
                    general,
                    float32
                ) && TypeUtils.equalTypes(general, float16) -> return true

                TypeUtils.equalTypes(specific, Int32) -> {
                    when {
                        TypeUtils.equalTypes(general, Int64) -> return true
                        TypeUtils.equalTypes(general, Int8) -> return true
                        TypeUtils.equalTypes(general, Int16) -> return true
                    }
                }

                TypeUtils.equalTypes(specific, Int16) && TypeUtils.equalTypes(general, Int8) -> return true
            }

            return false
        }
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
        }
}
