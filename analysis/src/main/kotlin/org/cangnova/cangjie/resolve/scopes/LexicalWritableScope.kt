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

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.utils.Printer

/**
 * 可写的词法作用域
 *
 * 该类表示一个可以动态添加声明的词法作用域。它主要用于在代码分析过程中
 * 逐步构建作用域，随着分析的深入，将新发现的声明添加到作用域中。
 *
 * ## 核心特性
 *
 * 1. **动态添加**: 可以在分析过程中添加变量、函数、类型等声明
 * 2. **冻结机制**: 可以冻结作用域，防止后续修改
 * 3. **快照支持**: 可以创建作用域的快照，保存某个时间点的状态
 * 4. **重声明检查**: 内置重声明检查机制，防止同名符号冲突
 *
 * ## 使用场景
 *
 * - **函数体分析**: 在分析函数体时，逐步添加局部变量声明
 * - **块作用域**: 为代码块（if/else、循环等）创建临时作用域
 * - **类成员收集**: 在分析类定义时，逐步添加成员声明
 * - **导入处理**: 处理导入语句时，将导入的符号添加到作用域
 *
 * ## 生命周期
 *
 * ```
 * 1. 创建可写作用域
 *   ↓
 * 2. 分析代码，逐步添加声明
 *   - addVariableDescriptor()
 *   - addFunctionDescriptor()
 *   - addClassifierDescriptor()
 *   ↓
 * 3. 可选：创建快照保存当前状态
 *   - takeSnapshot()
 *   ↓
 * 4. 冻结作用域，防止进一步修改
 *   - freeze()
 *   ↓
 * 5. 使用冻结后的作用域进行符号查找
 * ```
 *
 * ## 与父类的关系
 *
 * 继承自 [LexicalScopeStorage]，复用了存储和查找逻辑，
 * 同时添加了写入控制（freeze）和快照功能。
 *
 * ## 示例
 *
 * ```kotlin
 * // 创建可写作用域
 * val scope = LexicalWritableScope(
 *     parent = parentScope,
 *     ownerDescriptor = functionDescriptor,
 *     isOwnerDescriptorAccessibleByLabel = true,
 *     redeclarationChecker = redeclarationChecker,
 *     kind = LexicalScopeKind.FUNCTION_INNER_SCOPE
 * )
 *
 * // 添加局部变量
 * scope.addVariableDescriptor(localVar1)
 * scope.addVariableDescriptor(localVar2)
 *
 * // 创建快照（保存当前状态）
 * val snapshot = scope.takeSnapshot()
 *
 * // 继续添加更多变量
 * scope.addVariableDescriptor(localVar3)
 *
 * // 冻结作用域
 * scope.freeze()
 *
 * // 之后尝试添加会抛出异常
 * // scope.addVariableDescriptor(localVar4) // IllegalStateException!
 * ```
 *
 * @param parent 父作用域，用于符号查找的链式查找
 * @param ownerDescriptor 作用域的所有者描述符（如函数、类等）
 * @param isOwnerDescriptorAccessibleByLabel 所有者是否可以通过标签访问（用于标签引用）
 * @param redeclarationChecker 重声明检查器，用于检测同名符号冲突
 * @param kind 作用域类型，用于调试和日志
 *
 * @see LexicalScopeStorage
 * @see LexicalScope
 * @see Snapshot
 */
