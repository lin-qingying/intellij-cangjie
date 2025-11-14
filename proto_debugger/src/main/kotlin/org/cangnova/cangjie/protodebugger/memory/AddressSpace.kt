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

/**
 * 地址空间接口
 *
 * 该接口定义了地址空间的基本操作，用于管理内存地址范围内的区域。
 * 地址空间是一个将地址映射到数据区域的容器。
 *
 * 使用场景：
 * - 调试器中的内存管理
 * - 内存视图的数据缓存
 * - 内存区域的分配和释放
 *
 * 主要功能：
 * - 地址到数据区域的映射
 * - 区域的查询和迭代
 * - 地址范围的重叠检查
 */
interface AddressSpace<T : AddressSpace.Region> {
    /**
     * 地址空间区域接口
     *
     * 所有存储在地址空间中的数据区域都应该实现此接口。
     */
    interface Region : Interval {
        /**
         * 区域的数据范围
         */
        override val range: AddressRange
    }

    /**
     * 获取指定地址的数据区域
     *
     * @param address 要查询的地址
     * @return 包含该地址的数据区域，如果没有则返回null
     */
    fun getRegion(address: Address): T?

    /**
     * 获取与指定范围重叠的所有数据区域
     *
     * @param range 要查询的地址范围
     * @return 与指定范围重叠的数据区域集合
     */
    fun getRegions(range: AddressRange): Collection<T>

    /**
     * 检查地址空间是否为空
     *
     * @return 如果地址空间为空返回true，否则返回false
     */
    fun isEmpty(): Boolean

    /**
     * 获取地址空间中的所有区域
     *
     * @return 所有数据区域的集合
     */
    fun getAllRegions(): Collection<T>
}

/**
 * 可变地址空间接口
 *
 * 该接口扩展了AddressSpace，添加了修改地址空间内容的方法。
 * 支持区域的分配、重新分配和释放操作。
 *
 * 使用场景：
 * - 动态内存管理
 * - 缓存的数据更新
 * - 内存区域的动态调整
 *
 * 主要功能：
 * - 新区域的分配
 * - 现有区域的重新分配
     * - 区域的释放和清理
 */
interface MutableAddressSpace<T : AddressSpace.Region> : AddressSpace<T> {
    /**
     * 在指定地址分配新区域
     *
     * 如果该地址已有区域，则返回现有区域。
     * 如果没有，则使用提供的工厂函数创建新区域。
     *
     * @param address 要分配区域的地址
     * @param factory 用于创建新区域的工厂函数
     * @return 分配的区域
     */
    fun getOrAllocate(address: Address, factory: (AddressRange) -> T): T

    /**
     * 重新分配区域
     *
     * 用于更新现有区域的数据或范围。
     *
     * @param region 要重新分配的区域
     */
    fun reallocate(region: T)

    /**
     * 释放指定范围的区域
     *
     * @param range 要释放的地址范围
     * @return 被释放的区域集合
     */
    fun unallocate(range: AddressRange): UnallocatedRegions<T>

    /**
     * 释放所有区域
     */
    fun clear()
}

/**
 * 释放区域的结果
 *
 * 包含被释放的区域和剩余的空闲间隔。
 *
 * @param intervals 被释放的区域集合
 */
data class UnallocatedRegions<T : AddressSpace.Region>(
    val intervals: Collection<T>
)