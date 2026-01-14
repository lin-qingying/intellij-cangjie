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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.CastDiagnosticsUtil
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.TypeReconstructionResult
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.makeNonOption
import org.cangnova.cangjie.types.makeOption
import org.cangnova.cangjie.types.isOptionType
import org.cangnova.cangjie.types.OptionTypeUtils

/**
 * 裸类型类似于原始类型，但在 CangJie 中仅允许在 is/as 操作的右侧使用。
 * 例如：
 *
 * fun foo(a: Any) {
 *   if (a is List) {
 *     // 在这里 a 被认为是 List<Any>
 *   }
 * }
 *
 * 另一个例子：
 *
 * fun foo(a: Collection<String>) {
 *   if (a is List) {
 *     // 在这里 a 被认为是 List<String>
 *   }
 * }
 *
 * 可以调用 reconstruct(supertype) 从裸类型获取实际类型
 */
class PossiblyBareType private constructor(
    // 实际类型，如果为裸类型则可能为 null。
    private val _actualType: CangJieType?,
    // 裸类型的类型构造器，用于类型重建。
    private val _bareTypeConstructor: TypeConstructor?,
    // 标记此类型是否可选。
    private val optional: Boolean
) {
    /**
     * 检查裸类型是否可为空。
     *
     * @return 如果裸类型可为空则返回 true，否则返回 false
     */
    private fun isBareTypeNullable(): Boolean = optional

    val bareTypeConstructor get() = _bareTypeConstructor!!
    val actualType get() = _actualType!!

    /**
     * 检查此类型是否可选。
     *
     * @return 如果类型可选则返回 true，否则返回 false
     */
    fun isOptional(): Boolean {
        if (isBare()) return isBareTypeNullable()
        return actualType.isOptionType()
    }

    /**
     * 从裸类型重建实际类型。
     *
     * @param subjectType 主题类型
     * @return 重建后的类型结果
     */
    fun reconstruct(subjectType: CangJieType): TypeReconstructionResult {
        if (!isBare()) return TypeReconstructionResult(actualType, true)

        // 查找静态已知的子类型
        val reconstructionResult = CastDiagnosticsUtil.findStaticallyKnownSubtype(
            subjectType.makeNonOption(),
            bareTypeConstructor
        )
        val type = reconstructionResult.resultingType
        // 如果类型不存在，直接返回重建结果
        if (type == null) return reconstructionResult

        // 根据是否可空调整结果类型
        val resultingType = OptionTypeUtils.makeOptionalIfNeeded(type, isBareTypeNullable())
        return TypeReconstructionResult(resultingType, reconstructionResult.isAllArgumentsInferred)
    }

    /**
     * 将当前类型标记为可选。
     *
     * @return 新的 PossiblyBareType 实例，标记为可选
     */
    fun makeOptional(): PossiblyBareType {
        if (isBare()) {
            return if (isBareTypeNullable()) this else bare(bareTypeConstructor, true)
        }

        return type(actualType.makeOption())
    }


    /**
     * 检查当前类型是否为裸类型。
     *
     * @return 如果是裸类型则返回 true，否则返回 false
     */
    fun isBare(): Boolean = _actualType == null

    /**
     * 返回类型的字符串表示。
     *
     * @return 类型的可读字符串描述
     */
    override fun toString(): String {
        return if (isBare()) {
            val nullableStr = if (optional) "?" else ""
            "BareType(${_bareTypeConstructor}$nullableStr)"
        } else {
            "Type($_actualType)"
        }
    }

    companion object {
        /**
         * 创建一个裸类型的实例。
         *
         * @param bareTypeConstructor 裸类型的类型构造器
         * @param optional 标记此类型是否可选
         * @return 新的 PossiblyBareType 实例
         */
        fun bare(bareTypeConstructor: TypeConstructor, optional: Boolean): PossiblyBareType =
            PossiblyBareType(null, bareTypeConstructor, optional)

        /**
         * 创建一个具有实际类型的实例。
         *
         * @param actualType 实际类型
         * @return 新的 PossiblyBareType 实例
         */
        fun type(actualType: CangJieType): PossiblyBareType =
            PossiblyBareType(actualType, null, false)
    }
}