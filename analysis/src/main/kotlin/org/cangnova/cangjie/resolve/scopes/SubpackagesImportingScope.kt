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
import org.cangnova.cangjie.descriptors.impl.SubpackagesScope
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.utils.Printer


/**
 * 子包导入作用域
 *
 * 这个类表示一个特殊的导入作用域,用于支持子包的访问和导入。
 * 它结合了 [SubpackagesScope] 的子包访问能力和 [ImportingScope] 的导入机制。
 *
 * ## 功能说明
 *
 * 这个作用域主要用于处理包的层级结构,允许在当前包中访问其子包的内容。
 * 例如,当你在 `com.example` 包中时,可以访问 `com.example.utils` 子包。
 *
 * ## 设计特点
 *
 * 1. **继承 SubpackagesScope**: 提供基本的子包查找功能
 * 2. **实现 ImportingScope**: 通过委托到 [ImportingScope.Empty] 提供空的导入功能
 * 3. **有限的导入**: 这个作用域主要关注包结构,不支持完整的符号导入
 *
 * ## 工作原理
 *
 * - 从 [SubpackagesScope] 继承子包访问逻辑
 * - 通过 `ImportingScope by ImportingScope.Empty` 委托,提供最小化的导入支持
 * - 重写关键方法以适配导入作用域的接口要求
 *
 * ## 使用场景
 *
 * ```cangjie
 * // 文件位于: com/example/Main.cj
 * package com.example
 *
 * // 可以访问子包 com.example.utils 中的内容
 * import com.example.utils.Helper
 *
 * // SubpackagesImportingScope 负责解析 utils 子包
 * ```
 *
 * ## 限制
 *
 * - 不支持完整的名称导入(通过 [ImportingScope.Empty] 委托)
 * - 主要用于包结构导航,而非符号解析
 * - 某些方法(如 `getContributedDescriptors` 的某些重载)返回空集合
 *
 * @property parent 父导入作用域,用于作用域链
 * @param moduleDescriptor 模块描述符,表示当前模块
 * @param fqName 完全限定名,表示当前包的路径
 *
 * @see SubpackagesScope 子包作用域的基类
 * @see ImportingScope 导入作用域的接口
 * @see PackageViewDescriptor 包视图描述符
 */
class SubpackagesImportingScope(
    override val parent: ImportingScope?,
    moduleDescriptor: ModuleDescriptor,
    fqName: FqName
) : SubpackagesScope(moduleDescriptor, fqName), ImportingScope by ImportingScope.Empty {

    /**
     * 获取贡献的包
     *
     * 根据名称查找子包,返回对应的包视图描述符。
     * 这个方法是 [ImportingScope] 接口的要求,委托给 [SubpackagesScope.getPackage]。
     *
     * @param name 包名称
     * @return 包视图描述符,如果不存在则返回 null
     */
    override fun getContributedPackage(name: Name): PackageViewDescriptor? = getPackage(name)

    /**
     * 打印作用域结构
     *
     * 用于调试和诊断,打印这个作用域的结构信息。
     * 委托给 [ImportingScope.printScopeStructure] 方法。
     *
     * @param p 打印器,用于输出格式化的文本
     */
    override fun printStructure(p: Printer) = printScopeStructure(p)

    /**
     * 获取贡献的变量
     *
     * 查找指定名称的变量描述符。
     * 这个方法委托给父类 [SubpackagesScope] 的实现。
     *
     * @param name 变量名称
     * @param location 查找位置,用于增量编译的依赖跟踪
     * @return 找到的变量描述符集合
     */
    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> = super.getContributedVariables(name, location)




    /**
     * 获取贡献的函数
     *
     * 查找指定名称的函数描述符。
     * 这个方法委托给父类 [SubpackagesScope] 的实现。
     *
     * @param name 函数名称
     * @param location 查找位置,用于增量编译的依赖跟踪
     * @return 找到的函数描述符集合
     */
    override fun getContributedFunctions(name: Name, location: LookupLocation) =
        super.getContributedFunctions(name, location)

    /**
     * 确定是否绝对不包含指定名称
     *
     * 快速判断作用域中是否肯定不存在指定名称的符号,用于优化查找性能。
     * 委托给 [SubpackagesScope] 的实现。
     *
     * @param name 要检查的名称
     * @return 如果确定不包含该名称返回 true,否则返回 false
     */
    override fun definitelyDoesNotContainName(name: Name): Boolean {
        return super<SubpackagesScope>.definitelyDoesNotContainName(name)
    }

    /**
     * 获取贡献的分类器
     *
     * 查找指定名称的分类器(类、接口、枚举等)描述符。
     * 委托给父类的实现。
     *
     * @param name 分类器名称
     * @param location 查找位置,用于增量编译的依赖跟踪
     * @return 找到的分类器描述符,如果不存在则返回 null
     */
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return super.getContributedClassifier(name, location)
    }

    /**
     * 获取贡献的描述符(基础版本)
     *
     * 根据类型过滤器和名称过滤器,获取作用域中的所有匹配描述符。
     * 委托给 [SubpackagesScope] 的实现。
     *
     * @param kindFilter 描述符类型过滤器(函数、类、变量等)
     * @param nameFilter 名称过滤函数,用于筛选符合条件的名称
     * @return 匹配的声明描述符集合
     */
    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return super<SubpackagesScope>.getContributedDescriptors(kindFilter, nameFilter)
    }


    /**
     * 获取贡献的描述符(扩展版本)
     *
     * 这是一个扩展版本的描述符查找方法,支持别名名称转换。
     *
     * **重要提示**: 当前实现返回空集合,保持了旧有行为。
     * 这看起来很奇怪,因为调用父类方法似乎更合适,但为了兼容性暂时保留此行为。
     *
     * TODO: 保留了旧行为,但看起来很奇怪(调用父类方法似乎更适用)
     *
     * @param kindFilter 描述符类型过滤器(函数、类、变量等)
     * @param nameFilter 名称过滤函数,用于筛选符合条件的名称
     * @param changeNamesForAliased 是否为别名改变名称
     * @return 匹配的声明描述符集合(当前总是返回空集合)
     */
    //TODO: kept old behavior, but it seems very strange (super call seems more applicable)
    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        changeNamesForAliased: Boolean
    ): Collection<DeclarationDescriptor> = emptyList()

    /**
     * 计算导入的名称
     *
     * 返回这个作用域中所有导入的名称集合。
     * 对于 [SubpackagesImportingScope],总是返回空集合,因为它不执行实际的符号导入。
     *
     * @return 导入的名称集合(总是为空)
     */
    override fun computeImportedNames() = emptySet<Name>()
}
