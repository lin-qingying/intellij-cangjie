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

package org.cangnova.cangjie.types

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.SupertypeLoopChecker
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.checker.refineTypes

/**
 * 抽象类型构造器 - 提供类型父类型管理和循环依赖检测的基础实现
 *
 * AbstractTypeConstructor 是 [TypeConstructor] 接口的抽象实现，为类型系统中的类型构造器
 * 提供父类型（supertypes）的延迟计算、循环依赖检测和断开、以及类型精炼（refinement）等核心功能。
 * 它是仓颉语言类型系统中类、接口、类型别名等分类器的类型构造器的基础实现。
 *
 * ## 核心功能
 *
 * ### 父类型延迟计算
 *
 * 父类型通过 [StorageManager.createLazyValueWithPostCompute] 延迟计算：
 * - **计算阶段**: 调用 [computeSupertypes] 获取所有声明的父类型
 * - **后处理阶段**: 检测并断开循环依赖，处理空父类型列表
 * - **错误恢复**: 如果计算过程中发生错误，返回错误类型
 *
 * ### 循环依赖检测和断开
 *
 * 使用 [SupertypeLoopChecker] 检测父类型图中的循环：
 * - 检测直接的父类型循环（A extends B extends A）
 * - 检测通过伴随对象（companion）的循环
 * - 断开循环边，替换为错误类型
 * - 报告循环依赖的诊断信息
 *
 * ### 类型精炼（Type Refinement）
 *
 * 支持模块视图的类型精炼：
 * - 通过 [refine] 方法创建 [ModuleViewTypeConstructor]
 * - 使用 [CangJieTypeRefiner] 精炼父类型
 * - 避免死锁（使用 PUBLICATION 模式的 lazy）
 *
 * ## 使用场景
 *
 * ### 1. 类描述符的类型构造器
 *
 * 类的类型构造器管理其父类和实现的接口：
 *
 * ```kotlin
 * // 源代码示例：
 * // class MyClass : ParentClass, Interface1, Interface2 {
 * //     // ...
 * // }
 *
 * class LazyClassTypeConstructor(
 *     private val classDescriptor: LazyClassDescriptor,
 *     storageManager: StorageManager
 * ) : AbstractTypeConstructor(storageManager) {
 *     override fun computeSupertypes(): Collection<CangJieType> {
 *         // 从 PSI 或元数据中解析父类型
 *         val supertypeReferences = classDescriptor.getSuperTypeReferences()
 *         return supertypeReferences.map { resolveType(it) }
 *     }
 *
 *     override val supertypeLoopChecker: SupertypeLoopChecker
 *         get() = SupertypeLoopChecker.INSTANCE
 *
 *     override fun reportSupertypeLoopError(type: CangJieType) {
 *         // 报告错误: "Cyclic inheritance involving MyClass"
 *         trace.report(Errors.CYCLIC_INHERITANCE_HIERARCHY.on(classDescriptor))
 *     }
 * }
 * ```
 *
 * ### 2. 循环继承的检测和报告
 *
 * 检测多种形式的循环继承：
 *
 * ```kotlin
 * // 源代码错误示例 1: 直接循环
 * // class A : B {}
 * // class B : A {}  // 错误：循环继承
 *
 * // 源代码错误示例 2: 间接循环
 * // class A : B {}
 * // class B : C {}
 * // class C : A {}  // 错误：循环继承
 *
 * // AbstractTypeConstructor 的处理：
 * // 1. 计算 A 的父类型时，触发 B 的计算
 * // 2. 计算 B 的父类型时，触发 C 的计算
 * // 3. 计算 C 的父类型时，发现 A 正在计算中 → 检测到循环
 * // 4. 断开循环边：C -> A 被替换为 C -> ErrorType
 * // 5. 在所有涉及的类型上报告诊断
 * ```
 *
 * ### 3. 类型别名的父类型
 *
 * 类型别名的父类型是其展开类型的父类型：
 *
 * ```kotlin
 * // typealias MyInt = Int32
 *
 * class TypeAliasConstructor(
 *     private val typeAlias: TypeAliasDescriptor,
 *     storageManager: StorageManager
 * ) : AbstractTypeConstructor(storageManager) {
 *     override fun computeSupertypes(): Collection<CangJieType> {
 *         // 类型别名的父类型是展开类型的父类型
 *         val expandedType = typeAlias.expandedType
 *         return expandedType.constructor.supertypes
 *     }
 *
 *     override val supertypeLoopChecker: SupertypeLoopChecker
 *         get() = SupertypeLoopChecker.INSTANCE
 * }
 * ```
 *
 * ### 4. 空父类型的默认处理
 *
 * 为没有显式父类型的类型提供默认父类型：
 *
 * ```kotlin
 * // 源代码: class MyClass {}  // 没有显式父类
 *
 * override fun defaultSupertypeIfEmpty(): CangJieType? {
 *     // 如果没有显式父类型，默认继承 Any
 *     return builtIns.anyType
 * }
 * ```
 *
 * ## 实现细节
 *
 * ### 三阶段父类型计算
 *
 * 使用 `createLazyValueWithPostCompute` 分三个阶段计算父类型：
 *
 * ```kotlin
 * private val _supertypes = storageManager.createLazyValueWithPostCompute(
 *     // 阶段 1: 计算原始父类型
 *     compute = { Supertypes(computeSupertypes()) },
 *
 *     // 阶段 2: 错误恢复（计算失败时）
 *     onRecursiveCall = { Supertypes(listOf(ErrorUtils.errorTypeForLoopInSupertypes)) },
 *
 *     // 阶段 3: 后处理（循环检测和断开）
 *     postCompute = { supertypes ->
 *         var resultWithoutCycles = supertypeLoopChecker.findLoopsInSupertypesAndDisconnect(
 *             this, supertypes.allSupertypes,
 *             { it.computeNeighbours(useCompanions = false) },
 *             { reportSupertypeLoopError(it) }
 *         )
 *
 *         if (resultWithoutCycles.isEmpty()) {
 *             resultWithoutCycles = defaultSupertypeIfEmpty()?.let { listOf(it) }.orEmpty()
 *         }
 *
 *         supertypes.supertypesWithoutCycles = processSupertypesWithoutCycles(resultWithoutCycles)
 *     }
 * )
 * ```
 *
 * ### Supertypes 双版本存储
 *
 * 内部类 [Supertypes] 存储两个版本的父类型：
 *
 * ```kotlin
 * private class Supertypes(val allSupertypes: Collection<CangJieType>) {
 *     var supertypesWithoutCycles: List<CangJieType> = listOf(ErrorUtils.errorTypeForLoopInSupertypes)
 * }
 * ```
 *
 * - **allSupertypes**: 包含所有声明的父类型（包括循环边）
 *   - 用于计算邻居节点（检测循环时需要）
 * - **supertypesWithoutCycles**: 断开循环后的父类型
 *   - 用于对外提供的 `supertypes` 属性
 *   - 确保类型系统不会陷入无限递归
 *
 * ### 循环依赖检测算法
 *
 * 使用深度优先搜索（DFS）检测父类型图中的循环：
 *
 * ```kotlin
 * // 伪代码
 * fun findLoopsInSupertypesAndDisconnect(
 *     start: TypeConstructor,
 *     supertypes: Collection<CangJieType>,
 *     getNeighbours: (TypeConstructor) -> Collection<CangJieType>,
 *     reportError: (CangJieType) -> Unit
 * ): List<CangJieType> {
 *     val visited = mutableSetOf<TypeConstructor>()
 *     val onStack = mutableSetOf<TypeConstructor>()
 *     val result = mutableListOf<CangJieType>()
 *
 *     for (supertype in supertypes) {
 *         if (dfs(supertype, visited, onStack, getNeighbours)) {
 *             reportError(supertype)  // 发现循环，报告错误
 *             // 不添加到 result（断开循环边）
 *         } else {
 *             result.add(supertype)
 *         }
 *     }
 *
 *     return result
 * }
 * ```
 *
 * ### 类型精炼和模块视图
 *
 * [ModuleViewTypeConstructor] 提供模块视图的类型精炼：
 *
 * ```kotlin
 * override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor =
 *     ModuleViewTypeConstructor(cangjieTypeRefiner)
 *
 * private inner class ModuleViewTypeConstructor(
 *     private val cangjieTypeRefiner: CangJieTypeRefiner
 * ) : TypeConstructor {
 *     // 使用 PUBLICATION 模式避免死锁
 *     private val refinedSupertypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
 *         cangjieTypeRefiner.refineTypes(this@AbstractTypeConstructor.supertypes)
 *     }
 *
 *     override val supertypes: Collection<CangJieType>
 *         get() = refinedSupertypes
 *
 *     // 其他属性委托给外部类型构造器
 * }
 * ```
 *
 * **为什么使用 PUBLICATION 模式？**
 *
 * - 避免死锁：内置类型的 StorageManager 可能与源码的 StorageManager 不同
 * - 锁获取顺序：DefaultBuiltIns lock → Sources lock
 * - 其他代码可能以相反顺序获取锁：Sources lock → Built-ins lock
 * - PUBLICATION 模式允许在没有锁的情况下初始化
 *
 * ### 伴随对象的循环检测
 *
 * 额外检测通过伴随对象的作用域循环：
 *
 * ```kotlin
 * // 源代码示例：
 * // class A {
 * //     companion object : B {}
 * // }
 * // class B {
 * //     companion object : A {}
 * // }
 *
 * // 检测逻辑
 * if (shouldReportCyclicScopeWithCompanionWarning) {
 *     supertypeLoopChecker.findLoopsInSupertypesAndDisconnect(
 *         this, resultWithoutCycles,
 *         { it.computeNeighbours(useCompanions = true) },  // 包含伴随对象边
 *         { reportScopesLoopError(it) }
 *     )
 * }
 * ```
 *
 * ## 性能特性
 *
 * ### 延迟计算成本
 *
 * - **创建 AbstractTypeConstructor**: O(1)，仅存储引用
 * - **首次访问 supertypes**:
 *   - 调用 `computeSupertypes()`: 成本取决于子类实现
 *   - 循环检测: O(V + E)，V 是类型数量，E 是父类型边数量
 *   - 后处理: O(S)，S 是父类型数量
 * - **后续访问 supertypes**: O(1)，直接返回缓存值
 *
 * ### 循环检测复杂度
 *
 * - **最坏情况**: O(V + E)，完整的类型层次图遍历
 * - **平均情况**: O(S)，仅遍历直接父类型的邻居
 * - **最优情况**: O(1)，无循环且父类型已计算
 *
 * ### 内存占用
 *
 * - **未初始化**: 仅存储 StorageManager 引用
 * - **已初始化**:
 *   - Supertypes 对象（两个列表）
 *   - 父类型列表（去除循环后）
 *   - ModuleViewTypeConstructor（如果使用了 refine）
 *
 * ## 抽象方法和扩展点
 *
 * ### 必须实现的抽象方法
 *
 * ```kotlin
 * // 计算所有声明的父类型（在循环检测之前）
 * protected abstract fun computeSupertypes(): Collection<CangJieType>
 *
 * // 计算通过 extend 关键字声明的父类型
 * protected abstract fun computeExtendSuperTypes(extendId: String?): Collection<CangJieType>
 *
 * // 提供父类型循环检测器
 * protected abstract val supertypeLoopChecker: SupertypeLoopChecker
 * ```
 *
 * ### 可选覆盖的方法
 *
 * ```kotlin
 * // 报告父类型循环错误
 * protected open fun reportSupertypeLoopError(type: CangJieType) {}
 *
 * // 后处理无循环的父类型（例如排序、去重）
 * protected open fun processSupertypesWithoutCycles(
 *     supertypes: List<CangJieType>
 * ): List<CangJieType> = supertypes
 *
 * // 报告作用域循环错误（伴随对象相关）
 * protected open fun reportScopesLoopError(type: CangJieType) {}
 *
 * // 是否应该报告伴随对象的循环警告
 * protected open val shouldReportCyclicScopeWithCompanionWarning: Boolean = false
 *
 * // 获取额外的邻居节点（用于循环检测）
 * protected open fun getAdditionalNeighboursInSupertypeGraph(
 *     useCompanions: Boolean
 * ): Collection<CangJieType> = emptyList()
 *
 * // 父类型列表为空时的默认父类型
 * protected open fun defaultSupertypeIfEmpty(): CangJieType? = null
 * ```
 *
 * ## 典型使用模式
 *
 * ### 模式 1: 类的类型构造器
 *
 * ```kotlin
 * class LazyClassTypeConstructor(
 *     private val classDescriptor: LazyClassDescriptor,
 *     storageManager: StorageManager
 * ) : AbstractTypeConstructor(storageManager) {
 *     override fun computeSupertypes(): Collection<CangJieType> {
 *         val superTypeReferences = classDescriptor.getSuperTypeReferences()
 *         if (superTypeReferences.isEmpty()) {
 *             return emptyList()  // defaultSupertypeIfEmpty 会提供 Any
 *         }
 *         return superTypeReferences.map { resolveType(it) }
 *     }
 *
 *     override fun computeExtendSuperTypes(extendId: String?): Collection<CangJieType> {
 *         // 解析 extend 子句中的父类型
 *         return classDescriptor.getExtendReferences(extendId).map { resolveType(it) }
 *     }
 *
 *     override val supertypeLoopChecker: SupertypeLoopChecker
 *         get() = SupertypeLoopChecker.INSTANCE
 *
 *     override fun defaultSupertypeIfEmpty(): CangJieType? {
 *         // 除了 Any 自身，所有类默认继承 Any
 *         return if (classDescriptor.name == Name.identifier("Any")) {
 *             null
 *         } else {
 *             builtIns.anyType
 *         }
 *     }
 *
 *     override fun reportSupertypeLoopError(type: CangJieType) {
 *         trace.report(Errors.CYCLIC_INHERITANCE_HIERARCHY.on(
 *             classDescriptor.getPsiElement()
 *         ))
 *     }
 *
 *     override val parameters: List<TypeParameterDescriptor>
 *         get() = classDescriptor.declaredTypeParameters
 *
 *     override val declarationDescriptor: ClassifierDescriptor
 *         get() = classDescriptor
 *
 *     override val isFinal: Boolean
 *         get() = classDescriptor.modality == Modality.FINAL
 *
 *     override val isDenotable: Boolean
 *         get() = true
 *
 *     override val builtIns: CangJieBuiltIns
 *         get() = classDescriptor.module.builtIns
 * }
 * ```
 *
 * ### 模式 2: 处理泛型实例化
 *
 * ```kotlin
 * // 源代码: class List<T> : Collection<T>
 * // 实例化: List<Int>
 *
 * class GenericClassTypeConstructor(
 *     private val classDescriptor: ClassDescriptor,
 *     storageManager: StorageManager
 * ) : AbstractTypeConstructor(storageManager) {
 *     override fun computeSupertypes(): Collection<CangJieType> {
 *         // 获取原始父类型（带类型参数）
 *         val rawSupertypes = classDescriptor.typeConstructor.supertypes
 *         // Collection<T>
 *         return rawSupertypes
 *     }
 *
 *     override fun processSupertypesWithoutCycles(
 *         supertypes: List<CangJieType>
 *     ): List<CangJieType> {
 *         // 对父类型进行类型参数替换
 *         // 例如：将 Collection<T> 替换为 Collection<Int>
 *         val substitutor = TypeSubstitutor.create(typeParameterSubstitution)
 *         return supertypes.map { substitutor.substitute(it, Variance.INVARIANT)!! }
 *     }
 * }
 * ```
 *
 * ### 模式 3: 调试父类型计算
 *
 * ```kotlin
 * // 使用调试信息
 * val debugInfo = typeConstructor.renderAdditionalDebugInformation()
 * println(debugInfo)
 * // 输出: "supertypes=LazyValue{computed=true, value=[Int32, Comparable<Int>]}"
 *
 * // 自定义调试输出
 * override fun toString(): String {
 *     return "TypeConstructor(${declarationDescriptor?.name}, " +
 *            "supertypes=${renderAdditionalDebugInformation()})"
 * }
 * ```
 *
 * ## 常见陷阱
 *
 * ### 1. 在 computeSupertypes 中访问自身的 supertypes
 *
 * ```kotlin
 * // 错误示例：导致无限递归
 * override fun computeSupertypes(): Collection<CangJieType> {
 *     val mySuperTypes = this.supertypes  // 错误！触发重入
 *     // ...
 * }
 *
 * // 正确做法：仅从声明中解析
 * override fun computeSupertypes(): Collection<CangJieType> {
 *     val references = classDescriptor.getSuperTypeReferences()
 *     return references.map { resolveType(it) }
 * }
 * ```
 *
 * ### 2. 忘记处理空父类型列表
 *
 * ```kotlin
 * // 可能导致问题：类没有父类型
 * override fun computeSupertypes(): Collection<CangJieType> {
 *     return emptyList()  // 如果未覆盖 defaultSupertypeIfEmpty，类将无父类型
 * }
 *
 * // 正确做法
 * override fun defaultSupertypeIfEmpty(): CangJieType? {
 *     return builtIns.anyType
 * }
 * ```
 *
 * ### 3. 不报告循环依赖错误
 *
 * ```kotlin
 * // 问题：用户看不到错误
 * override fun reportSupertypeLoopError(type: CangJieType) {
 *     // 空实现，不报告错误
 * }
 *
 * // 正确做法
 * override fun reportSupertypeLoopError(type: CangJieType) {
 *     trace.report(Errors.CYCLIC_INHERITANCE_HIERARCHY.on(psiElement))
 * }
 * ```
 *
 * ### 4. 在多线程环境下使用错误的 StorageManager
 *
 * ```kotlin
 * // 危险示例：IDE 环境使用无锁版本
 * AbstractTypeConstructor(LockBasedStorageManager.NO_LOCKS)
 * // 可能导致：多个线程同时计算 supertypes，竞态条件
 *
 * // 正确做法：IDE 环境使用带锁版本
 * AbstractTypeConstructor(LockBasedStorageManager("ClassTypeConstructor"))
 * ```
 *
 * ## 线程安全性
 *
 * 线程安全性完全取决于传入的 [StorageManager]：
 *
 * - **无锁模式**: 适用于单线程批量编译
 * - **带锁模式**: 适用于 IDE 多线程环境
 * - **ModuleViewTypeConstructor**: 使用 PUBLICATION 模式避免死锁
 *
 * ## 调试支持
 *
 * ### 调试信息方法
 *
 * ```kotlin
 * // 获取父类型的调试信息
 * fun renderAdditionalDebugInformation(): String =
 *     "supertypes=${_supertypes.renderDebugInformation()}"
 * ```
 *
 * 输出示例：
 * ```
 * supertypes=LazyValue{computed=false}  // 未计算
 * supertypes=LazyValue{computed=true, value=[ParentClass, Interface1, Interface2]}  // 已计算
 * ```
 *
 * ## 示例：完整的循环检测流程
 *
 * ```kotlin
 * // 源代码：
 * // class A : B {}  // A 的父类型是 B
 * // class B : C {}  // B 的父类型是 C
 * // class C : A {}  // C 的父类型是 A （循环！）
 *
 * // 1. 访问 A.supertypes
 * val aSupertypes = typeConstructorA.supertypes
 *
 * // 2. 首次访问，触发计算
 * // - 调用 A.computeSupertypes() → 返回 [B]
 * // - 创建 Supertypes(allSupertypes = [B])
 * // - 进入后处理阶段
 *
 * // 3. 后处理：循环检测
 * // - 调用 supertypeLoopChecker.findLoopsInSupertypesAndDisconnect
 * // - 计算 A 的邻居：A.computeNeighbours() → [B]
 * // - DFS 遍历 B：
 * //   - 访问 B.supertypes → 触发 B 的计算
 * //   - B.computeSupertypes() → 返回 [C]
 * //   - 计算 B 的邻居 → [C]
 * //   - DFS 遍历 C：
 * //     - 访问 C.supertypes → 触发 C 的计算
 * //     - C.computeSupertypes() → 返回 [A]
 * //     - 计算 C 的邻居 → [A]
 * //     - DFS 遍历 A → 发现 A 已在栈中！检测到循环
 * //     - 断开循环边：C -> A
 * //     - 调用 reportSupertypeLoopError(A)
 *
 * // 4. 结果：
 * // - A.supertypes = [B]
 * // - B.supertypes = [C]
 * // - C.supertypes = [ErrorType]  // 循环被断开
 * // - 用户看到错误：Cyclic inheritance hierarchy detected
 * ```
 *
 * @param storageManager 存储管理器，控制父类型的延迟计算和线程安全
 *
 * @property supertypes 去除循环依赖后的父类型列表
 * @property _supertypes 内部父类型存储（包含循环检测逻辑）
 *
 * @see TypeConstructor 实现的接口
 * @see ClassifierBasedTypeConstructor 父类
 * @see StorageManager 延迟值管理器
 * @see SupertypeLoopChecker 循环检测器
 * @see CangJieTypeRefiner 类型精炼器
 * @see ModuleViewTypeConstructor 模块视图的类型构造器
 */
