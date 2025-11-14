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
 * 区间接口
 *
 * 该接口定义了一个区间的基本行为，包含起始和结束地址。
 * 所有区间类型都应该实现此接口。
 *
 * 使用场景：
 * - 内存地址范围的表示
 * - 数据区域的边界定义
 * - 范围计算和比较操作
 *
 * 主要功能：
 * - 提供区间的起始和结束地址
 * - 支持区间的大小计算
 * - 提供区间的基本操作接口
 */
interface Interval {
    val range: AddressRange

    /**
     * 检查区间是否为空
     *
     * @return 如果区间为空返回true，否则返回false
     */
    fun isEmpty(): Boolean = range.start > range.endInclusive

    /**
     * 检查区间是否包含指定地址
     *
     * @param address 要检查的地址
     * @return 如果地址在区间内返回true，否则返回false
     */
    operator fun contains(address: Address): Boolean =
        !isEmpty() && address >= range.start && address <= range.endInclusive

    /**
     * 检查区间是否与另一个区间相交
     *
     * @param other 另一个区间
     * @return 如果两个区间相交返回true，否则返回false
     */
    fun intersects(other: Interval): Boolean {
        return !this.isEmpty() && !other.isEmpty() &&
                this.range.start <= other.range.endInclusive && other.range.start <= this.range.endInclusive
    }
}