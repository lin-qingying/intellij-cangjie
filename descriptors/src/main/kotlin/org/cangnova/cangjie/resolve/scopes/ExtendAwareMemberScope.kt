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
import org.cangnova.cangjie.descriptors.extend.ExtendDescriptor
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.extend.ExtendManager
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ComposableTypeSubstitutor
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.utils.Printer

/**
 * 扩展感知的成员作用域包装器
 *
 * 该类实现装饰器模式（Decorator Pattern），在原始作用域的基础上动态添加
 * 来自 extend 声明的成员。当查询成员时，会同时从原始作用域和 ExtendManager
 * 中注册的扩展定义获取结果。
 *
 * ## 设计目标
 *
 * 1. **透明性**: 对调用者透明，通过标准 MemberScope 接口即可获取所有成员（包括扩展成员）
 * 2. **延迟查询**: 仅在需要时才从 ExtendManager 获取扩展成员
 * 3. **增量友好**: ExtendManager 更新会自动反映到查询结果中
 * 4. **类型替换**: 自动处理泛型扩展的类型参数替换
 *
 * ## 使用场景
 *
 * ```cangjie
 * class Array<T> { ... }
 *
 * extend<T> Array<T> <: Printable {
 *     func print() { ... }
 * }
 *
 * // 通过 ExtendAwareMemberScope，Array<T> 的 memberScope 可以找到 print() 方法
 * ```
 *
 * ## 架构集成
 *
 * ```
 * 类型描述符
 *   ↓
 * ExtendAwareMemberScope (包装器)
 *   ├── 原始 MemberScope (类自身的成员)
 *   └── ExtendManager 查询 (扩展声明的成员)
 * ```
 *
 * ## 性能考虑
 *
 * - 名称集合属性采用惰性计算
 * - 对于没有扩展的类型，开销仅为一次 ExtendManager 查询
 * - 可在子类中添加缓存机制进一步优化
 *
 * @param originalScope 原始作用域，包含类型自身的成员
 * @param typeConstructor 类型构造器，用于从 ExtendManager 查询扩展定义
 * @param typeArguments 类型参数，用于泛型扩展的类型替换
 * @param module 模块描述符，提供 ExtendManager 访问能力
 *
 * @see ExtendManager 扩展管理器，存储所有扩展定义
 * @see ChainedMemberScope 链式作用域，类似的组合模式实现
 */