abstract class AbstractTypeConstructor(storageManager: StorageManager) : ClassifierBasedTypeConstructor() {
    /**
     * 父类型的延迟计算值
     *
     * 使用三阶段计算模式：
     * 1. 计算阶段：调用 computeSupertypes() 获取所有声明的父类型
     * 2. 错误恢复：如果发生递归调用，返回错误类型
     * 3. 后处理：检测并断开循环依赖，处理空父类型列表
     */
    private val _supertypes = storageManager.createLazyValueWithPostCompute(
        { Supertypes(computeSupertypes()) },
        {
            Supertypes(listOf(ErrorUtils.errorTypeForLoopInSupertypes))
        },
        { supertypes ->
            // 重要：循环断开必须在后处理阶段开始，这样可以保证
            // 当我们开始计算父类型的父类型时（用于计算邻居节点），它们也开始自己的循环断开过程
            // 因为我们希望在所有声明上报告循环诊断，所以它们应该看到一致版本的 'allSupertypes'
            var resultWithoutCycles =
                supertypeLoopChecker.findLoopsInSupertypesAndDisconnect(
                    this, supertypes.allSupertypes,
                    { it.computeNeighbours(useCompanions = false) },
                    { reportSupertypeLoopError(it) }
                )

            if (resultWithoutCycles.isEmpty()) {
                resultWithoutCycles = defaultSupertypeIfEmpty()?.let { listOf(it) }.orEmpty()
            }

            // 我们还检查是否存在带有从伴随对象所有者到伴随对象本身的额外边的循环
            // 注意，我们使用已断开的类型，以避免在循环父类型上报告两个诊断
            if (shouldReportCyclicScopeWithCompanionWarning) {
                supertypeLoopChecker.findLoopsInSupertypesAndDisconnect(
                    this, resultWithoutCycles,
                    { it.computeNeighbours(useCompanions = true) },
                    { reportScopesLoopError(it) }
                )
            }

            supertypes.supertypesWithoutCycles = processSupertypesWithoutCycles(
                resultWithoutCycles as? List<CangJieType> ?: resultWithoutCycles.toList()
            )
        })

