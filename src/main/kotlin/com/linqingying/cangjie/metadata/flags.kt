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

package com.linqingying.cangjie.metadata

import com.linqingying.cangjie.lexer.CjModifierKeywordToken
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.metadata.deserialization.Flags

abstract class FlagsToModifiers {
    abstract fun getModifiers(flags: Int): CjModifierKeywordToken?
}
/**
 * 对于接口，我们只需要记住 `sealed` 修饰符，因为接口默认是抽象的。
 */
val INTERFACE_MODALITY: FlagsToModifiers = object : FlagsToModifiers() {
    /**
     * 获取接口的模态修饰符。
     *
     * @param flags 标志位
     * @return 如果模态为 SEALED，则返回 SEALED 关键字，否则返回 null
     */
    override fun getModifiers(flags: Int): CjModifierKeywordToken? {
        val modality = Flags.MODALITY.get(flags)
        return CjTokens.SEALED_KEYWORD.takeIf { modality == ProtoBuf.Modality.SEALED }
    }
}

val MODALITY: FlagsToModifiers = object : FlagsToModifiers() {
    /**
     * 获取类的模态修饰符。
     *
     * @param flags 标志位
     * @return 根据模态返回相应的关键字
     */
    override fun getModifiers(flags: Int): CjModifierKeywordToken {
        val modality = Flags.MODALITY.get(flags)
        return when (modality) {
            ProtoBuf.Modality.ABSTRACT -> CjTokens.ABSTRACT_KEYWORD
            ProtoBuf.Modality.FINAL -> throw IllegalStateException("意外的模态: null")
            ProtoBuf.Modality.OPEN -> CjTokens.OPEN_KEYWORD
            ProtoBuf.Modality.SEALED -> CjTokens.SEALED_KEYWORD
            null -> throw IllegalStateException("意外的模态: null")
        }
    }
}

val VISIBILITY: FlagsToModifiers = object : FlagsToModifiers() {
    /**
     * 获取类的可见性修饰符。
     *
     * @param flags 标志位
     * @return 根据可见性返回相应的关键字
     */
    override fun getModifiers(flags: Int): CjModifierKeywordToken? {
        return when (val visibility = Flags.VISIBILITY.get(flags)) {
            ProtoBuf.Visibility.PRIVATE, ProtoBuf.Visibility.PRIVATE_TO_THIS -> CjTokens.PRIVATE_KEYWORD
            ProtoBuf.Visibility.INTERNAL -> CjTokens.INTERNAL_KEYWORD
            ProtoBuf.Visibility.PROTECTED -> CjTokens.PROTECTED_KEYWORD
            ProtoBuf.Visibility.PUBLIC -> CjTokens.PUBLIC_KEYWORD
            else -> throw IllegalStateException("意外的可见性: $visibility")
        }
    }
}

val OPERATOR = createBooleanFlagToModifier(Flags.IS_OPERATOR, CjTokens.OPERATOR_KEYWORD)

/**
 * 创建一个布尔标志到修饰符的映射。
 *
 * @param flagField 布尔标志字段
 * @param cjModifierKeywordToken 修饰符关键字
 * @return FlagsToModifiers 实例
 */
private fun createBooleanFlagToModifier(
    flagField: Flags.BooleanFlagField,
    cjModifierKeywordToken: CjModifierKeywordToken
): FlagsToModifiers = BooleanFlagToModifier(flagField, cjModifierKeywordToken)

/**
 * 布尔标志到修饰符的映射类。
 *
 * @param flagField 布尔标志字段
 * @param cjModifierKeywordToken 修饰符关键字
 */
private class BooleanFlagToModifier(
    private val flagField: Flags.BooleanFlagField,
    private val cjModifierKeywordToken: CjModifierKeywordToken
) : FlagsToModifiers() {
    /**
     * 根据布尔标志获取修饰符。
     *
     * @param flags 标志位
     * @return 如果标志位为 true，则返回修饰符关键字，否则返回 null
     */
    override fun getModifiers(flags: Int): CjModifierKeywordToken? = if (flagField.get(flags)) cjModifierKeywordToken else null
}
