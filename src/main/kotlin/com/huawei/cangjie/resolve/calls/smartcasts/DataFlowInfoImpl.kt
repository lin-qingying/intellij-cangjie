package com.huawei.cangjie.resolve.calls.smartcasts

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.types.CangJieType
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet

private typealias ImmutableMultimap<K, V> = ImmutableMap<K, ImmutableSet<V>>

internal class DataFlowInfoImpl : DataFlowInfo {
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


}