    /**
     * 获取去除循环依赖后的父类型列表
     */
    override val supertypes
        get() = _supertypes().supertypesWithoutCycles

    /**
     * 精炼类型构造器，创建模块视图的类型构造器
     *
     * @param cangjieTypeRefiner 类型精炼器
     * @return 精炼后的类型构造器
     */
    
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor =
        ModuleViewTypeConstructor(cangjieTypeRefiner)

    /**
     * 模块视图类型构造器
     *
     * 提供类型精炼功能，用于在模块视图中使用精炼后的父类型。
     *
     * 注意：这里使用 PUBLICATION 模式而不是 'storageManager.createLazyValue { ... }' 非常重要
     *
     * 原因是 'storageManager' 可能是来自 DefaultBuiltIns 的存储管理器
     * （例如，如果这个类型构造器是某个内置类如 'Int' 的类型构造器）。
     * 因此，调用精炼的父类型会导致以下获取锁的顺序：
     * DefaultBuiltIns lock → Sources lock
     *
     * 显然，很多代码以不同的顺序获取锁（先获取源码锁，然后是内置类型锁），
     * 这会导致死锁。
     */
    
    private inner class ModuleViewTypeConstructor(
        private val cangjieTypeRefiner: CangJieTypeRefiner
    ) : TypeConstructor {
        /**
         * 精炼后的父类型列表
         * 使用 PUBLICATION 模式避免死锁
         */
        private val refinedSupertypes by lazy(LazyThreadSafetyMode.PUBLICATION) {

            cangjieTypeRefiner.refineTypes(this@AbstractTypeConstructor.supertypes)
        }

        override val parameters: List<TypeParameterDescriptor>
            get() = this@AbstractTypeConstructor.parameters

        override val supertypes: Collection<CangJieType>
            get() = refinedSupertypes

        override val isFinal: Boolean
            get() = this@AbstractTypeConstructor.isFinal

        override val isDenotable: Boolean
            get() = this@AbstractTypeConstructor.isDenotable

        override val declarationDescriptor
            get() = this@AbstractTypeConstructor.declarationDescriptor

        override val builtIns: CangJieBuiltIns
            get() = this@AbstractTypeConstructor.builtIns

        override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor =
            this@AbstractTypeConstructor.refine(cangjieTypeRefiner)

        override fun equals(other: Any?) = this@AbstractTypeConstructor.equals(other)
        override fun hashCode() = this@AbstractTypeConstructor.hashCode()
        override fun toString() = this@AbstractTypeConstructor.toString()
    }

