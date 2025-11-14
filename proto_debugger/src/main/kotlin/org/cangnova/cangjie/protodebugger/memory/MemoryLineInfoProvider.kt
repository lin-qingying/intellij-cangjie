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

package org.cangnova.cangjie.protodebugger.memory

import com.intellij.openapi.editor.Document

/**
 * 内存行信息提供者接口
 *
 * 提供内存文档中行号与地址的映射关系。
 * 支持行号到地址和地址到行号的双向转换。
 */
interface MemoryLineInfoProvider {
    /**
     * 获取文档中指定行号对应的内存地址
     *
     * @param lineNumber 文档中的行号（从0开始）
     * @return 对应的内存地址，如果没有则返回null
     */
    fun getAddressForLineNumber(lineNumber: Int): Address?

    /**
     * 获取内存地址在文档中的行号
     *
     * @param address 内存地址
     * @return 对应的行号，如果没有则返回null
     */
    fun getLineNumberForAddress(address: Address): Int?

    /**
     * 获取内存地址在文档中的偏移量
     *
     * @param address 内存地址
     * @return 对应的偏移量，如果没有则返回null
     */
    fun getOffsetForAddress(address: Address): Int?

    /**
     * 获取指定行号在文档中的地址范围
     *
     * @param lineNumber 文档中的行号（从0开始）
     * @return 对应的地址范围，如果没有则返回null
     */
    fun getLineRange(lineNumber: Int): AddressRange?

    /**
     * 获取内存地址在文档中的范围
     *
     * @param address 内存地址
     * @return 对应的文档范围，如果没有则返回null
     */
    fun getDocumentRange(address: Address): com.intellij.openapi.util.TextRange?

    /**
     * 获取内存地址在文档中的行范围
     *
     * @param address 内存地址
     * @return 对应的行号范围，如果没有则返回null
     */
    fun getLineRangeInDocument(address: Address): IntRange?
}