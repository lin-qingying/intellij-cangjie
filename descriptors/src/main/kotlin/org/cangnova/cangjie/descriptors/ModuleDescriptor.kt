/*
 * Copyright 2024 LinQingYing. and contributors.
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
 * 模块能力类，用于表示模块的特定功能或特性
 * @param T 能力的类型参数
 * @param name 能力的名称
 */
class ModuleCapability<T>(val name: String) {
    override fun toString() = name
}

/**
 * 模块描述符接口，继承自声明描述符
 * 用于描述和管理仓颉语言的模块信息
 */
interface ModuleDescriptor : DeclarationDescriptor{
    /** 模块是否有效 */
    val isValid: Boolean

    /**
     * 根据完全限定名获取包视图描述符
     * @param fqName 完全限定名
     * @return 包视图描述符
     */
    fun getPackage(fqName: FqName): PackageViewDescriptor

    /** 仓颉内置类型和函数集合 */
    val builtIns: CangJieBuiltIns

    /**
     * 获取指定包名下的所有子包
     * @param fqName 父包的完全限定名
     * @param nameFilter 名称过滤器函数
     * @return 子包的完全限定名集合
     */
    fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName>

    /** 包含此声明的父声明描述符，对于模块来说总是null */
    override val containingDeclaration: DeclarationDescriptor?
        get() = null

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