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
 * 内存相关的扩展函数和工具方法
 */

/**
 * 创建可变地址空间的工厂函数
 *
 * @return 新的可变地址空间实例
 */
fun <T : AddressSpace.Region> mutableAddressSpace(): MutableAddressSpace<T> {
    return MutableAddressSpaceImpl()
}

/**
 * 检查地址是否在指定范围内
 *
 * @param range 要检查的范围
 * @return 如果地址在范围内返回true，否则抛出异常
 */
fun Address.requireInRange(range: AddressRange): Address {
    if (!range.contains(this)) {
        throw IllegalArgumentException("Address $this is not in range $range")
    }
    return this
}

/**
 * 检查范围是否在另一个范围内
 *
 * @param container 容器范围
 * @return 如果范围在容器内返回true，否则抛出异常
 */
fun AddressRange.requireInRange(container: AddressRange): AddressRange {
    if (!container.contains(this)) {
        throw IllegalArgumentException("Range $this is not in container $container")
    }
    return this
}

/**
 * 检查区间是否有效
 *
 * @param range 要检查的区间
 * @return 如果区间有效返回区间本身，否则抛出异常
 */
fun <T : Interval> T.checkInRange(container: AddressRange): T {
    if (!container.contains(this.range.start) || !container.contains(this.range.endInclusive)) {
        throw IllegalArgumentException("Interval $this is not in range $container")
    }
    return this
}

/**
 * 从地址空间中过滤区域
 *
 * @return 符合条件的区域集合
 */
fun <T : AddressSpace.Region> AddressSpace<T>.filterRegions(
    predicate: (T) -> Boolean = { true }
): Collection<T> {
    return getAllRegions().filter(predicate)
}

/**
 * 可变地址空间的简单实现
 */
private class MutableAddressSpaceImpl<T : AddressSpace.Region> : MutableAddressSpace<T> {
    private val regions = mutableMapOf<Address, T>()

    override fun getRegion(address: Address): T? {
        return regions[address]
    }

    override fun getRegions(range: AddressRange): Collection<T> {
        return regions.values.filter { it.range.intersects(range) }
    }

    override fun isEmpty(): Boolean = regions.isEmpty()

    override fun getAllRegions(): Collection<T> = regions.values.toList()

    override fun getOrAllocate(address: Address, factory: (AddressRange) -> T): T {
        return regions.getOrPut(address) {
            // 创建一个简单的范围，实际实现可能需要更复杂的逻辑
            factory(address.rangeTo(address))
        }
    }

    override fun reallocate(region: T) {
        regions[region.range.start] = region
    }

    override fun unallocate(range: AddressRange): UnallocatedRegions<T> {
        val toRemove = regions.values.filter { it.range.intersects(range) }
        toRemove.forEach { regions.remove(it.range.start) }
        return UnallocatedRegions(toRemove)
    }

    override fun clear() {
        regions.clear()
    }
}