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

package org.cangnova.cangjie.resolve.calls.tower

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.receivers.QualifierReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.cangnova.cangjie.utils.isExtension

fun <C : Candidate> C.forceResolution(): C {
    resultingApplicability
    return this
}

private val INAPPLICABLE_STATUSES = setOf(
    CandidateApplicability.INAPPLICABLE,
    CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR,
    CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER
)

val CandidateApplicability.isInapplicable: Boolean
    get() = this in INAPPLICABLE_STATUSES

val CallableDescriptor.isSynthesized: Boolean
    get() = (this is CallableMemberDescriptor && kind == CallableMemberDescriptor.Kind.SYNTHESIZED)

/**
 * 判断候选函数是否需要扩展接收者
 *
 * 这个属性表示当前候选函数是否定义了扩展接收者参数
 * 扩展接收者参数用于指定一个类型，该类型的成员可以通过扩展函数或扩展属性直接调用
 *
 * @return Boolean 如果候选函数有扩展接收者参数，则返回true；否则返回false
 */
val CandidateWithBoundDispatchReceiver.requiresExtensionReceiver: Boolean
    get() = descriptor.isExtension


/**
 * 限定符作用域塔层级
 *
 * 这个类表示作用域塔中的一个特殊层级,用于解析通过限定符(Qualifier)访问的静态成员。
 * 限定符可以是包名、类名等。
 *
 * ## 功能说明
 *
 * 限定符访问不同于普通的成员访问,它访问的是类型的静态作用域中的成员,而不是实例成员。
 * 例如:
 * - `MyClass.staticFunc()` - 通过类名访问静态函数
 * - `MyClass.staticVar` - 通过类名访问静态属性
 * - `package.name.function()` - 通过包名访问顶层函数
 *
 * ## 工作原理
 *
 * 1. 使用 [qualifier] 的静态作用域 ([QualifierReceiver.staticScope]) 进行查找
 * 2. 支持类值接收者 ([QualifierReceiver.classValueReceiverWithSmartCastInfo]),用于访问类的静态成员
 * 3. 创建的候选者没有分发接收者 (dispatchReceiver = null),因为访问的是静态成员
 *
 * ## 与其他层级的区别
 *
 * - [MemberScopeTowerLevel]: 访问实例成员,有分发接收者
 * - [QualifierScopeTowerLevel]: 访问静态成员,无分发接收者
 * - [ScopeBasedTowerLevel]: 在普通作用域中查找
 *
 * ## 示例
 *
 * ```cangjie
 * class MyClass {
 *     static func create(): MyClass {
 *         return MyClass()
 *     }
 *
 *     static var instanceCount: Int64 = 0
 *
 *     init() {
 *         MyClass.instanceCount++
 *     }
 * }
 *
 * // 通过限定符访问静态成员:
 * let obj = MyClass.create()        // 访问静态函数
 * let count = MyClass.instanceCount // 访问静态属性
 * ```
 *
 * @property scopeTower 隐式作用域塔,提供上下文信息
 * @property qualifier 限定符接收者,包含静态作用域和可能的类值接收者
 *
 * @see QualifierReceiver 限定符接收者的定义
 * @see AbstractScopeTowerLevel 作用域塔层级的抽象基类
 * @see MemberScopeTowerLevel 用于实例成员访问的作用域层级
 */
internal class QualifierScopeTowerLevel(scopeTower: ImplicitScopeTower, val qualifier: QualifierReceiver) :
    AbstractScopeTowerLevel(scopeTower) {

    /**
     * 获取变量(属性、字段等)候选者
     *
     * 在限定符的静态作用域中查找指定名称的变量。
     * 这个方法用于解析通过限定符访问的静态属性。
     *
     * ## 查找过程
     *
     * 1. 在限定符的静态作用域 ([qualifier.staticScope]) 中查找
     * 2. 传递类值接收者 ([qualifier.classValueReceiverWithSmartCastInfo])
     * 3. 可选的扩展接收者用于查找扩展属性
     * 4. 将找到的变量描述符包装为候选者,不设置分发接收者
     *
     * ## 示例
     *
     * ```cangjie
     * class Config {
     *     static var maxSize: Int64 = 100
     * }
     * let size = Config.maxSize  // 通过限定符访问静态属性
     * ```
     *
     * @param name 要查找的变量名称
     * @param extensionReceiver 可选的扩展接收者,用于查找扩展属性
     * @return 找到的变量候选者集合,每个候选者都没有分发接收者
     */
    override fun getVariables(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> = qualifier.staticScope
        .getContributedVariablesAndInterceptAndEnumConstructor(
            name,
            location,
            qualifier,
            qualifier.classValueReceiverWithSmartCastInfo,
            scopeTower
        ).map {
            createCandidateDescriptor(it, dispatchReceiver = null)
        }

    /**
     * 获取函数和构造器候选者
     *
     * 在限定符的静态作用域中查找指定名称的函数或构造器。
     * 这个方法用于解析通过限定符访问的静态函数或构造器。
     *
     * ## 查找过程
     *
     * 1. 在限定符的静态作用域 ([qualifier.staticScope]) 中查找
     * 2. 传递类值接收者 ([qualifier.classValueReceiverWithSmartCastInfo])
     * 3. 可选的扩展接收者用于查找扩展函数
     * 4. 将找到的函数/构造器描述符包装为候选者,不设置分发接收者
     *
     * ## 示例
     *
     * ```cangjie
     * class Builder {
     *     static func create(name: String): Builder {
     *         return Builder(name)
     *     }
     *
     *     init(name: String) { /* ... */ }
     * }
     *
     * class Utils {
     *     static func format(text: String): String {
     *         return text.toUpperCase()
     *     }
     * }
     *
     * let b = Builder.create("test")  // 通过类名访问静态函数
     * let s = Utils.format("hello")   // 通过类名访问静态函数
     * ```
     *
     * @param name 要查找的函数名称
     * @param extensionReceiver 可选的扩展接收者,用于查找扩展函数
     * @return 找到的函数/构造器候选者集合,每个候选者都没有分发接收者
     */
    override fun getFunctions(
        name: Name
    ): Collection<CandidateWithBoundDispatchReceiver> = qualifier.staticScope
        .getContributedFunctionsAndEnumConstructors(
            name,
            location,
            qualifier,
            qualifier.classValueReceiverWithSmartCastInfo,

            scopeTower
        ).map {
            createCandidateDescriptor(it, dispatchReceiver = null)
        }

    /**
     * 记录名称查找
     *
     * 限定符作用域层级目前不记录查找操作。
     * 这是因为限定符访问通常是明确的,不需要跟踪用于性能分析或缓存失效。
     *
     * @param name 查找的名称
     */
    override fun recordLookup(name: Name) {
        // 不需要记录限定符访问的查找
    }
}