class ExtendAwareMemberScope(
    private val originalScope: MemberScope,
    private val typeConstructor: TypeConstructor,
    private val typeArguments: List<CangJieType>,
    private val module: ModuleDescriptor
) : MemberScope {

    /**
     * 获取 ExtendManager 实例
     *
     * 通过模块能力（Capability）获取扩展管理器。
     * 如果模块不支持扩展管理器，返回 null。
     */
    private val extendManager: ExtendManager?
        get() = module.getCapability(ExtendManager.CAPABILITY)

    /**
     * 获取当前类型的所有扩展定义
     *
     * 使用 ThreadLocal 递归保护，防止在类型构建和成员查找过程中
     * 出现循环依赖导致的 StackOverflowError。
     *
     * 循环场景：类型创建 → ExtendAwareMemberScope → 查询扩展 →
     * 延迟解析扩展声明 → 解析扩展的目标类型 → 类型创建（循环）
     */
    private fun getExtensionDefs(): Collection<ExtendManager.ExtensionDef> {
        val querying = queryingTypeConstructors.get()
        if (!querying.add(typeConstructor)) {
            // 已经在查询该类型构造器的扩展，返回空以打断循环
            return emptyList()
        }
        return try {
            extendManager?.getExtensionsForType(typeConstructor) ?: emptyList()
        } finally {
            querying.remove(typeConstructor)
        }
    }

    /**
     * 构建类型替换器
     *
     * 将扩展定义的类型参数替换为实际的类型参数。
     * 例如：extend<T> Array<T> 中的 T 替换为 Array<String> 中的 String
     */
    private fun buildSubstitutor(
        extTypeParams: List<TypeParameterDescriptor>
    ): ComposableTypeSubstitutor {
        val map = extTypeParams.zip(typeArguments).associate { (param, arg) ->
            param.typeConstructor to arg.unwrap()
        }
        return ComposableTypeSubstitutor.create(map)
    }

    /**
     * 从扩展作用域获取成员
     *
     * @param getter 获取函数，用于从单个扩展作用域获取成员
     * @return 所有扩展作用域中的成员集合
     */
    private inline fun <T> getFromExtends(
        getter: (MemberScope, ComposableTypeSubstitutor) -> Collection<T>
    ): Collection<T> {
        val defs = getExtensionDefs()
        if (defs.isEmpty()) return emptyList()

        val result = mutableListOf<T>()
        for (def in defs) {
            val scope = def.memberScope ?: continue
            val substitutor = buildSubstitutor(def.typeParameters)
            result.addAll(getter(scope, substitutor))
        }
        return result
    }

    // ==================== MemberScope 接口实现 ====================

    override val functionNames: Set<Name>
        get() {
            val original = originalScope.functionNames
            val fromExtends = getExtensionDefs().flatMapTo(mutableSetOf()) {
                it.memberScope?.functionNames ?: emptySet()
            }
            return if (fromExtends.isEmpty()) original else original + fromExtends
        }

    override val variableNames: Set<Name>
        get() {
            val original = originalScope.variableNames
            val fromExtends = getExtensionDefs().flatMapTo(mutableSetOf()) {
                it.memberScope?.variableNames ?: emptySet()
            }
            return if (fromExtends.isEmpty()) original else original + fromExtends
        }

    override val propertyNames: Set<Name>
        get() {
            val original = originalScope.propertyNames
            val fromExtends = getExtensionDefs().flatMapTo(mutableSetOf()) {
                it.memberScope?.propertyNames ?: emptySet()
            }
            return if (fromExtends.isEmpty()) original else original + fromExtends
        }

    override val classifierNames: Set<Name>?
        get() = originalScope.classifierNames

    override fun getContributedFunctions(
        name: Name,
        location: LookupLocation
    ): Collection<SimpleFunctionDescriptor> {
        val original = originalScope.getContributedFunctions(name, location)
        val fromExtends = getFromExtends { scope, substitutor ->
            scope.getContributedFunctions(name, location).map { func ->
                // 对函数应用类型替换
                @Suppress("UNCHECKED_CAST")
                func.substitute(substitutor) as? SimpleFunctionDescriptor ?: func
            }
        }
        return if (fromExtends.isEmpty()) original else original + fromExtends
    }

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<VariableDescriptor> {
        val original = originalScope.getContributedVariables(name, location)
        val fromExtends = getFromExtends { scope, substitutor ->
            scope.getContributedVariables(name, location).mapNotNull { variable ->
                variable.substitute(substitutor) as? VariableDescriptor ?: variable
            }
        }
        return if (fromExtends.isEmpty()) original else original + fromExtends
    }

    override fun getContributedPropertys(
        name: Name,
        location: LookupLocation
    ): Collection<PropertyDescriptor> {
        val original = originalScope.getContributedPropertys(name, location)
        val fromExtends = getFromExtends { scope, substitutor ->
            scope.getContributedPropertys(name, location).map { property ->
                @Suppress("UNCHECKED_CAST")
                property.substitute(substitutor) ?: property
            }
        }
        return if (fromExtends.isEmpty()) original else original + fromExtends
    }

    override fun getContributedMacros(
        name: Name,
        location: LookupLocation
    ): Collection<MacroDescriptor> {
        val original = originalScope.getContributedMacros(name, location)
        val fromExtends = getFromExtends { scope, _ ->
            scope.getContributedMacros(name, location)
        }
        return if (fromExtends.isEmpty()) original else original + fromExtends
    }

    override fun getContributedClassifier(
        name: Name,
        location: LookupLocation
    ): ClassifierDescriptor? {
        // 扩展不能定义新的分类器（类、接口等），直接委托给原始作用域
        return originalScope.getContributedClassifier(name, location)
    }

    override fun getContributedClassifiers(
        name: Name,
        location: LookupLocation
    ): List<ClassifierDescriptor> {
        return originalScope.getContributedClassifiers(name, location)
    }

    override fun getContributedExtend(
        extendId: String,
        location: LookupLocation
    ): ExtendDescriptor? {
        return originalScope.getContributedExtend(extendId, location)
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        val original = originalScope.getContributedDescriptors(kindFilter, nameFilter)

        // 如果不需要函数、变量或属性，直接返回原始结果
        if (!kindFilter.acceptsKinds(DescriptorKindFilter.CALLABLES.kindMask)) {
            return original
        }

        val fromExtends = getFromExtends { scope, substitutor ->
            scope.getContributedDescriptors(kindFilter, nameFilter).mapNotNull { descriptor ->
                when (descriptor) {
                    is SimpleFunctionDescriptor -> {
                        @Suppress("UNCHECKED_CAST")
                        descriptor.substitute(substitutor) as? DeclarationDescriptor
                    }
                    is PropertyDescriptor -> {
                        @Suppress("UNCHECKED_CAST")
                        descriptor.substitute(substitutor) as? DeclarationDescriptor
                    }
                    is VariableDescriptor -> {
                        descriptor.substitute(substitutor) as? DeclarationDescriptor
                    }
                    else -> descriptor
                }
            }
        }

        return if (fromExtends.isEmpty()) original else original + fromExtends
    }

    override fun definitelyDoesNotContainName(name: Name): Boolean {
        // 如果原始作用域可能包含该名称，返回 false
        if (!originalScope.definitelyDoesNotContainName(name)) {
            return false
        }
        // 检查扩展作用域
        return getExtensionDefs().all { def ->
            def.memberScope?.definitelyDoesNotContainName(name) ?: true
        }
    }

    override fun recordLookup(name: Name, location: LookupLocation) {
        originalScope.recordLookup(name, location)
        // 记录扩展作用域的查找
        getExtensionDefs().forEach { def ->
            def.memberScope?.recordLookup(name, location)
        }
    }

    override fun printScopeStructure(p: Printer) {
        p.println("ExtendAwareMemberScope for $typeConstructor {")
        p.pushIndent()

        p.println("Original scope:")
        p.pushIndent()
        originalScope.printScopeStructure(p)
        p.popIndent()

        val defs = getExtensionDefs()
        if (defs.isNotEmpty()) {
            p.println("Extend scopes (${defs.size}):")
            p.pushIndent()
            for (def in defs) {
                p.println("- ${def.id}")
                def.memberScope?.let {
                    p.pushIndent()
                    it.printScopeStructure(p)
                    p.popIndent()
                }
            }
            p.popIndent()
        }

        p.popIndent()
        p.println("}")
    }

    override fun toString(): String {
        return "ExtendAwareMemberScope($typeConstructor)"
    }

    companion object {
        /**
         * ThreadLocal 递归保护集合
         *
         * 记录当前线程正在查询扩展的类型构造器，防止循环依赖：
         * 类型创建 → 查询扩展 → 延迟解析 → 类型创建 → 查询扩展（循环）
         */
        private val queryingTypeConstructors: ThreadLocal<MutableSet<TypeConstructor>> =
            ThreadLocal.withInitial { mutableSetOf() }

        /**
         * 创建扩展感知的成员作用域
         *
         * 如果模块支持 ExtendManager，则包装原始作用域以支持 extend 声明添加的成员。
         * 注意：不在此处查询具体的扩展定义，避免在类型构建过程中触发延迟解析导致栈溢出。
         * 扩展定义的查询延迟到实际成员查找时进行。
         *
         * @param originalScope 原始作用域
         * @param typeConstructor 类型构造器
         * @param typeArguments 类型参数
         * @param module 模块描述符
         * @return 包装后的作用域，或原始作用域（如果模块不支持扩展）
         */
        fun createIfNeeded(
            originalScope: MemberScope,
            typeConstructor: TypeConstructor,
            typeArguments: List<CangJieType>,
            module: ModuleDescriptor
        ): MemberScope {
            // 仅检查 ExtendManager 是否存在，不查询扩展定义
            // 避免在类型构建过程中触发延迟解析导致 StackOverflowError
            val hasExtendManager = module.getCapability(ExtendManager.CAPABILITY) != null

            return if (hasExtendManager) {
                ExtendAwareMemberScope(originalScope, typeConstructor, typeArguments, module)
            } else {
                originalScope
            }
        }
    }
}