    /**
     * 父类型存储类
     *
     * 在当前版本中，循环父类型的诊断会在位于循环上的每个顶点（父类型引用）上报告。
     * 为了实现这一点，我们存储两个版本的父类型——循环断开之前和之后。
     * 第一个版本用于计算父类型图中的邻居节点（见 Companion.computeNeighbours）
     *
     * @param allSupertypes 所有声明的父类型（包括循环边）
     */
    private class Supertypes(val allSupertypes: Collection<CangJieType>) {
        /**
         * 去除循环后的父类型列表
         * 初始化器仅用作占位符，用于在计算 'supertypes' 时调用 'getSupertypes' 的情况
         */
        var supertypesWithoutCycles: List<CangJieType> = listOf(ErrorUtils.errorTypeForLoopInSupertypes)
    }

    /**
     * 计算类型构造器的邻居节点
     *
     * 用于循环检测算法中的图遍历。
     *
     * @param useCompanions 是否包含通过伴随对象的边
     * @return 邻居节点对应的类型集合
     */
    private fun TypeConstructor.computeNeighbours(useCompanions: Boolean): Collection<CangJieType> =
        (this as? AbstractTypeConstructor)?.let { abstractClassifierDescriptor ->
            abstractClassifierDescriptor._supertypes().allSupertypes +
                    abstractClassifierDescriptor.getAdditionalNeighboursInSupertypeGraph(useCompanions)
        } ?: supertypes

