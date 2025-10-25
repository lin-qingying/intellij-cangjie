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

package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 模块能力（Module Capability）
 *
 * 模块能力是一种扩展机制，允许向 [ModuleDescriptor] 附加额外的元数据和功能，
 * 而无需修改 [ModuleDescriptor] 接口本身。每个能力都由一个唯一的类型和名称标识。
 *
 * ## 设计目的
 *
 * 1. **解耦扩展**: 允许不同的子系统向模块添加特定的元数据，而不影响核心模块描述符接口
 * 2. **类型安全**: 通过泛型参数 [T] 提供编译时类型检查
 * 3. **灵活性**: 支持任意类型的能力数据，从简单的标志到复杂的对象
 *
 * ## 使用示例
 *
 * ### 定义一个新的能力
 * ```kotlin
 * // 定义一个存储项目引用的能力
 * object ProjectCapability : ModuleCapability<Project>("Project")
 *
 * // 定义一个存储编译配置的能力
 * object CompilerConfigCapability : ModuleCapability<CompilerConfig>("CompilerConfig")
 * ```
 *
 * ### 在创建模块时设置能力
 * ```kotlin
 * val module = ModuleDescriptorImpl(
 *     moduleName = Name.identifier("myModule"),
 *     storageManager = storageManager,
 *     builtIns = builtIns,
 *     capabilities = mapOf(
 *         ProjectCapability to project,
 *         CompilerConfigCapability to compilerConfig
 *     )
 * )
 * ```
 *
 *
 * ## 内置能力
 *
 * - `PackageViewDescriptorFactory.CAPABILITY`: 自定义包视图描述符工厂
 *
 * ## 注意事项
 *
 * - 能力对象应该定义为单例 (object)，以确保类型唯一性
 * - 能力的名称应该具有描述性，便于调试和日志记录
 * - 能力数据一旦设置后通常不应该修改（遵循不可变性原则）
 *
 * @param T 能力所存储的数据类型
 * @param name 能力的名称，用于调试和日志记录
 *
 * @see ModuleDescriptor.getCapability
 * @see ProjectCapability
 */
class ModuleCapability<T>(val name: String) {
    override fun toString() = name
}

/**
 * 模块描述符接口，继承自声明描述符
 * 用于描述和管理仓颉语言的模块信息
 *
 * 模块是仓颉项目的组成部分，一个 [ProjectDescriptor] 包含多个 [ModuleDescriptor]。
 * 每个模块拥有自己的包结构、类型定义和依赖关系。
 */
interface ModuleDescriptor : DeclarationDescriptor{

    /**
     * 所属的仓颉项目
     * 每个模块都属于一个仓颉项目，模块不能独立存在
     */
    val projectDescriptor: ProjectDescriptor

    /** 模块是否有效 */
    val isValid: Boolean

    /**
     * 根据完全限定名获取包视图描述符
     * @param fqName 完全限定名
     * @return 包视图描述符
     */
    fun getPackage(fqName: FqName): PackageViewDescriptor

    /** 仓颉内置类型和函数集合 */
    /**
     * 便捷访问 BuiltIns
     */
    val builtIns: CangJieBuiltIns
        get() = projectDescriptor.builtIns

    /**
     * 获取指定包名下的所有子包
     * @param fqName 父包的完全限定名
     * @param nameFilter 名称过滤器函数
     * @return 子包的完全限定名集合
     */
    fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName>

    /** 包含此声明的父声明描述符，对于模块来说是它所属的 ProjectDescriptor */
    override val containingDeclaration: DeclarationDescriptor?
        get() = projectDescriptor as? DeclarationDescriptor

    /**
     * 接受访问者模式的访问
     * @param visitor 声明描述符访问者
     * @param data 传递给访问者的数据
     * @return 访问结果
     */
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitModuleDeclaration(this, data!!)
    }

    /** 依赖此模块的期望模块列表 */
    val expectedByModules: List<ModuleDescriptor>

    /**
     * 仓颉模块的稳定名称，可用于ABI（例如声明的名称修饰）
     */
    val stableName: Name?

    /**
     * 断言模块的有效性
     * 如果模块无效则抛出异常
     */
    fun assertValid()

    /**
     * 判断是否应该看到目标模块的内部成员
     * @param targetModule 目标模块
     * @return 是否可以访问内部成员
     */
    fun shouldSeeInternalsOf(targetModule: ModuleDescriptor): Boolean

    /**
     * 判断是否应该访问目标模块的受保护成员
     * @param targetModule 目标模块
     * @return 是否可以访问受保护成员
     */
    fun shouldProtectedsOf(targetModule: ModuleDescriptor): Boolean

    /**
     * 获取模块的特定能力
     * @param capability 能力类型
     * @return 能力实例，如果不存在则返回null
     */
    fun <T> getCapability(capability: ModuleCapability<T>): T?
}