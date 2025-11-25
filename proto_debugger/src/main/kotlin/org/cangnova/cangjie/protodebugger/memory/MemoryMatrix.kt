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

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListMap

/**
 * 稀疏内存矩阵 存储具体数据
 *
 * 使用跳表实现的高效稀疏存储结构，支持快速的地址查询和范围操作。
 *
 * 核心特性：
 * - O(log n) 的插入、查询和删除
 * - O(log n) 的范围查询
 * - O(1) 的行/列快速定位（通过索引）
 * - 支持矩阵操作（转置、切片、投影）
 * - 线程安全
 * - 泛型支持：可以存储特定类型的 MemoryCell
 *
 * 设计理念：
 * - 稀疏存储：只存储有数据的单元格
 * - 快速查询：使用跳表和索引双重加速
 * - 类型安全：通过泛型约束存储的 Cell 类型
 *
 * @param T 存储的单元格类型，必须是 MemoryCell 的子类
 */
class MemoryMatrix<T : MemoryCell> {
    // 跳表：主要存储结构，按地址排序
    private val skipList = ConcurrentSkipListMap<Address, T>()

    // 行索引：快速定位某一行的所有地址
    // 行号 = (地址 / 16) % 65536，用于分页显示
    private val rowIndex = ConcurrentHashMap<Int, MutableSet<Address>>()

    /**
     * 设置单元格
     *
     * O(log n) 插入操作
     *
     * @param cell 要设置的单元格
     */
    fun setCell(cell: T) {
        skipList[cell.address] = cell
        updateIndices(cell.address)
    }

    /**
     * 批量设置单元格
     *
     * @param cells 单元格列表
     */
    fun setCells(cells: List<T>) {
        cells.forEach { setCell(it) }
    }
    /**
     * 获取第一个单元格
     */
    fun getFristCell():T? = skipList.firstEntry()?.value
    /**
     * 获取最后一个单元格
     */
    fun getLastCell(): T? = skipList.lastEntry()?.value
    /**
     * 获取单元格
     *
     * O(log n) 查询操作
     *
     * @param address 地址
     * @return 单元格，如果不存在返回null
     */
    fun getCell(address: Address): T? = skipList[address]

    /**
     * 获取地址范围内的所有单元格
     *
     * O(log n + k) 范围查询，k为结果数量
     *
     * @param range 地址范围
     * @return 范围内的单元格列表（按地址排序）
     */
    fun getCellsInRange(range: AddressRange): List<T> {
        if (range.isEmpty()) return emptyList()

        return skipList.subMap(
            range.start, true,
            range.endInclusive, true
        ).values.toList()
    }

    /**
     * 获取某一行的所有单元格
     *
     * O(1) 获取行索引 + O(k) 查询单元格
     *
     * @param rowNumber 行号
     * @return 该行的所有单元格
     */
    fun getRow(rowNumber: Int): List<T> {
        val addresses = rowIndex[rowNumber] ?: return emptyList()
        return addresses.mapNotNull { skipList[it] }
    }

    /**
     * 删除单元格
     *
     * @param address 地址
     * @return 被删除的单元格，如果不存在返回null
     */
    fun removeCell(address: Address): T? {
        val removed = skipList.remove(address)
        if (removed != null) {
            removeFromIndices(address)
        }
        return removed
    }

    /**
     * 删除地址范围内的所有单元格
     *
     * @param range 地址范围
     * @return 被删除的单元格数量
     */
    fun removeCellsInRange(range: AddressRange): Int {
        if (range.isEmpty()) return 0

        val toRemove = skipList.subMap(
            range.start, true,
            range.endInclusive, true
        ).keys.toList()

        toRemove.forEach { address ->
            skipList.remove(address)
            removeFromIndices(address)
        }

        return toRemove.size
    }

    /**
     * 清空矩阵
     */
    fun clear() {
        skipList.clear()
        rowIndex.clear()
    }

    /**
     * 获取矩阵中单元格的总数
     */
    fun size(): Int = skipList.size

    /**
     * 检查矩阵是否为空
     */
    fun isEmpty(): Boolean = skipList.isEmpty()

    /**
     * 获取矩阵覆盖的地址范围
     *
     * @return 地址范围，如果矩阵为空返回EMPTY
     */
    fun getAddressRange(): AddressRange {
        if (skipList.isEmpty()) return AddressRange.EMPTY
        return skipList.firstKey().rangeTo(skipList.lastKey())
    }

    /**
     * 创建矩阵的副本
     *
     * @return 新的矩阵实例
     */
    fun copy(): MemoryMatrix<T> {
        val newMatrix = MemoryMatrix<T>()
        skipList.forEach { (_, cell) ->
            newMatrix.setCell(cell)
        }
        return newMatrix
    }



    // ==================== 私有辅助方法 ====================

    /**
     * 更新索引
     */
    private fun updateIndices(address: Address) {
        val rowNumber = calculateRowNumber(address)
        rowIndex.getOrPut(rowNumber) { ConcurrentHashMap.newKeySet() }.add(address)
    }

    /**
     * 从索引中移除
     */
    private fun removeFromIndices(address: Address) {
        val rowNumber = calculateRowNumber(address)
        rowIndex[rowNumber]?.remove(address)
        if (rowIndex[rowNumber]?.isEmpty() == true) {
            rowIndex.remove(rowNumber)
        }
    }

    /**
     * 计算行号
     *
     * 使用地址的高位部分计算行号，每行16字节
     */
    private fun calculateRowNumber(address: Address): Int {
        return ((address.value shr 4) and 0xFFFFUL).toInt()
    }

    /**
     * 矩阵统计信息
     */
    data class MatrixStats(
        val totalCells: Int,
        val dataCells: Int,
        val instructionCells: Int,
        val placeholderCells: Int,
        val loadedCells: Int,
        val modifiedCells: Int,
        val addressRange: AddressRange,
        val rowCount: Int
    )
}

/**
 * 压缩矩阵
 *
 * 使用CSR（Compressed Sparse Row）格式存储矩阵数据，
 * 适合用于缓存和持久化。
 */
data class CompressedMatrix(
    val values: List<Byte>,
    val rowIndices: List<Int>,
    val columnPointers: List<Int>
) {
    /**
     * 解压缩为完整矩阵
     */
    fun decompress(): MemoryMatrix<MemoryCell.DataCell> {
        val matrix = MemoryMatrix<MemoryCell.DataCell>()

        values.forEachIndexed { index, value ->
            val row = rowIndices[index]
            val col = columnPointers[index]
            val address = reconstructAddress(row, col)

            matrix.setCell(
                MemoryCell.DataCell(
                    address = address,
                    value = value,
                    version = 0,
                    state = CellState.LOADED
                )
            )
        }

        return matrix
    }

    /**
     * 从行列索引重建地址
     */
    private fun reconstructAddress(row: Int, col: Int): Address {
        val addressValue = (row.toLong() shl 4) or col.toLong()
        return Address.Companion.Factory.fromLong(addressValue)
    }
}