    // ==================== 抽象方法 ====================

    /**
     * 计算所有声明的父类型
     *
     * 在循环检测之前调用，应该返回类型声明中直接指定的所有父类型。
     *
     * @return 所有声明的父类型集合
     */
    protected abstract fun computeSupertypes(): Collection<CangJieType>


    /**
     * 父类型循环检测器
     *
     * 用于检测和断开父类型图中的循环依赖。
     */
    protected abstract val supertypeLoopChecker: SupertypeLoopChecker

    // ==================== 可选覆盖的方法 ====================

    /**
     * 报告父类型循环错误
     *
     * 当检测到循环依赖时调用，子类应该实现此方法以向用户报告错误。
     *
     * @param type 参与循环的类型
     */
    protected open fun reportSupertypeLoopError(type: CangJieType) {}

    /**
     * 后处理无循环的父类型
     *
     * 在循环断开之后调用，子类可以覆盖此方法以执行额外的处理，
     * 例如排序、去重或其他转换。
     *
     * @param supertypes 已断开循环的父类型列表
     * @return 处理后的父类型列表
     */
    protected open fun processSupertypesWithoutCycles(supertypes: List<@JvmSuppressWildcards CangJieType>): List<CangJieType> =
        supertypes

    /**
     * 报告作用域循环错误
     *
     * 用于报告通过伴随对象的作用域循环错误。
     *
     * @param type 参与循环的类型
     */
    protected open fun reportScopesLoopError(type: CangJieType) {}

