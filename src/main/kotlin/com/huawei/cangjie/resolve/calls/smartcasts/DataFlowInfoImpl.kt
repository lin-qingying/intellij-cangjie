package com.huawei.cangjie.resolve.calls.smartcasts

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.SetMultimap
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.checker.NewCapturedTypeConstructor
import com.huawei.cangjie.types.util.contains
import com.huawei.cangjie.utils.*
import javaslang.Tuple2
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


