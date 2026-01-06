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

package org.cangnova.cangjie.resolve.controlFlow.variable

import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.utils.ImmutableHashMap
import org.cangnova.cangjie.utils.ImmutableMap

/**
 * 平凡变量初始化信息的只读实现
 *
 * 用于表示平凡初始化变量（在声明时就已初始化的变量）的初始化状态。
 * 这些变量的状态可以静态确定，无需数据流分析。
 *
 * ## 设计特点
 *
 * 1. **不可变集合**: 使用 `ImmutableSet` 存储声明和初始化状态，保证线程安全
 * 2. **委托模式**: 通过 `delegate` 可以将平凡变量的状态与非平凡变量的状态组合
 * 3. **懒加载组合**: 只在需要时才创建组合对象
 *
 * @property declaredSet 已声明的平凡变量集合
 * @property initSet 已初始化的平凡变量集合
 * @property delegate 委托给非平凡变量的控制流信息（用于组合结果）
 */
class TrivialVariableInitInfo(
    private val declaredSet: Set<VariableDescriptor>,
    private val initSet: Set<VariableDescriptor>,
    private val delegate: VariableInitReadOnlyControlFlowInfo? = null
) : VariableInitReadOnlyControlFlowInfo {

    /**
     * 获取变量的初始化状态
     *
     * 查询顺序：
     * 1. 如果变量在 declaredSet 中，返回其初始化状态（基于 initSet）
     * 2. 否则委托给 delegate 查询（用于非平凡变量）
     *
     * @param key 要查询的变量描述符
     * @return 变量的控制流状态，如果未找到则返回 null
     */
    override fun getOrNull(key: VariableDescriptor): VariableControlFlowState? {
        if (key in declaredSet) {
            return VariableControlFlowState.create(isInitialized = key in initSet, isDeclared = true)
        }
        return delegate?.getOrNull(key)
    }

    /**
     * 检查 match 表达式中的确定初始化
     *
     * 委托给非平凡变量的检查逻辑
     */
    override fun checkDefiniteInitializationInMatch(merge: VariableInitReadOnlyControlFlowInfo): Boolean =
        delegate?.checkDefiniteInitializationInMatch(merge) ?: false

    /**
     * 替换委托对象
     *
     * 用于在合并平凡和非平凡变量结果时，创建新的组合对象
     *
     * @param newDelegate 新的委托对象（非平凡变量的控制流信息）
     * @return 新的控制流信息对象
     */
    fun replaceDelegate(newDelegate: VariableInitReadOnlyControlFlowInfo): VariableInitReadOnlyControlFlowInfo =
        TrivialVariableInitInfo(declaredSet, initSet, newDelegate)

    /**
     * 将控制流信息转换为不可变映射
     *
     * @return 变量描述符到控制流状态的映射
     */
    override fun asMap(): ImmutableMap<VariableDescriptor, VariableControlFlowState> {
        val initial = delegate?.asMap() ?: ImmutableHashMap.empty()
        return declaredSet.fold(initial) { acc, variableDescriptor ->
            acc.put(variableDescriptor, getOrNull(variableDescriptor)!!)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrivialVariableInitInfo) return false
        return declaredSet == other.declaredSet &&
                initSet == other.initSet &&
                delegate == other.delegate
    }

    override fun hashCode(): Int {
        var result = declaredSet.hashCode()
        result = 31 * result + initSet.hashCode()
        result = 31 * result + (delegate?.hashCode() ?: 0)
        return result
    }

    /**
     * 构建器类，用于增量构建平凡变量信息
     */
    class Builder {
        private val declaredSet = linkedSetOf<VariableDescriptor>()
        private val initSet = linkedSetOf<VariableDescriptor>()

        /**
         * 添加已声明的变量
         */
        fun addDeclared(descriptor: VariableDescriptor): Builder {
            declaredSet.add(descriptor)
            return this
        }

        /**
         * 添加已初始化的变量
         */
        fun addInitialized(descriptor: VariableDescriptor): Builder {
            initSet.add(descriptor)
            return this
        }

        /**
         * 添加多个已声明的变量
         */
        fun addAllDeclared(descriptors: Collection<VariableDescriptor>): Builder {
            declaredSet.addAll(descriptors)
            return this
        }

        /**
         * 添加多个已初始化的变量
         */
        fun addAllInitialized(descriptors: Collection<VariableDescriptor>): Builder {
            initSet.addAll(descriptors)
            return this
        }

        /**
         * 构建不可变的 TrivialVariableInitInfo 实例
         */
        fun build(): TrivialVariableInitInfo = TrivialVariableInitInfo(declaredSet.toSet(), initSet.toSet())

        /**
         * 创建当前状态的快照
         */
        fun snapshot(): TrivialVariableInitInfo = TrivialVariableInitInfo(
            declaredSet.toSet(),
            initSet.toSet()
        )
    }
}

/**
 * 平凡变量使用信息的只读实现
 *
 * 用于表示平凡初始化变量的使用状态。
 * 对于平凡变量，使用分析非常简单：所有被读取的变量都标记为 READ。
 *
 * @property usedSet 已使用的平凡变量集合
 * @property delegate 委托给非平凡变量的使用控制流信息
 */
class TrivialVariableUsageInfo(
    private val usedSet: Set<VariableDescriptor>,
    private val delegate: VariableUsageReadOnlyControlInfo? = null
) : VariableUsageReadOnlyControlInfo {

    /**
     * 获取变量的使用状态
     *
     * @param key 要查询的变量描述符
     * @return 变量的使用状态，如果未找到则返回 null
     */
    override fun getOrNull(key: VariableDescriptor): VariableUseState? {
        if (key in usedSet) return VariableUseState.READ
        return delegate?.getOrNull(key)
    }

    /**
     * 替换委托对象
     *
     * @param newDelegate 新的委托对象（非平凡变量的使用控制流信息）
     * @return 新的使用控制流信息对象
     */
    fun replaceDelegate(newDelegate: VariableUsageReadOnlyControlInfo): VariableUsageReadOnlyControlInfo =
        TrivialVariableUsageInfo(usedSet, newDelegate)

    /**
     * 将使用控制流信息转换为不可变映射
     *
     * @return 变量描述符到使用状态的映射
     */
    override fun asMap(): ImmutableMap<VariableDescriptor, VariableUseState> {
        val initial = delegate?.asMap() ?: ImmutableHashMap.empty()
        return usedSet.fold(initial) { acc, variableDescriptor ->
            acc.put(variableDescriptor, getOrNull(variableDescriptor)!!)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrivialVariableUsageInfo) return false
        return usedSet == other.usedSet && delegate == other.delegate
    }

    override fun hashCode(): Int {
        var result = usedSet.hashCode()
        result = 31 * result + (delegate?.hashCode() ?: 0)
        return result
    }
}
