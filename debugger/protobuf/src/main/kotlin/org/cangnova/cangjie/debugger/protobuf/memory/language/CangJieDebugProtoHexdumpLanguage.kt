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

package org.cangnova.cangjie.debugger.protobuf.memory.language

import com.intellij.lang.Language

/**
 * Hexdump语言定义
 *
 * 定义十六进制转储视图的语言特性，使其能够享受IDE的语言支持：
 * - 语法高亮
 * - 代码折叠
 * - 代码结构视图
 * - 查找/替换
 *
 * Hexdump格式示例：
 * ```
 * 0000000000001000: 48 89 E5 48 83 EC 20 48  89 7D F8 89 75 F4 8B 45  |H..H.. H.}..u..E|
 * 0000000000001010: F4 0F AF 45 F8 89 45 FC  8B 45 FC 48 83 C4 20 5D  |...E..E..E.H.. ]|
 * ```
 *
 * 语法元素：
 * - ADDRESS: 地址（如 0000000000001000）
 * - COLON: 冒号分隔符
 * - HEX_BYTE: 十六进制字节（如 48）
 * - SEPARATOR: 空格分隔符
 * - ASCII_PIPE: ASCII边界（|）
 * - ASCII_CHAR: ASCII字符
 * - COMMENT: 注释（以 ; 开头）
 */
object CangJieDebugProtoHexdumpLanguage : Language("CangJieDebugProtoHexdump") {


    override fun isCaseSensitive(): Boolean = false

    override fun getDisplayName(): String = "Hexdump"
}