    /**
     * 是否应该报告伴随对象的循环警告
     *
     * 如果为 true，将执行额外的检查以检测通过伴随对象的循环。
     */
    protected open val shouldReportCyclicScopeWithCompanionWarning: Boolean = false

    /**
     * 获取父类型图中的额外邻居节点
     *
     * 用于在循环检测时添加额外的边，例如通过伴随对象的边。
     *
     * @param useCompanions 是否包含伴随对象相关的边
     * @return 额外的邻居节点类型集合
     */
    protected open fun getAdditionalNeighboursInSupertypeGraph(useCompanions: Boolean): Collection<CangJieType> =
        emptyList()

    /**
     * 父类型列表为空时的默认父类型
     *
     * 当类型没有显式声明父类型时，返回默认的父类型。
     * 例如，在仓颉语言中，除了 Any 自身，所有类默认继承 Any。
     *
     * @return 默认父类型，如果不需要默认父类型则返回 null
     */
    protected open fun defaultSupertypeIfEmpty(): CangJieType? = null

    // ==================== 调试支持 ====================

    /**
     * 渲染额外的调试信息
     *
     * 仅用于调试目的，返回父类型延迟值的调试信息。
     *
     * @return 调试信息字符串
     */
    fun renderAdditionalDebugInformation(): String = "supertypes=${_supertypes.renderDebugInformation()}"
}