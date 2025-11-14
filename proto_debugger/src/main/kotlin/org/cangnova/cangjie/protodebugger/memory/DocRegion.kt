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
import com.intellij.openapi.util.TextRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 文档区域接口
 *
 * 表示内存文档中的一个区域，包含地址范围和文本范围的映射关系。
 * 扩展了AddressSpace.Region，提供文档区域的额外功能。
 */
interface DocRegion : AddressSpace.Region {
    /**
     * 文本范围
     */
    val textRange: TextRange

    /**
     * 读取地址范围内的数据
     *
     * @param range 要读取的地址范围
     * @return 读取的数据
     */
    fun read(range: AddressRange): String

    // Additional methods needed by MemoryDoc
    fun getAddressForLineNumberInDocument(lineNumber: Int): Address? = null
    fun getLineNumberInDocument(address: Address): Int? = null
    fun getOffsetInDocument(address: Address): Int? = null
    fun getLineRangeInDocument(): IntRange? = null
    fun markOutdated() {}
    fun deleteFromDocument() {}
}

/**
 * 文档区域基类
 *
 * 提供DocRegion的基本实现，包含地址范围和文本范围的映射关系。
 *
 * @param addressRange 地址范围
 * @param textRange 文本范围
 */
abstract class BaseDocRegion(
    override val range: AddressRange,
    override val textRange: TextRange
) : DocRegion

/**
 * 加载中的文档区域
 *
 * 表示正在加载数据的文档区域。
 *
 * @param addressRange 地址范围
 * @param textRange 文本范围
 * @param document 关联的文档
 * @param coroutineScope 协程作用域
 * @param load 加载数据的挂起函数
 */
class LoadingDocRegion<T>(
    addressRange: AddressRange,
    textRange: TextRange,
    val document: Document,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    load: suspend () -> DocRegion
) : BaseDocRegion(addressRange, textRange) {

    // Alternative constructor for MemoryDoc usage
    constructor(
        doc: Document,
        textRange: TextRange,
        dataRegion: MemoryData.DataRegion.Loading<T>
    ) : this(
        dataRegion.range,
        textRange,
        doc,
        kotlinx.coroutines.CoroutineScope(Dispatchers.Unconfined),
        { throw NotImplementedError("Load function not implemented") }
    )
    private val loadJob = coroutineScope.launch {
        try {
            val loadedRegion = load()
            updateDocument(loadedRegion)
        } catch (e: Exception) {
            // 处理加载错误
        }
    }

    private fun updateDocument(region: DocRegion) {
        // 更新文档内容
        document.replaceString(textRange.startOffset, textRange.endOffset, region.read(range))
    }

    override fun read(range: AddressRange): String {
        return "Loading..."
    }

    /**
     * 等待加载完成
     */
    suspend fun awaitUnallocation() {
        loadJob.join()
    }
}

/**
 * 错误文档区域
 *
 * 表示加载失败或出现错误的文档区域。
 *
 * @param addressRange 地址范围
 * @param textRange 文本范围
 * @param document 关联的文档
 * @param error 错误信息
 */
class ErrorDocRegion(
    addressRange: AddressRange,
    textRange: TextRange,
    val document: Document,
    val error: Throwable
) : BaseDocRegion(addressRange, textRange) {

    // Alternative constructor for MemoryDoc usage
    constructor(
        document: Document,
        textRange: TextRange,
        dataRegion: MemoryData.DataRegion.Completed.LoadError<*>
    ) : this(
        dataRegion.range,
        textRange,
        document,
        dataRegion.exception
    )

    override fun read(range: AddressRange): String {
        return "Error loading memory data: ${error.message}"
    }
}

/**
 * 文档区域监听器
 *
 * 监听文档区域的变化事件。
 */
interface DocRegionListener<T> {
    /**
     * 当文档区域添加时调用
     *
     * @param region 添加的文档区域
     */
    fun regionAdded(region: DocRegion)

    /**
     * 当文档区域移除时调用
     *
     * @param region 移除的文档区域
     */
    fun regionRemoved(region: DocRegion)

    /**
     * 当文档区域更新时调用
     *
     * @param oldRegion 更新前的文档区域
     * @param newRegion 更新后的文档区域
     */
    fun regionUpdated(oldRegion: DocRegion, newRegion: DocRegion)
}

/**
 * 文档区域工厂
 *
 * 用于创建不同类型的文档区域。
 */
interface DocRegionFactory<T> {
    /**
     * 创建文档区域
     *
     * @param addressRange 地址范围
     * @param data 数据
     * @param document 关联的文档
     * @return 创建的文档区域
     */
    fun createDocRegion(
        addressRange: AddressRange,
        data: T,
        document: Document
    ): DocRegion
}

/**
 * 内存文档访问权限
 *
 * 定义内存文档的访问权限和范围。
 */
enum class MemoryDocAccess {
    /**
     * 只读访问
     */
    READ_ONLY,

    /**
     * 读写访问
     */
    READ_WRITE
}

/**
 * 内存文档访问助手类
 *
 * 提供文档的读写操作支持
 */
class MemoryDocAccessHelper<T>(
    private val memoryDoc: MemoryDoc<T>,
    val accessType: MemoryDocAccess
) {
    inline fun <R> read(block: () -> R): R {
        return block()
    }

    inline fun <R> edit(block: () -> R): R {
        if (accessType == MemoryDocAccess.READ_ONLY) {
            throw UnsupportedOperationException("Cannot edit document in READ_ONLY mode")
        }
        return block()
    }
}