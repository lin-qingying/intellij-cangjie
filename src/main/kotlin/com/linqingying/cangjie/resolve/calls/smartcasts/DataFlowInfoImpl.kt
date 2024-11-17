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

package com.linqingying.cangjie.resolve.calls.smartcasts

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.SetMultimap
import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.builtins.isFunctionType
import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.DefinitelyNotNullType
import com.linqingying.cangjie.types.checker.NewCapturedTypeConstructor
import com.linqingying.cangjie.types.isDefinitelyNotNullType
import com.linqingying.cangjie.types.isFlexible
import com.linqingying.cangjie.types.util.TypeUtils
import com.linqingying.cangjie.types.util.contains
import com.linqingying.cangjie.types.util.isSubtypeOf
import com.linqingying.cangjie.types.util.makeNotNullable
import com.linqingying.cangjie.utils.*
import javaslang.Tuple2
import java.util.LinkedHashSet

private typealias ImmutableMultimap<K, V> = ImmutableMap<K, ImmutableSet<V>>


private fun <K, V> ImmutableMultimap<K, V>.put(key: K, value: V): ImmutableMultimap<K, V> {
    val oldSet = this[key].getOrElse(ImmutableLinkedHashSet.empty<V>())
    if (oldSet.contains(value)) return this

    return put(key, oldSet.add(value))
}
internal class DataFlowInfoImpl(

    override val completeNullabilityInfo: ImmutableMap<DataFlowValue, Nullability>,
    override val completeTypeInfo: ImmutableMultimap<DataFlowValue, CangJieType>
) : DataFlowInfo {

    constructor() : this(EMPTY_NULLABILITY_INFO, EMPTY_TYPE_INFO)


    override fun getCollectedTypes(key: DataFlowValue, languageVersionSettings: LanguageVersionSettings) =
        getCollectedTypes(key, true, languageVersionSettings)

    override fun disequate(
        a: DataFlowValue, b: DataFlowValue, languageVersionSettings: LanguageVersionSettings
    ): DataFlowInfo = equateOrDisequate(a, b, languageVersionSettings, identityEquals = false, isEquate = false)
    private fun putNullabilityAndTypeInfo(
        map: MutableMap<DataFlowValue, Nullability>,
        value: DataFlowValue,
        nullability: Nullability,
        languageVersionSettings: LanguageVersionSettings,
        newTypeInfoBuilder: SetMultimap<DataFlowValue, CangJieType>? = null,
        // XXX: set to false only as a workaround for OI,   for details (in NI everything works automagically)
        recordUnstable: Boolean = true
    ) {
        if (value.isStable || recordUnstable) {
            map[value] = nullability
        }

        val identifierInfo = value.identifierInfo
        if (!nullability.canBeNull() && languageVersionSettings.supportsFeature(LanguageFeature.SafeCallBoundSmartCasts)) {
            when (identifierInfo) {
                is IdentifierInfo.Qualified -> {
                    val receiverType = identifierInfo.receiverType
                    if (identifierInfo.safe && receiverType != null) {
                        val receiverValue = DataFlowValue(identifierInfo.receiverInfo, receiverType)
                        putNullabilityAndTypeInfo(
                            map, receiverValue, nullability,
                            languageVersionSettings, newTypeInfoBuilder, recordUnstable = recordUnstable
                        )
                    }
                }
                is IdentifierInfo.SafeCast -> {
                    val targetType = identifierInfo.targetType
                    val subjectType = identifierInfo.subjectType
                    if (targetType != null && subjectType != null &&
                        languageVersionSettings.supportsFeature(LanguageFeature.SafeCastCheckBoundSmartCasts)) {

                        val subjectValue = DataFlowValue(identifierInfo.subjectInfo, subjectType)
                        putNullabilityAndTypeInfo(
                            map, subjectValue, nullability,
                            languageVersionSettings, newTypeInfoBuilder, recordUnstable = false
                        )
                        if (subjectValue.isStable) {
                            newTypeInfoBuilder?.put(subjectValue, targetType)
                        }
                    }
                }
                is IdentifierInfo.Variable -> identifierInfo.bound?.let {
                    putNullabilityAndTypeInfo(
                        map, it, nullability,
                        languageVersionSettings, newTypeInfoBuilder, recordUnstable = recordUnstable
                    )
                }
            }
        }
    }

    private fun equateOrDisequate(
        a: DataFlowValue,
        b: DataFlowValue,
        languageVersionSettings: LanguageVersionSettings,
        identityEquals: Boolean,
        isEquate: Boolean
    ): DataFlowInfo {
        val resultNullabilityInfo = hashMapOf<DataFlowValue, Nullability>()
        val newTypeInfoBuilder = newTypeInfoBuilder()

        val nullabilityOfA = getStableNullability(a)
        val nullabilityOfB = getStableNullability(b)
        val newANullability = nullabilityOfA.refine(if (isEquate) nullabilityOfB else nullabilityOfB.invert())
        val newBNullability = nullabilityOfB.refine(if (isEquate) nullabilityOfA else nullabilityOfA.invert())

        putNullabilityAndTypeInfo(
            resultNullabilityInfo,
            a,
            newANullability,
            languageVersionSettings,
            newTypeInfoBuilder
        )

        putNullabilityAndTypeInfo(
            resultNullabilityInfo,
            b,
            newBNullability,
            languageVersionSettings,
            newTypeInfoBuilder
        )

        var changed = getCollectedNullability(a) != newANullability || getCollectedNullability(b) != newBNullability

        // NB: == has no guarantees of type equality,
        if (isEquate && (identityEquals || !nullabilityOfA.canBeNonNull() || !nullabilityOfB.canBeNonNull())) {
            newTypeInfoBuilder.putAll(a, getStableTypes(b, false, languageVersionSettings))
            newTypeInfoBuilder.putAll(b, getStableTypes(a, false, languageVersionSettings))
            if (a.type != b.type) {
                // To avoid recording base types of own type
                if (!a.type.isSubtypeOf(b.type)) {
                    newTypeInfoBuilder.put(a, b.type)
                }
                if (!b.type.isSubtypeOf(a.type)) {
                    newTypeInfoBuilder.put(b, a.type)
                }
            }
            changed = changed or !newTypeInfoBuilder.isEmpty
        }

        return if (changed) create(this, resultNullabilityInfo, newTypeInfoBuilder) else this
    }
    override fun getStableTypes(key: DataFlowValue, languageVersionSettings: LanguageVersionSettings) =
        getStableTypes(key, true, languageVersionSettings)

    private fun getStableTypes(key: DataFlowValue, enrichWithNotNull: Boolean, languageVersionSettings: LanguageVersionSettings) =
        if (!key.isStable) LinkedHashSet() else getCollectedTypes(key, enrichWithNotNull, languageVersionSettings)

    override fun getStableNullability(key: DataFlowValue): Nullability = getNullability(key, true)
    override fun getCollectedNullability(key: DataFlowValue) = getNullability(key, false)

    private fun getNullability(key: DataFlowValue, stableOnly: Boolean): Nullability =
        if (stableOnly && !key.isStable) {
            key.immanentNullability
        } else {
            completeNullabilityInfo[key].getOrElse(key.immanentNullability)
        }

    override fun getCollectedTypes(key: DataFlowValue/*, languageVersionSettings: LanguageVersionSettings*/) =
        getCollectedTypes(key, true/*, languageVersionSettings*/)


    private fun getCollectedTypes(
        key: DataFlowValue,
        enrichWithNotNull: Boolean,
        languageVersionSettings: LanguageVersionSettings? = null
    ): Set<CangJieType> {
        return emptySet()
//        val types = completeTypeInfo[key].getOrElse(ImmutableLinkedHashSet.empty())
//        if (!enrichWithNotNull || getCollectedNullability(key).canBeNull()) {
//            return types.toJavaSet()
//        }
//
//        val enrichedTypes = newLinkedHashSetWithExpectedSize<CangJieType>(types.size() + 1)
//        val originalType = key.type
//        types.mapTo(enrichedTypes) { type -> type.makeReallyNotNullIfNeeded(languageVersionSettings) }
//        if (originalType.canBeDefinitelyNotNullOrNotNull(languageVersionSettings)) {
//            enrichedTypes.add(originalType.makeReallyNotNullIfNeeded(languageVersionSettings))
//        }
//
//        return enrichedTypes
    }

    override fun equate(
        a: DataFlowValue,
        b: DataFlowValue,
        identityEquals: Boolean,
        languageVersionSettings: LanguageVersionSettings
    ): DataFlowInfo= equateOrDisequate(a, b, languageVersionSettings, identityEquals, isEquate = true)
    override fun establishSubtyping(
        value: DataFlowValue, type: CangJieType, languageVersionSettings: LanguageVersionSettings
    ): DataFlowInfo {
        if (value.type == type) return this
        if (getCollectedTypes(value, languageVersionSettings).contains(type)) return this
        if (!value.type.isFlexible() && value.type.isSubtypeOf(type)) return this

        val nullabilityInfo = hashMapOf<DataFlowValue, Nullability>()

        val isTypeNotNull =
            if (languageVersionSettings.supportsFeature(LanguageFeature.NewInference))
                !TypeUtils.isNullableType(type)
            else
                !type.isFunctionType

        if (isTypeNotNull) {
            putNullabilityAndTypeInfo(nullabilityInfo, value, Nullability.NOT_NULL, languageVersionSettings)
        }

        return create(
            this,
            nullabilityInfo,
            listOf(Tuple2(value, listOf(type)))
        )
    }

    override fun or(other: DataFlowInfo): DataFlowInfo {
        if (other === DataFlowInfo.EMPTY) return DataFlowInfo.EMPTY
        if (this === DataFlowInfo.EMPTY) return DataFlowInfo.EMPTY
        if (this === other) return this

        assert(other is DataFlowInfoImpl) { "Unknown DataFlowInfo type: " + other }

        val resultNullabilityInfo = hashMapOf<DataFlowValue, Nullability>()
        for ((key, otherFlags) in other.completeNullabilityInfo) {
            val thisFlags = getCollectedNullability(key)
            resultNullabilityInfo.put(key, thisFlags.or(otherFlags))
        }

        val myTypeInfo = completeTypeInfo
        val otherTypeInfo = other.completeTypeInfo
        val newTypeInfoBuilder = newTypeInfoBuilder()

        for (key in myTypeInfo.keySet()) {
            if (key in otherTypeInfo.keySet()) {
                newTypeInfoBuilder.putAll(
                    key,
                    myTypeInfo[key].getOrNull().intersectConsideringNothing(otherTypeInfo[key].getOrNull())
                        ?: ImmutableLinkedHashSet.empty()
                )
            }
        }
        return create(null, resultNullabilityInfo, newTypeInfoBuilder)
    }

    override fun clearValueInfo(value: DataFlowValue, languageVersionSettings: LanguageVersionSettings): DataFlowInfo {
        val resultNullabilityInfo = hashMapOf<DataFlowValue, Nullability>()
        putNullabilityAndTypeInfo(resultNullabilityInfo, value, value.immanentNullability, languageVersionSettings)
        return create(this, resultNullabilityInfo, EMPTY_TYPE_INFO, value)

    }

    private fun ImmutableSet<CangJieType>?.containsNothing() = this?.any { CangJieBuiltIns.isNothing(it) } ?: false

    private fun ImmutableSet<CangJieType>?.intersectConsideringNothing(other: ImmutableSet<CangJieType>?) =
        when {
            other.containsNothing() -> this
            this.containsNothing() -> other
            else -> this.intersect(other)
        }
    private fun ImmutableSet<CangJieType>?.intersect(other: ImmutableSet<CangJieType>?): ImmutableSet<CangJieType> =
        when {
            this == null -> other ?: ImmutableLinkedHashSet.empty()
            other == null -> this
            else -> {
                // Here we cover the case when "this" has T?!! type and "other" has T
                val thisApproximated = approximateDefinitelyNotNullableTypes(this)
                val otherApproximated = approximateDefinitelyNotNullableTypes(other)
                if (thisApproximated == null && otherApproximated == null ||
                    thisApproximated != null && otherApproximated != null
                ) {
                    this.intersect(other)
                } else {
                    (thisApproximated ?: this).intersect(otherApproximated ?: other)
                }
            }
        }
    private fun approximateDefinitelyNotNullableTypes(set: ImmutableSet<CangJieType>): ImmutableSet<CangJieType>? {
        if (!set.any { it.isDefinitelyNotNullType }) return null
        return set.map { if (it is DefinitelyNotNullType) it.original.makeNotNullable() else it }
    }
    override fun and(other: DataFlowInfo): DataFlowInfo {
        if (other === DataFlowInfo.EMPTY) return this
        if (this === DataFlowInfo.EMPTY) return other
        if (this === other) return this
        return create()
//
//        assert(other is DataFlowInfoImpl) { "Unknown DataFlowInfo type: " + other }
//
//        val resultNullabilityInfo = hashMapOf<DataFlowValue, Nullability>()
//        for ((key, otherFlags) in other.completeNullabilityInfo) {
//            val thisFlags = getCollectedNullability(key)
//            val flags = thisFlags.and(otherFlags)
//            if (flags != thisFlags) {
//                resultNullabilityInfo.put(key, flags)
//            }
//        }
//
//        val otherTypeInfo = other.completeTypeInfo
//
//        return create(this, resultNullabilityInfo, otherTypeInfo)
    }

    override fun assign(a: DataFlowValue, b: DataFlowValue): DataFlowInfo {
//        val nullabilityOfB = getStableNullability(b)
//        val nullabilityUpdate = mapOf(a to nullabilityOfB)
//
//        var typesForB = getStableTypes(b)
//        // Own type of B must be recorded separately, e.g. for a constant
//        // But if its type is the same as A, there is no reason to do it
//        // because own type is not saved in this set
//        // Error types are also not saved
//        if (!b.type.isError && a.type != b.type) {
//            typesForB += b.type
//        }

        return create()
    }

    private fun create(): DataFlowInfo {


        return DataFlowInfoImpl()

    }

    override fun toString() = if (completeTypeInfo.isEmpty && completeNullabilityInfo.isEmpty()) "EMPTY" else "Non-trivial DataFlowInfo"

    companion object {
        private val EMPTY_NULLABILITY_INFO: ImmutableMap<DataFlowValue, Nullability> =
            ImmutableHashMap.empty()

        private val EMPTY_TYPE_INFO: ImmutableMultimap<DataFlowValue, CangJieType> =
            ImmutableHashMap.empty()

        private fun newTypeInfoBuilder(): SetMultimap<DataFlowValue, CangJieType> =
            LinkedHashMultimap.create()

        private fun create(
            parent: DataFlowInfo?,
            updatedNullabilityInfo: Map<DataFlowValue, Nullability>,
            updatedTypeInfo: SetMultimap<DataFlowValue, CangJieType>
        ): DataFlowInfo =
            create(
                parent,
                updatedNullabilityInfo,
                updatedTypeInfo.asMap().entries.map { Tuple2(it.key, it.value) }
            )

        private fun create(
            parent: DataFlowInfo?,
            updatedNullabilityInfo: Map<DataFlowValue, Nullability>,
            // NB: typeInfo must be mutable here!
            updatedTypeInfo: Iterable<Tuple2<DataFlowValue, out Iterable<CangJieType>>>,
            valueToClearPreviousTypeInfo: DataFlowValue? = null
        ): DataFlowInfo {
            if (updatedNullabilityInfo.isEmpty() && updatedTypeInfo.none() && valueToClearPreviousTypeInfo == null) {
                return parent ?: DataFlowInfo.EMPTY
            }

            val resultingNullabilityInfo =
                updatedNullabilityInfo.entries.fold(
                    parent?.completeNullabilityInfo ?: EMPTY_NULLABILITY_INFO
                ) { result, (dataFlowValue, nullability) ->
                    if (dataFlowValue.immanentNullability != nullability)
                        result.put(dataFlowValue, nullability)
                    else
                        result.remove(dataFlowValue)
                }

            var resultingTypeInfo = parent?.completeTypeInfo ?: EMPTY_TYPE_INFO

            valueToClearPreviousTypeInfo?.let {
                resultingTypeInfo = resultingTypeInfo.remove(it)
            }

            for ((value, types) in updatedTypeInfo) {
                for (type in types) {
                    if (value.type == type || type.contains { it.constructor is NewCapturedTypeConstructor }) continue
                    resultingTypeInfo = resultingTypeInfo.put(value, type)
                }
            }

            if (resultingNullabilityInfo.isEmpty && resultingTypeInfo.isEmpty) return DataFlowInfo.EMPTY
            if (resultingNullabilityInfo === parent?.completeNullabilityInfo && resultingTypeInfo === parent.completeTypeInfo) {
                return parent
            }

            return DataFlowInfoImpl(resultingNullabilityInfo, resultingTypeInfo)
        }
    }
}