class LexicalWritableScope(
    parent: LexicalScope,
    override val ownerDescriptor: DeclarationDescriptor,
    override val isOwnerDescriptorAccessibleByLabel: Boolean,
    redeclarationChecker: LocalRedeclarationChecker,
    override val kind: LexicalScopeKind
) : LexicalScopeStorage(parent, redeclarationChecker) {
    /**
     * 隐式接收者
     *
     * 对于可写作用域，通常没有隐式接收者。
     * 隐式接收者主要用于类成员作用域（this）和扩展函数（扩展接收者）。
     *
     * @return 总是返回 null
     */
    override val implicitReceiver: ReceiverParameterDescriptor?
        get() = null

    /**
     * 上下文接收者组
     *
     * 上下文接收者用于支持上下文相关的函数调用。
     * 对于普通的可写作用域，不包含上下文接收者。
     *
     * @return 总是返回空列表
     */
//    override fun getContributedPackageFqName(name: Name, location: LookupLocation): List<FqName> = emptyList()

    /**
     * 是否可写标志
     *
     * - true: 可以添加新的声明
     * - false: 已冻结，不能再添加声明
     */
    private var canWrite: Boolean = true

    /**
     * 最后一次创建的快照
     *
     * 缓存最近的快照以优化性能，避免重复创建相同的快照。
     */
    private var lastSnapshot: Snapshot? = null

    /**
     * 冻结作用域
     *
     * 将作用域设置为只读状态，之后任何添加声明的操作都会抛出异常。
     * 这是一个单向操作，一旦冻结就无法解冻。
     *
     * ## 使用场景
     *
     * - **分析完成**: 当某个作用域的分析完全结束后，冻结以防止意外修改
     * - **传递作用域**: 在将作用域传递给其他组件前冻结，确保数据一致性
     * - **调试辅助**: 在调试时冻结作用域，确保状态不会被意外改变
     *
     * ## 示例
     *
     * ```kotlin
     * val scope = LexicalWritableScope(...)
     * scope.addVariableDescriptor(var1)
     * scope.addVariableDescriptor(var2)
     *
     * // 分析完成，冻结作用域
     * scope.freeze()
     *
     * // 之后的添加操作会失败
     * scope.addVariableDescriptor(var3) // IllegalStateException
     * ```
     *
     * ## 线程安全
     *
     * 注意：此方法不是线程安全的。如果在多线程环境中使用，
     * 需要外部同步机制。
     */
    fun freeze() {
        canWrite = false
    }

    /**
     * 作用域快照
     *
     * 该内部类捕获词法作用域在某个特定时间点的状态。
     * 快照限制了可见的描述符数量，只暴露创建快照时已经添加的描述符。
     *
     * ## 核心特性
     *
     * - **不可变视图**: 快照创建后，即使原作用域继续添加声明，快照也不会改变
     * - **描述符限制**: 只暴露快照创建时的前 N 个描述符
     * - **委托模式**: 大部分功能委托给外部作用域，只覆盖查找相关方法
     *
     * ## 使用场景
     *
     * - **增量分析**: 在分析过程中需要保存中间状态
     * - **回溯**: 当分析出错时，可以回退到之前的快照
     * - **并发分析**: 不同的分析线程可以使用不同的快照
     * - **条件分支**: if/else 分支可能需要不同的作用域视图
     *
     * ## 示例
     *
     * ```kotlin
     * val scope = LexicalWritableScope(...)
     * scope.addVariableDescriptor(var1)
     * scope.addVariableDescriptor(var2)
     *
     * // 创建快照 - 只包含 var1 和 var2
     * val snapshot1 = scope.takeSnapshot()
     *
     * // 继续添加
     * scope.addVariableDescriptor(var3)
     *
     * // 在 snapshot1 中查找 var3 会找不到
     * snapshot1.getContributedVariables(Name.identifier("var3"), location) // 空
     *
     * // 但在原作用域中可以找到
     * scope.getContributedVariables(Name.identifier("var3"), location) // var3
     * ```
     *
     * @param descriptorLimit 描述符数量限制，只暴露前 N 个描述符
     */
    private inner class Snapshot(val descriptorLimit: Int) : LexicalScope by this {

        /**
         * 获取符合过滤条件的贡献描述符
         *
         * 只返回快照创建时已存在的描述符（前 descriptorLimit 个）。
         *
         * @param kindFilter 描述符种类过滤器（未使用，保持接口一致性）
         * @param nameFilter 名称过滤器（未使用，保持接口一致性）
         * @return 快照中的所有描述符
         */
        override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) =
            addedDescriptors.subList(0, descriptorLimit)

        /**
         * 获取指定名称的分类器描述符
         *
         * 在快照范围内查找分类器（类、接口、类型别名等）。
         *
         * @param name 分类器名称
         * @param location 查找位置（用于诊断）
         * @return 找到的分类器，或 null
         */
        override fun getContributedClassifier(name: Name, location: LookupLocation) =
            variableOrClassDescriptorByName(name, descriptorLimit) as? ClassifierDescriptor

        /**
         * 获取包括已弃用的分类器描述符
         *
         * **重要**: 此方法必须显式重写！
         *
         * 如果不重写，调用会委托给 `this` 代理（外部 LexicalWritableScope），
         * 这会使用 ResolutionScope 的默认实现，而不是在快照上调用
         * `getContributedClassifier`，导致快照的描述符限制失效。
         *
         * ## 设计说明
         *
         * Kotlin 的委托模式（by this）会将所有接口方法委托给被委托的对象，
         * 但我们需要某些方法（如符号查找）使用快照的限制版本。
         * 因此必须显式覆盖这些方法。
         *
         * @param name 分类器名称
         * @param location 查找位置
         * @return 包装了弃用信息的分类器描述符
         */
        override fun getContributedClassifierIncludeDeprecated(
            name: Name,
            location: LookupLocation
        ): DescriptorWithDeprecation<ClassifierDescriptor>? {
            return (variableOrClassDescriptorByName(name, descriptorLimit) as? ClassifierDescriptor)
                ?.let { DescriptorWithDeprecation.createNonDeprecated(it) }
        }

        /**
         * 获取指定名称的变量描述符
         *
         * 在快照范围内查找变量（局部变量、参数等）。
         *
         * @param name 变量名称
         * @param location 查找位置
         * @return 找到的变量描述符集合（通常为单个元素或空）
         */
        override fun getContributedVariables(
            name: Name,
            location: LookupLocation
        ): Collection<@JvmWildcard VariableDescriptor> =
            listOfNotNull(variableOrClassDescriptorByName(name, descriptorLimit) as? VariableDescriptor)

        /**
         * 获取指定名称的函数描述符
         *
         * 在快照范围内查找函数。
         *
         * @param name 函数名称
         * @param location 查找位置
         * @return 找到的函数描述符集合
         */
        override fun getContributedFunctions(name: Name, location: LookupLocation) =
            functionsByName(name, descriptorLimit)

        /**
         * 返回快照的字符串表示
         *
         * 用于调试和日志输出。
         *
         * @return 快照的描述字符串
         */
        override fun toString(): String = "Snapshot($descriptorLimit) for $kind"

        /**
         * 打印快照的结构信息
         *
         * 用于调试，输出快照的详细结构。
         *
         * @param p 打印器对象
         */
        override fun printStructure(p: Printer) {
            p.println("Snapshot with descriptorLimit = $descriptorLimit for scope:")
            this@LexicalWritableScope.printStructure(p)
        }
    }

    /**
     * 创建作用域的快照
     *
     * 捕获当前作用域的状态，返回一个不可变的视图。
     * 即使之后继续向作用域添加声明，快照也保持不变。
     *
     * ## 优化策略
     *
     * 为了性能优化，此方法会缓存最后一次创建的快照。
     * 如果自上次快照以来没有添加新的描述符，则返回缓存的快照。
     *
     * ## 使用场景
     *
     * - **条件分析**: 在分析 if/else 等分支时，保存分支前的状态
     * - **循环分析**: 在分析循环体前创建快照，用于处理变量的作用域
     * - **错误恢复**: 当分析出错时，可以回退到快照状态
     * - **增量分析**: 保存中间状态，支持增量更新
     *
     * ## 示例
     *
     * ```kotlin
     * val scope = LexicalWritableScope(...)
     *
     * // 添加一些变量
     * scope.addVariableDescriptor(var1)
     * scope.addVariableDescriptor(var2)
     *
     * // 创建快照
     * val beforeBranch = scope.takeSnapshot()
     *
     * // 分析 if 分支，可能添加临时变量
     * scope.addVariableDescriptor(tempVar)
     *
     * // 回到快照状态（通过重新创建作用域）
     * // 或者使用快照作为不同分支的基础
     * ```
     *
     * @return 当前作用域的快照
     */
    fun takeSnapshot(): LexicalScope {
        if (lastSnapshot == null || lastSnapshot!!.descriptorLimit != addedDescriptors.size) {
            lastSnapshot = Snapshot(addedDescriptors.size)
        }
        return lastSnapshot!!
    }

    /**
     * 添加变量描述符
     *
     * 将变量（局部变量、参数等）添加到作用域中。
     *
     * ## 检查
     *
     * - **可写性检查**: 如果作用域已冻结，抛出异常
     * - **重声明检查**: 通过 redeclarationChecker 检查同名冲突
     *
     * @param variableDescriptor 要添加的变量描述符
     * @throws IllegalStateException 如果作用域已冻结
     */
    override fun addVariableDescriptor(variableDescriptor: VariableDescriptor) {
        checkMayWrite()
        addVariableOrClassDescriptor(variableDescriptor)
    }

    /**
     * 添加函数描述符
     *
     * 将函数（局部函数、lambda 等）添加到作用域中。
     *
     * ## 检查
     *
     * - **可写性检查**: 如果作用域已冻结，抛出异常
     * - **重声明检查**: 检查同名函数冲突（支持重载）
     *
     * @param functionDescriptor 要添加的函数描述符
     * @throws IllegalStateException 如果作用域已冻结
     */
    fun addFunctionDescriptor(functionDescriptor: FunctionDescriptor) {
        checkMayWrite()
        addFunctionDescriptorInternal(functionDescriptor)
    }

    /**
     * 添加分类器描述符
     *
     * 将分类器（类、接口、类型别名等）添加到作用域中。
     *
     * ## 检查
     *
     * - **可写性检查**: 如果作用域已冻结，抛出异常
     * - **重声明检查**: 检查同名类型冲突
     *
     * @param classifierDescriptor 要添加的分类器描述符
     * @throws IllegalStateException 如果作用域已冻结
     */
    fun addClassifierDescriptor(classifierDescriptor: ClassifierDescriptor) {
        checkMayWrite()
        addVariableOrClassDescriptor(classifierDescriptor)
    }

    /**
     * 返回作用域的字符串表示
     *
     * @return 作用域类型的字符串
     */
    override fun toString(): String = kind.toString()

    /**
     * 打印作用域的结构信息
     *
     * 用于调试，输出作用域的详细信息，包括：
     * - 作用域类型
     * - 所有者描述符
     * - 隐式接收者
     * - 上下文接收者
     * - 父作用域
     *
     * @param p 打印器对象
     */
    override fun printStructure(p: Printer) {
        p.println(
            this::class.java.simpleName,
            ": ",
            kind,
            "; for descriptor: ",
            ownerDescriptor.name,
            " with implicitReceivers: ",
            implicitReceiver?.value ?: "NONE",
            " {"
        )
        p.pushIndent()

        p.print("parent = ")
        parent.printStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }

    /**
     * 检查是否可以写入
     *
     * 内部辅助方法，用于在添加描述符前检查作用域是否已冻结。
     *
     * @throws IllegalStateException 如果作用域已冻结
     */
    private fun checkMayWrite() {
        if (!canWrite) {
            throw IllegalStateException("Cannot write into freezed scope:" + toString())
        }
    }

}
