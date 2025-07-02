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

package cn.cangnova.cangjie.resolve.calls.smartcasts

import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.utils.ImmutableMap
import cn.cangnova.cangjie.utils.ImmutableSet

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
    fun establishSubtyping(value: DataFlowValue, type: CangJieType, languageVersionSettings: LanguageVersionSettings): DataFlowInfo

    /**
     * Call this function to choose data flow information common for this and other and return it as the result
     */
    infix fun or(other: DataFlowInfo): DataFlowInfo

    /**
     * Call this function to clear all data flow information about
     * the given data flow value. Useful when we are not sure how this value can be changed, e.g. in a loop.
     */
    fun clearValueInfo(value: DataFlowValue, languageVersionSettings: LanguageVersionSettings): DataFlowInfo

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
