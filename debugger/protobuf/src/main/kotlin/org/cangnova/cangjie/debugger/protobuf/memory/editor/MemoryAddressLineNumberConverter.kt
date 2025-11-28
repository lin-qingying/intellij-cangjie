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

package org.cangnova.cangjie.debugger.protobuf.memory.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LineNumberConverter
import org.cangnova.cangjie.debugger.protobuf.memory.Address
import org.cangnova.cangjie.debugger.protobuf.memory.state.MemoryStore

/**
 * 内存地址行号转换器
 *
 * 将行号转换为内存地址显示在 gutter 中。
 */
class MemoryAddressLineNumberConverter(
    private val store: MemoryStore<*>
) : LineNumberConverter {

    /**
     * 将行号转换为地址字符串（在 gutter 中显示）
     *
     * @param editor 编辑器
     * @param lineNumber 行号（从1开始）
     * @return 格式化的内存地址字符串
     */
    override fun convertLineNumberToString(editor: Editor, lineNumber: Int): String? {
        // 行号是从1开始的，需要转换为0开始的索引
        val lineIndex = lineNumber - 1

        // 获取该行对应的内存地址
        val address = store.getAddressForLine(lineIndex)

        // 如果找不到对应的地址，返回 null
        if (address == null) {
            return null
        }

        // 格式化地址为十六进制字符串
        return String.format("%016X", address.value.toLong())
    }

    /**
     * 获取最大行号字符串（用于计算 gutter 宽度）
     *
     * @param editor 编辑器
     * @return 最大地址的字符串表示
     */
    override fun getMaxLineNumberString(editor: Editor): String {
        // 64位地址的最大值
        return Address.MAX_VALUE.toString()
    }

    /**
     * 将行号转换为整数（用于内部计算）
     *
     * @param editor 编辑器
     * @param lineNumber 行号
     * @return 转换后的整数（对于内存视图始终返回0）
     */
    override fun convert(editor: Editor, lineNumber: Int): Int {
        return 0
    }

    /**
     * 获取最大行号
     *
     * @param editor 编辑器
     * @return 最大行号（对于内存视图始终返回0）
     */
    override fun getMaxLineNumber(editor: Editor): Int {
        return 0
    }
}