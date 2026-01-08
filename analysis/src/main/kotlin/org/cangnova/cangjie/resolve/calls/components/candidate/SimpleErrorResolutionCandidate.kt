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

package org.cangnova.cangjie.resolve.calls.components.candidate

import org.cangnova.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import org.cangnova.cangjie.resolve.calls.components.ErrorDescriptorResolutionPart
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.model.CangJieCallComponents
import org.cangnova.cangjie.resolve.calls.model.MutableResolvedCallAtom
import org.cangnova.cangjie.resolve.calls.model.ResolutionPart
import org.cangnova.cangjie.resolve.calls.tower.ImplicitScopeTower

/**
 * 简单错误解析候选 (Simple Error Resolution Candidate)
 *
 * 这是 [SimpleResolutionCandidate] 的特化版本,专门用于处理错误描述符的情况。
 * 当重载解析找不到合适的候选,或者候选本身是错误描述符时,使用此类来表示该候选。
 *
 * ## 错误描述符
 * 错误描述符是编译器在解析失败时创建的占位符描述符,它们:
 * - 允许编译器继续进行后续分析,而不会因为单个错误而完全停止
 * - 提供有限的类型信息,通常使用错误类型
 * - 标记为错误状态,IDE 可以识别并提供相应的用户体验
 *
 * ## 简化的解析序列
 * 与普通候选不同,错误候选使用特殊的解析序列 [ErrorDescriptorResolutionPart],它:
 * - 跳过正常的类型检查和参数映射
 * - 直接设置空的类型参数映射和参数映射
 * - 使用空的替换器,避免类型推导
 * - 保证快速完成,不会产生额外的错误
 *
 * ## 使用场景
 * 此类在以下情况下创建:
 * 1. **符号未找到**: 调用了不存在的函数或访问了不存在的属性
 * 2. **可见性错误**: 目标存在但不可访问(private、internal 等)
 * 3. **解析歧义**: 存在多个候选且无法确定优先级
 * 4. **参数错误**: 无法创建有效的参数映射
 *
 * ## 示例
 * ```kotlin
 * // 假设没有名为 unknownFunction 的函数
 * val result = unknownFunction(42)
 * // 编译器会创建 SimpleErrorResolutionCandidate 来表示这个失败的调用
 * // 这允许后续分析继续进行,result 的类型会是错误类型
 * ```
 *
 * @property callComponents 调用组件
 * @property resolutionCallbacks 解析回调
 * @property scopeTower 作用域塔
 * @property baseSystem 基础约束系统
 * @property resolvedCall 已解析的调用原子(包含错误描述符)
 *
 * @see SimpleResolutionCandidate 父类,提供基本的候选功能
 * @see ErrorDescriptorResolutionPart 错误场景的特殊解析步骤
 */
class SimpleErrorResolutionCandidate(
    callComponents: CangJieCallComponents,
    resolutionCallbacks: CangJieResolutionCallbacks,
    scopeTower: ImplicitScopeTower,
    baseSystem: ConstraintStorage,
    resolvedCall: MutableResolvedCallAtom
) : SimpleResolutionCandidate(callComponents, resolutionCallbacks, scopeTower, baseSystem, resolvedCall) {
    /**
     * 解析步骤序列
     *
     * 错误候选只有一个解析步骤: [ErrorDescriptorResolutionPart]。
     * 这个步骤会快速完成解析,跳过所有正常的类型检查和参数映射,
     * 直接设置空的映射和替换器。
     *
     * 这确保了即使在错误情况下,解析也能快速完成,不会产生额外的
     * 级联错误或性能问题。
     */
    override val resolutionSequence: List<ResolutionPart> = listOf(ErrorDescriptorResolutionPart)
}
