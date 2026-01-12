/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.calls.components


import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.cangnova.cangjie.resolve.calls.results.*
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.utils.CancellationChecker


/**
 * 可调用引用重载冲突解析器 (Callable Reference Overload Conflict Resolver)
 *
 * 这是 [AbstractOverloadingConflictResolver] 的特化版本,专门用于解析可调用引用的重载冲突。
 * 当存在多个同名的函数或属性可以作为可调用引用的目标时,此解析器负责选择最合适的那个。
 *
 * @deprecated 此类已弃用。重载冲突解析现在使用 ECS (存在性约束系统) 实现,
 * 参见 [AbstractOverloadingConflictResolver] 中的 ECS 集成。
 *
 * ## 可调用引用
 * 可调用引用允许将函数或属性作为值传递,而不立即调用它们:
 * ```kotlin
 * fun foo(x: Int): String = x.toString()
 * fun foo(x: String): Int = x.length
 *
 * val ref1: (Int) -> String = ::foo    // 引用第一个 foo
 * val ref2: (String) -> Int = ::foo    // 引用第二个 foo
 * ```
 *
 * ## 重载解析策略
 * 与普通函数调用不同,可调用引用的重载解析主要基于:
 * 1. **期望类型**: 根据上下文推断的函数类型(如 `(Int) -> String`)
 * 2. **签名匹配**: 候选的参数类型和返回类型是否与期望类型兼容
 * 3. **特异性比较**: 当多个候选都适用时,选择最具体的那个
 *
 * ## 与普通调用解析的区别
 * - **无实际参数**: 可调用引用没有传递参数,只有期望的函数类型
 * - **无默认参数**: 不考虑默认参数(通过 `numDefaults = 0` 表示)
 * - **无扩展接收者**: 扩展接收者的处理由上层逻辑完成
 *
 * ## 实现细节
 * 此类通过以下方式配置父类 [AbstractOverloadingConflictResolver]:
 * - **candidateCall**: 提取 `candidate.candidate` 作为被比较的可调用描述符
 * - **createFlatSignature**: 创建简化的签名用于比较(只包含类型参数和 vararg 信息)
 * - **isDescriptorFromSource**: 判断描述符是否来自源代码
 *
 * @property builtIns 仓颉内置类型定义
 * @property module 当前模块描述符
 * @property specificityComparator 类型特异性比较器,用于比较类型的具体程度
 * @property platformOverloadsSpecificityComparator 平台重载特异性比较器,处理平台特定的重载
 * @property cancellationChecker 取消检查器,用于响应用户取消操作
 * @property statelessCallbacks 无状态回调接口,提供解析辅助功能
 * @property cangjieTypeRefiner 类型精炼器,处理智能类型转换
 *
 * @see AbstractOverloadingConflictResolver 通用的重载冲突解析器基类
 * @see CallableReferenceResolutionCandidate 可调用引用解析候选
 * @see FlatSignature 扁平化的函数签名,用于高效比较
 */
@Deprecated("使用 ECS (存在性约束系统) 替代，参见 AbstractOverloadingConflictResolver")
class CallableReferenceOverloadConflictResolver(
    builtIns: CangJieBuiltIns,
    module: ModuleDescriptor,
    specificityComparator: TypeSpecificityComparator,
    platformOverloadsSpecificityComparator: PlatformOverloadsSpecificityComparator,
    cancellationChecker: CancellationChecker,
    statelessCallbacks: CangJieResolutionStatelessCallbacks,
    cangjieTypeRefiner: CangJieTypeRefiner,
) : AbstractOverloadingConflictResolver<CallableReferenceResolutionCandidate>(
    builtIns,
    module,
    specificityComparator,
    platformOverloadsSpecificityComparator,
    cancellationChecker,
    { it.candidate },  // 提取被引用的可调用描述符
    Companion::createFlatSignature,  // 创建扁平化签名
    { null },  // 可调用引用没有调用参数映射
    { statelessCallbacks.isDescriptorFromSource(it) },  // 判断是否来自源代码
    null,  // 无额外的候选层级提供者
    cangjieTypeRefiner,
) {

    companion object {
        /**
         * 创建扁平化签名 (Flat Signature)
         *
         * 将可调用引用候选转换为扁平化的签名表示,用于高效的重载解析比较。
         * 扁平化签名只提取关键信息,忽略不影响重载解析的细节。
         *
         * ## 包含的信息
         * - **类型参数**: 函数的泛型参数列表
         * - **Vararg 标记**: 是否包含可变参数
         *
         * ## 不包含的信息
         * - **参数列表**: 对于可调用引用,参数匹配通过期望类型进行,不需要在签名中体现
         * - **默认参数数量**: 可调用引用不考虑默认参数,始终为 0
         * - **合成成员标记**: 可调用引用不涉及合成成员,始终为 false
         *
         * ## 为什么参数列表为空?
         * 可调用引用的重载解析不基于实际传递的参数,而是基于上下文的期望类型。
         * 例如 `val f: (Int) -> String = ::foo`,期望类型是 `(Int) -> String`,
         * 重载解析会检查哪个 `foo` 的签名与这个类型兼容,而不是检查传递了什么参数。
         *
         * @param candidate 可调用引用解析候选
         * @return 扁平化的签名,用于重载解析比较
         */
        private fun createFlatSignature(candidate: CallableReferenceResolutionCandidate): FlatSignature<CallableReferenceResolutionCandidate> {
            val descriptor = candidate.candidate
            return FlatSignature(
                candidate,
                descriptor.typeParameters,  // 泛型类型参数
                emptyList(),  // 参数列表为空,因为可调用引用不基于实际参数解析
                hasVarargs = descriptor.valueParameters.any { it.varargElementType != null },  // 检查是否有可变参数
                numDefaults = 0,  // 可调用引用不考虑默认参数
                isSyntheticMember = false  // 可调用引用不涉及合成成员
            )
        }
    }
}
