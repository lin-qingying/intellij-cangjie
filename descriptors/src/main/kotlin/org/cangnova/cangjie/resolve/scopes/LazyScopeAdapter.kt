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

package org.cangnova.cangjie.resolve.scopes

import org.cangnova.cangjie.storage.LockBasedStorageManager
import org.cangnova.cangjie.storage.StorageManager

/**
 * 延迟作用域适配器 - 提供作用域的延迟初始化和线程安全访问
 *
 * LazyScopeAdapter 是 [AbstractScopeAdapter] 的一个特殊实现，用于延迟创建底层作用域。
 * 它利用 [StorageManager] 提供线程安全的惰性求值，确保作用域只在首次访问时初始化一次，
 * 这在构建复杂的作用域层次或避免循环依赖时特别有用。
 *
 * ## 核心功能
 *
 * ### 延迟初始化
 *
 * 作用域通过工厂函数 `getScope` 延迟创建：
 * - 首次访问时才执行 `getScope()`
 * - 结果被缓存，后续访问直接返回缓存值
 * - 线程安全（如果使用了带锁的 StorageManager）
 *
 * ### 自动适配器展开
 *
 * 创建时会自动展开嵌套的适配器：
 *
 * ```kotlin
 * private val lazyScope = storageManager.createLazyValue {
 *     getScope().let {
 *         if (it is AbstractScopeAdapter) it.getActualScope() else it
 *     }
 * }
 * ```
 *
 * 这避免了多层适配器嵌套造成的性能问题。
 *
 * ## 使用场景
 *
 * ### 1. 避免循环依赖
 *
 * 在构建相互引用的描述符时，延迟初始化可以打破循环：
 *
 * ```kotlin
 * class MyClassDescriptor(...) {
 *     // 成员作用域依赖于类描述符本身
 *     val memberScope = LazyScopeAdapter {
 *         // 此时 MyClassDescriptor 已经构造完成
 *         createMemberScope(this)
 *     }
 * }
 * ```
 *
 * ### 2. 性能优化 - 按需创建
 *
 * 某些作用域创建成本高，但不一定会被使用：
 *
 * ```kotlin
 * // 复杂的作用域构建
 * val expensiveScope = LazyScopeAdapter {
 *     // 只在实际访问时才执行
 *     val scope1 = buildScope1() // 耗时操作
 *     val scope2 = buildScope2() // 耗时操作
 *     ChainedMemberScope.create("Combined", scope1, scope2)
 * }
 *
 * // 如果用户代码从不访问此作用域，上述构建永远不会执行
 * ```
 *
 * ### 3. 延迟解析依赖
 *
 * 在某些阶段，依赖的符号可能还未解析完成：
 *
 * ```kotlin
 * // 父类作用域在构造子类时可能还未完全初始化
 * val parentScope = LazyScopeAdapter {
 *     // 延迟到实际使用时才解析父类
 *     val parent = resolveParentClass()
 *     parent.memberScope
 * }
 * ```
 *
 * ### 4. IDE 中的增量更新
 *
 * IDE 需要在代码变化时部分更新描述符：
 *
 * ```kotlin
 * val scope = LazyScopeAdapter(storageManager) {
 *     // storageManager 可以在代码变化时失效缓存
 *     buildCurrentScope()
 * }
 * ```
 *
 * ## 构造参数
 *
 * ### storageManager
 *
 * 控制延迟值的存储和线程安全策略：
 *
 * - **LockBasedStorageManager.NO_LOCKS**: 无锁版本，适合单线程场景
 * - **LockBasedStorageManager(lockName)**: 带锁版本，线程安全
 * - 自定义 StorageManager: 支持失效（invalidation）等高级功能
 *
 * ### getScope
 *
 * 工厂函数，返回实际的作用域：
 * - 仅在首次访问时调用一次
 * - 应该是幂等的（多次调用返回等价结果）
 * - 可以访问外部上下文（闭包）
 *
 * ## 实现细节
 *
 * ### 惰性值创建
 *
 * 使用 StorageManager 的 `createLazyValue` API：
 *
 * ```kotlin
 * private val lazyScope = storageManager.createLazyValue {
 *     getScope().let {
 *         if (it is AbstractScopeAdapter) it.getActualScope() else it
 *     }
 * }
 * ```
 *
 * ### workerScope 实现
 *
 * 覆盖父类的抽象属性，返回惰性值：
 *
 * ```kotlin
 * override val workerScope: MemberScope
 *     get() = lazyScope() // 调用惰性值，触发计算（如果尚未计算）
 * ```
 *
 * ## 线程安全性
 *
 * ### 无锁模式（默认）
 *
 * ```kotlin
 * val scope = LazyScopeAdapter { createScope() }
 * // 等价于
 * val scope = LazyScopeAdapter(LockBasedStorageManager.NO_LOCKS) { createScope() }
 * ```
 *
 * - 适用于单线程场景
 * - 性能最优，无同步开销
 * - **注意**: 多线程访问可能导致重复初始化
 *
 * ### 带锁模式
 *
 * ```kotlin
 * val storageManager = LockBasedStorageManager("MyScope")
 * val scope = LazyScopeAdapter(storageManager) { createScope() }
 * ```
 *
 * - 线程安全，保证只初始化一次
 * - 使用读写锁，读操作可并发
 * - 适用于多线程环境（如 IDE 后台任务）
 *
 * ## 与其他延迟机制的对比
 *
 * | 机制 | LazyScopeAdapter | Kotlin lazy | 手动延迟 |
 * |------|------------------|------------|---------|
 * | 线程安全 | 可配置 | 可配置 | 需手动实现 |
 * | 失效支持 | 支持（通过 StorageManager） | 不支持 | 需手动实现 |
 * | 适配器展开 | 自动 | 无 | 需手动实现 |
 * | 与描述符系统集成 | 完整 | 无 | 无 |
 *
 * ## 性能特性
 *
 * ### 初始化成本
 *
 * - 创建 LazyScopeAdapter 本身：O(1)，极低成本
 * - 首次访问：执行 `getScope()`，成本取决于工厂函数
 * - 后续访问：O(1)，直接返回缓存值
 *
 * ### 内存占用
 *
 * - 未初始化：仅存储工厂函数（闭包）
 * - 已初始化：存储结果 MemberScope，释放工厂函数引用
 *
 * ## 常见陷阱
 *
 * ### 1. 循环初始化
 *
 * ```kotlin
 * // 错误示例：getScope 内部访问自身
 * val scope = LazyScopeAdapter {
 *     scope.getContributedFunctions(...) // 死锁或栈溢出！
 *     ...
 * }
 * ```
 *
 * **解决方案**: 确保工厂函数不依赖于正在创建的作用域。
 *
 * ### 2. 捕获可变状态
 *
 * ```kotlin
 * // 错误示例：捕获可能变化的状态
 * var currentModule = module1
 * val scope = LazyScopeAdapter {
 *     currentModule.scope // 可能使用错误的 module
 * }
 * currentModule = module2
 * ```
 *
 * **解决方案**: 确保闭包捕获的是不可变的或最终状态。
 *
 * ### 3. 副作用在工厂函数中
 *
 * ```kotlin
 * // 不推荐：副作用可能不可预测
 * val scope = LazyScopeAdapter {
 *     println("Initializing scope") // 何时执行不确定
 *     createScope()
 * }
 * ```
 *
 * ## 典型用法模式
 *
 * ### 模式 1: 类描述符的成员作用域
 *
 * ```kotlin
 * class LazyClassDescriptor(...) : ClassDescriptor {
 *     private val storageManager = ...
 *
 *     override val unsubstitutedMemberScope = LazyScopeAdapter(storageManager) {
 *         // 延迟构建成员作用域
 *         val declaredMembers = buildDeclaredMembersScope()
 *         val inheritedMembers = buildInheritedMembersScope()
 *
 *         ChainedMemberScope.create(
 *             name.asString(),
 *             declaredMembers,
 *             inheritedMembers
 *         )
 *     }
 * }
 * ```
 *
 * ### 模式 2: 条件作用域
 *
 * ```kotlin
 * fun getScopeForType(type: CangJieType): MemberScope {
 *     return LazyScopeAdapter {
 *         when {
 *             type.isError -> ErrorScope(...)
 *             type.isNullable -> createNullableSafeMemberScope(type)
 *             else -> type.constructor.declarationDescriptor.defaultType.memberScope
 *         }
 *     }
 * }
 * ```
 *
 * ### 模式 3: 组合多个延迟作用域
 *
 * ```kotlin
 * val combinedScope = LazyScopeAdapter {
 *     val scope1 = lazyScope1.workerScope
 *     val scope2 = lazyScope2.workerScope
 *     ChainedMemberScope.create("Combined", scope1, scope2)
 * }
 * ```
 *
 * ## 调试技巧
 *
 * ### 检查是否已初始化
 *
 * 由于 `lazyScope` 是私有的，无法直接检查。可以通过日志：
 *
 * ```kotlin
 * val scope = LazyScopeAdapter {
 *     println("LazyScopeAdapter initialized at ${System.currentTimeMillis()}")
 *     createScope()
 * }
 * ```
 *
 * ### 追踪初始化堆栈
 *
 * 在工厂函数中添加堆栈跟踪，帮助定位何时何地触发初始化：
 *
 * ```kotlin
 * val scope = LazyScopeAdapter {
 *     Thread.currentThread().stackTrace.forEach { println(it) }
 *     createScope()
 * }
 * ```
 *
 * ## 示例：完整的延迟作用域使用
 *
 * ```kotlin
 * // 创建带锁的存储管理器（线程安全）
 * val storageManager = LockBasedStorageManager("MyClass")
 *
 * // 定义类描述符
 * class MyClassDescriptor(...) {
 *     // 成员作用域使用延迟加载
 *     val memberScope = LazyScopeAdapter(storageManager) {
 *         // 这段代码仅在首次访问 memberScope 时执行
 *
 *         // 1. 构建自身声明的成员
 *         val declaredScope = buildDeclaredMembersScope()
 *
 *         // 2. 获取父类成员（父类此时应已完全初始化）
 *         val parentScope = superTypes.firstOrNull()?.let {
 *             val parentClass = it.constructor.declarationDescriptor as? ClassDescriptor
 *             parentClass?.unsubstitutedMemberScope
 *         }
 *
 *         // 3. 组合作用域
 *         if (parentScope != null) {
 *             ChainedMemberScope.create(
 *                 name.asString(),
 *                 declaredScope,
 *                 parentScope
 *             )
 *         } else {
 *             declaredScope
 *         }
 *     }
 * }
 *
 * // 使用
 * val myClass = MyClassDescriptor(...)
 * // 此时 memberScope 尚未初始化
 *
 * val functions = myClass.memberScope.getContributedFunctions(...)
 * // 首次访问，触发初始化
 *
 * val variables = myClass.memberScope.getContributedVariables(...)
 * // 后续访问，直接使用缓存
 * ```
 *
 * @param storageManager 存储管理器，控制延迟值的线程安全和失效策略。
 *                       默认为 [LockBasedStorageManager.NO_LOCKS]（无锁，单线程）
 * @param getScope 工厂函数，返回实际的作用域。仅在首次访问时调用一次
 *
 * @property lazyScope 内部的惰性值，存储延迟创建的作用域
 * @property workerScope 覆盖父类属性，返回惰性计算的作用域
 *
 * @see AbstractScopeAdapter 父类，提供委托基础设施
 * @see StorageManager 存储管理器接口
 * @see LockBasedStorageManager 基于锁的存储管理器实现
 */
class LazyScopeAdapter @JvmOverloads constructor(
    storageManager: StorageManager = LockBasedStorageManager.NO_LOCKS,
    getScope: () -> MemberScope
) : AbstractScopeAdapter() {

    private val lazyScope = storageManager.createLazyValue{
        getScope().let {
            if (it is AbstractScopeAdapter) it.getActualScope() else it
        }
    }

    override val workerScope: MemberScope
        get() = lazyScope()


}
