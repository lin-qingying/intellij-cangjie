/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.calls.smartcasts

import io.vavr.collection.HashMap
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.utils.ImmutableMap
import org.cangnova.cangjie.utils.ImmutableSet

/**
 * 数据流信息接口
 *
 * ⚠️ 注意: 仓颉语言的数据流分析与 Kotlin 有本质区别
 *
 * ## 仓颉语言特性
 *
 * - **无运行时 null**: 仓颉没有 null 值,只有 Option<T> 枚举
 * - **类型细化**: 通过模式匹配进行类型细化,不是运行时检查
 * - **稳定性追踪**: 追踪变量是否可能被意外修改(闭包捕获、可变属性等)
 *
 * ## 主要用途
 *
 * 1. **类型推导**: 追踪表达式的可能类型
 * 2. **稳定性分析**: 判断值是否稳定(影响模式匹配的安全性)
 * 3. **类型细化**: 支持 match 表达式的类型细化
 *
 * 数据流信息是不可变的,所有函数返回新的 DataFlowInfo 实例。
 *
 * @see DataFlowValue
 * @see OptionStatus
 */
interface DataFlowInfo {

    /**
     * 完整的 Option 状态信息 (推荐使用)
     *
     * 记录每个值的 Option 类型状态:
     * - DEFINITE: 非 Option 类型
     * - OPTION: Option<T> 类型
     * - UNKNOWN: 未确定
     */
    val completeOptionStatusInfo: ImmutableMap<DataFlowValue, OptionStatus>



    /** 完整的类型信息 */
    val completeTypeInfo: ImmutableMap<DataFlowValue, ImmutableSet<CangJieType>>

    /**
     * Call this function when it's known than a == b.
     */
    fun equate(
        a: DataFlowValue,
        b: DataFlowValue,
        identityEquals: Boolean,
        languageVersionSettings: LanguageVersionSettings
    ): DataFlowInfo

    fun establishSubtyping(
        value: DataFlowValue,
        type: CangJieType,
        languageVersionSettings: LanguageVersionSettings
    ): DataFlowInfo

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
    fun getCollectedTypes(
        key: DataFlowValue
//                          , languageVersionSettings: LanguageVersionSettings
    ): Set<CangJieType>

    fun getCollectedTypes(
        key: DataFlowValue, languageVersionSettings: LanguageVersionSettings
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
     * 获取稳定值的 Option 状态 (推荐使用)
     *
     * 如果值是稳定的,返回收集到的 Option 状态。
     * 否则返回基本的 Option 状态(从类型推导)。
     *
     * @param key 数据流值
     * @return Option 状态
     */
    fun getStableOptionStatus(key: DataFlowValue): OptionStatus


    /**
     * 获取收集到的 Option 状态 (推荐使用)
     *
     * 返回收集到的 Option 状态,不考虑值的稳定性。
     *
     * @param key 数据流值
     * @return Option 状态
     */
    fun getCollectedOptionStatus(key: DataFlowValue): OptionStatus

    /**
     * Call this function when b is assigned to a
     */
    fun assign(a: DataFlowValue, b: DataFlowValue/*, languageVersionSettings: LanguageVersionSettings*/): DataFlowInfo

    companion object {
        val EMPTY = DataFlowInfoFactory.EMPTY
    }
}


object DataFlowInfoFactory {
    
    val EMPTY: DataFlowInfo = DataFlowInfoImpl()
}
