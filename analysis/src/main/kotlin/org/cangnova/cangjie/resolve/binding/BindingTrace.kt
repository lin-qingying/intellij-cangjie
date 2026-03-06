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

package org.cangnova.cangjie.resolve.binding

import org.cangnova.cangjie.diagnostics.DiagnosticSink
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.resolve.binding.slicedMap.ReadOnlySlice
import org.cangnova.cangjie.resolve.binding.slicedMap.WritableSlice
import org.cangnova.cangjie.types.CangJieType

/**
 * 绑定追踪接口，继承自 [DiagnosticSink]。
 *
 * 负责记录语义分析过程中的绑定信息（如表达式类型、键值映射等），
 * 并提供对 [BindingContext] 的访问能力。
 */
interface BindingTrace : DiagnosticSink {

    /** 当前绑定追踪对应的只读绑定上下文 */
    val bindingContext: BindingContext

    /**
     * 获取某个可写切片中所有已记录的键集合。
     *
     * @param slice 目标可写切片
     * @return 该切片中所有键的集合
     */
    fun <K : Any, V : Any> getKeys(slice: WritableSlice<K, V>): Collection<K>

    /**
     * 获取指定表达式的类型。
     *
     * 注意：表达式的类型应从 EXPRESSION_TYPE_INFO 切片中读取，
     * 而非直接从其他来源获取。
     *
     * @param expression 要查询类型的表达式
     * @return 该表达式的仓颉类型，若未记录则返回 null
     */
    fun getType(expression: CjExpression): CangJieType?

    /**
     * 向指定切片中记录一条键值映射。
     *
     * @param slice 目标可写切片
     * @param key   映射的键
     * @param value 映射的值
     */
    fun <K : Any, V : Any> record(slice: WritableSlice<K, V>, key: K, value: V)

    /**
     * 向布尔类型的切片中记录键，将其值置为 true。
     *
     * @param slice 目标布尔可写切片
     * @param key   要记录的键
     */
    fun <K : Any> record(slice: WritableSlice<K, Boolean>, key: K)

    /**
     * 记录指定表达式的类型信息。
     *
     * 注意：类型应写入 EXPRESSION_TYPE_INFO 切片，
     * 可以是更新已有记录，也可以是新增记录。
     *
     * @param expression 要记录类型的表达式
     * @param type       对应的仓颉类型，允许为 null
     */
    fun recordType(expression: CjExpression, type: CangJieType?)

    /**
     * 从指定只读切片中查询键对应的值。
     *
     * @param slice 目标只读切片
     * @param key   要查询的键
     * @return 对应的值，若不存在则返回 null
     */
    operator fun <K : Any, V : Any> get(slice: ReadOnlySlice<K, V>, key: K): V?

    /** 当前已记录的绑定条目总数，默认为 0 */
    val size: Int get() = 0
}