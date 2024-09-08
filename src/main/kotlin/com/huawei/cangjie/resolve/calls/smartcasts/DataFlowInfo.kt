package com.huawei.cangjie.resolve.calls.smartcasts

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.utils.ImmutableMap
import com.huawei.cangjie.utils.ImmutableSet

/**
 *此接口用于提供和编辑有关值为空和可能类型的信息。
 *数据流信息是不可变的，因此函数永远不会更改它。
 */
interface DataFlowInfo {


    val completeNullabilityInfo: ImmutableMap<DataFlowValue, Nullability>

    val completeTypeInfo: ImmutableMap<DataFlowValue, ImmutableSet<CangJieType>>
    /**
     * Call this function when it's known than a == b.
     */
    fun equate(a: DataFlowValue, b: DataFlowValue, identityEquals: Boolean, languageVersionSettings: LanguageVersionSettings): DataFlowInfo

    /**
     * Call this function to choose data flow information common for this and other and return it as the result
     */
    fun or(other: DataFlowInfo): DataFlowInfo
    /**
     * Call this function to add data flow information from other to this and return sum as the result
     */
    fun and(other: DataFlowInfo): DataFlowInfo
    /**
     * Returns possible types for the given value, NOT taking its stability into account.
     *
     * IMPORTANT: by default, the original (native) type for this value
     * are NOT included. So it's quite possible to get an empty set here.
     * Also, type order in the result set MAKES SENSE so keep it stable and do not change without reason
     */
    fun getCollectedTypes(key: DataFlowValue
//                          , languageVersionSettings: LanguageVersionSettings
    ): Set<CangJieType>
    fun getCollectedTypes(key: DataFlowValue
                         , languageVersionSettings: LanguageVersionSettings
    ): Set<CangJieType>
    /**
     * Call this function when it's known than a != b
     */
    fun disequate(a: DataFlowValue, b: DataFlowValue, languageVersionSettings: LanguageVersionSettings): DataFlowInfo

    /**
     * Returns possible types for the given value if it's stable.
     * Otherwise, basic value type is returned.
     *
     * IMPORTANT: by default, the original (native) type for this value
     * are NOT included. So it's quite possible to get an empty set here.
     * Also, type order in the result set MAKES SENSE so keep it stable and do not change without reason
     */
    fun getStableTypes(key: DataFlowValue, languageVersionSettings: LanguageVersionSettings): Set<CangJieType>

    /**
     * Returns collected nullability for the given value if it's stable.
     * Otherwise basic value nullability is returned
     */
    fun getStableNullability(key: DataFlowValue): Nullability
    /**
     * Returns collected nullability for the given value, NOT taking its stability into account.
     */
    fun getCollectedNullability(key: DataFlowValue): Nullability

    /**
     * Call this function when b is assigned to a
     */
    fun assign(a: DataFlowValue, b: DataFlowValue/*, languageVersionSettings: LanguageVersionSettings*/): DataFlowInfo

    companion object {
        val EMPTY = DataFlowInfoFactory.EMPTY
    }
}

//object DataFlowInfoFactory {
//    @JvmField
//    val EMPTY: DataFlowInfo = DataFlowInfoImpl()
//}
//

object DataFlowInfoFactory {
    @JvmField
    val EMPTY: DataFlowInfo = DataFlowInfoImpl()
}
