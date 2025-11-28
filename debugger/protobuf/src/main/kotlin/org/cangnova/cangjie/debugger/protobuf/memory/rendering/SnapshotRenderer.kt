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

package org.cangnova.cangjie.debugger.protobuf.memory.rendering

import org.cangnova.cangjie.debugger.protobuf.memory.Address
import org.cangnova.cangjie.debugger.protobuf.memory.AddressRange
import org.cangnova.cangjie.debugger.protobuf.memory.MemoryCell
import org.cangnova.cangjie.debugger.protobuf.memory.state.MemoryViewState

/**
 * 渲染快照
 *
 * 简化的不可变快照，只存储核心数据用于差异计算。
 *
 * 简化说明：
 * - 移除了 viewport（已废弃）
 * - 移除了 selection（已废弃）
 * - 移除了 highlights（已废弃）
 * - 只保留单元格列表和时间戳
 */
data class RenderSnapshot(
    val cells: List<MemoryCell>,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 单元格的快速查找映射
     */
    private val cellMap: Map<Address, MemoryCell> by lazy {
        cells.associateBy { it.address }
    }

    /**
     * 获取指定地址的单元格
     */
    fun getCell(address: Address): MemoryCell? = cellMap[address]

    /**
     * 获取地址范围
     */
    fun getAddressRange(): AddressRange {
        if (cells.isEmpty()) return AddressRange.EMPTY
        val minAddr = cells.minOf { it.address.value }
        val maxAddr = cells.maxOf { it.address.value }
        return Address(minAddr).rangeTo(Address(maxAddr))
    }
}

/**
 * 单元格差异
 */
data class CellDiff(
    val added: List<MemoryCell>,
    val removed: List<MemoryCell>,
    val modified: List<Pair<MemoryCell, MemoryCell>> // (old, new)
) {
    /**
     * 是否有任何变化
     */
    fun hasChanges(): Boolean = added.isNotEmpty() || removed.isNotEmpty() || modified.isNotEmpty()
}

/**
 * 快照渲染器
 *
 * 简化版本的渲染器，专注于核心功能：
 * - 跟踪Matrix中单元格的变化
 * - 计算差异以支持增量更新
 * - 提供渲染统计信息
 *
 * 设计原则：
 * - 不再管理document的直接更新（交给上层MemoryEditor处理）
 * - 只负责差异计算和快照管理
 * - 简化后更易于与IntelliJ的Editor系统集成
 *
 * 核心理念：
 * - 每次渲染创建新快照
 * - 计算与上次快照的差异
 * - 返回差异信息供上层使用
 */
class SnapshotRenderer {
    private var lastSnapshot: RenderSnapshot? = null

    /**
     * 创建快照并计算差异
     *
     * @param state 内存视图状态
     * @return 差异信息，如果是初次渲染则返回null
     */
    fun render(state: MemoryViewState<*>): CellDiff? {
        val newSnapshot = createSnapshot(state)

        val diff = if (lastSnapshot == null) {
            // 初始渲染：没有差异，返回null表示需要全量渲染
            null
        } else {
            // 增量渲染：计算差异
            calculateDiff(lastSnapshot!!, newSnapshot)
        }

        lastSnapshot = newSnapshot
        return diff
    }

    /**
     * 从状态创建快照
     *
     * 简化实现：直接使用Matrix中的所有单元格
     */
    private fun createSnapshot(state: MemoryViewState<*>): RenderSnapshot {
        // 获取Matrix中的所有单元格
        val allCells = state.matrix.getAddressRange().let { range ->
            if (range.isEmpty()) {
                emptyList()
            } else {
                state.matrix.getCellsInRange(range)
            }
        }

        return RenderSnapshot(
            cells = allCells,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * 计算差异
     */
    private fun calculateDiff(old: RenderSnapshot, new: RenderSnapshot): CellDiff {
        return diffCells(old.cells, new.cells)
    }

    /**
     * 差异计算：找出新增、删除、修改的单元格
     */
    private fun diffCells(oldCells: List<MemoryCell>, newCells: List<MemoryCell>): CellDiff {
        val oldMap = oldCells.associateBy { it.address }
        val newMap = newCells.associateBy { it.address }

        val added = mutableListOf<MemoryCell>()
        val removed = mutableListOf<MemoryCell>()
        val modified = mutableListOf<Pair<MemoryCell, MemoryCell>>()

        // 查找新增和修改的单元格
        newMap.forEach { (address, newCell) ->
            val oldCell = oldMap[address]
            when {
                oldCell == null -> added.add(newCell)
                oldCell != newCell -> modified.add(oldCell to newCell)
            }
        }

        // 查找删除的单元格
        oldMap.forEach { (address, oldCell) ->
            if (!newMap.containsKey(address)) {
                removed.add(oldCell)
            }
        }

        return CellDiff(added, removed, modified)
    }

    /**
     * 清除快照
     */
    fun clear() {
        lastSnapshot = null
    }

    /**
     * 获取当前快照
     */
    fun getCurrentSnapshot(): RenderSnapshot? = lastSnapshot

    /**
     * 获取统计信息
     */
    fun getStats(): RendererStats {
        return RendererStats(
            lastSnapshotTime = lastSnapshot?.timestamp ?: 0,
            lastSnapshotCells = lastSnapshot?.cells?.size ?: 0,
            addressRange = lastSnapshot?.getAddressRange() ?: AddressRange.EMPTY
        )
    }

    /**
     * 渲染统计信息
     */
    data class RendererStats(
        val lastSnapshotTime: Long,
        val lastSnapshotCells: Int,
        val addressRange: AddressRange
    )
}