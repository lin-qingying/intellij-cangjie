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

package org.cangnova.cangjie.protodebugger.memory.language

import com.intellij.psi.tree.IElementType

/**
 * Hexdump Token类型
 *
 * 定义Hexdump语言中的所有词法单元（Token）。
 */
class HexdumpTokenType(debugName: String) : IElementType(debugName, HexdumpLanguage.INSTANCE) {
    override fun toString(): String = "HexdumpToken." + super.toString()
}

/**
 * Hexdump Element类型
 *
 * 定义Hexdump语言中的AST节点类型。
 */
class HexdumpElementType(debugName: String) : IElementType(debugName, HexdumpLanguage.INSTANCE) {
    override fun toString(): String = "HexdumpElement." + super.toString()
}

/**
 * Token类型常量
 */
object HexdumpTokenTypes {
    // 地址
    @JvmField
    val ADDRESS = HexdumpTokenType("ADDRESS")

    // 冒号
    @JvmField
    val COLON = HexdumpTokenType("COLON")

    // 十六进制字节
    @JvmField
    val HEX_BYTE = HexdumpTokenType("HEX_BYTE")

    // 空格分隔符
    @JvmField
    val WHITESPACE = HexdumpTokenType("WHITESPACE")

    // ASCII边界
    @JvmField
    val ASCII_PIPE = HexdumpTokenType("ASCII_PIPE")

    // ASCII字符
    @JvmField
    val ASCII_CHAR = HexdumpTokenType("ASCII_CHAR")

    // 占位符
    @JvmField
    val PLACEHOLDER = HexdumpTokenType("PLACEHOLDER")

    // 注释
    @JvmField
    val COMMENT = HexdumpTokenType("COMMENT")

    // 换行
    @JvmField
    val NEWLINE = HexdumpTokenType("NEWLINE")

    // 错误
    @JvmField
    val BAD_CHARACTER = HexdumpTokenType("BAD_CHARACTER")
}

/**
 * Element类型常量
 */
object HexdumpElementTypes {
    // 文件根节点
    @JvmField
    val FILE = HexdumpElementType("FILE")

    // 行
    @JvmField
    val LINE = HexdumpElementType("LINE")

    // 地址部分
    @JvmField
    val ADDRESS_PART = HexdumpElementType("ADDRESS_PART")

    // 十六进制部分
    @JvmField
    val HEX_PART = HexdumpElementType("HEX_PART")

    // ASCII部分
    @JvmField
    val ASCII_PART = HexdumpElementType("ASCII_PART")
}