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


package org.cangnova.cangjie.resolve.extend

import com.intellij.openapi.util.Key
import org.cangnova.cangjie.descriptors.ModuleCapability
import org.cangnova.cangjie.descriptors.ProjectDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeConstructor

/**
 * 扩展发现器接口
 *
 * 用于在 ExtendManager 中按需发现和解析扩展声明。
 * 当 ExtendManager 查询某个类型的扩展时，如果缓存为空，会调用发现器来查找并解析扩展。
 */
fun interface ExtensionDiscoverer {
    /**
     * 为指定的类型名称发现并解析扩展
     *
     * @param typeName 被扩展类型的短名称（如 "Int64"）
     */
    fun discoverExtensions(typeName: String)
}


/**
 * 扩展管理器接口
 *
 * 负责管理模块内的所有 extend 声明，提供类型扩展查询功能。
 * 当类型系统需要获取某个类型通过 extend 声明实现的接口时，通过此接口查询。
 */
interface ExtendManager {
    /**
     * 扩展定义
     *
     * @property id 扩展的唯一标识符
     * @property extendedConstructor 被扩展类型的类型构造器
     * @property typeParameters 扩展声明的类型参数
     * @property interfaces 扩展声明实现的接口列表
     * @property memberScope 扩展声明的成员作用域，包含扩展定义的方法和属性
     * @property descriptor 对应的扩展描述符（可选，用于反向查询）
     */
    data class ExtensionDef(
        val id: String,
        val extendedConstructor: TypeConstructor,
        val typeParameters: List<TypeParameterDescriptor>,
        val interfaces: List<CangJieType>,
        val memberScope: MemberScope? = null,
        val descriptor: Any? = null,  // 使用 Any 避免循环依赖，实际类型为 ExtendDescriptor
    )

    /**
     * 获取类型通过 extend 声明获得的超类型（实现的接口）
     *
     * @param forConstructor 目标类型的类型构造器
     * @param forTypeArgs 类型参数（用于泛型扩展）
     * @param excludeExtendId 要排除的扩展 ID（用于避免循环依赖）
     * @return 扩展声明提供的超类型集合
     */
    fun getExtendSupertypes(
        forConstructor: TypeConstructor,
        forTypeArgs: List<CangJieType> = emptyList(),
        excludeExtendId: String? = null,
    ): Collection<CangJieType>

    /**
     * 注册单个扩展定义
     *
     * 将扩展定义添加到管理器中。如果已存在相同 ID 的扩展，则替换。
     *
     * @param def 要注册的扩展定义
     */
    fun register(def: ExtensionDef)

    /**
     * 批量重建扩展定义
     *
     * 清除所有现有的扩展定义，并用新的定义集合替换。
     * 通常在模块完全重新解析时使用。
     *
     * @param defs 新的扩展定义集合
     */
    fun rebuild(defs: Collection<ExtensionDef>)

    /**
     * 使所有缓存失效
     *
     * 当代码发生变更时调用，清除所有扩展定义和缓存。
     */
    fun invalidate()

    /**
     * 获取指定类型构造器的所有扩展定义
     *
     * @param forConstructor 目标类型的类型构造器
     * @return 该类型的所有扩展定义
     */
    fun getExtensionsForType(forConstructor: TypeConstructor): Collection<ExtensionDef>

    /**
     * 获取模块中的所有扩展定义
     *
     * @return 模块中注册的所有扩展定义
     */
    fun getAllExtensions(): Collection<ExtensionDef>

    /**
     * 设置扩展发现器
     *
     * 发现器会在查询扩展时按需调用，用于延迟解析扩展声明。
     * 这是为了解决缓存失效后扩展未被重新解析的问题。
     *
     * @param discoverer 扩展发现器
     */
    fun setDiscoverer(discoverer: ExtensionDiscoverer?)

    /**
     * 注册待解析的扩展 PSI 元素
     *
     * 该方法用于预注册扩展声明的 PSI 元素，而不立即解析。
     * 当 [getExtensionsForType] 被调用时，如果有待解析的 PSI，会触发延迟解析。
     *
     * 这是延迟解析策略的一部分：
     * - 预加载时只注册 PSI（快速，不消耗资源）
     * - 使用时才触发完整解析（按需）
     *
     * @param typeName 被扩展类型的短名称（如 "Int64"）
     * @param psi PSI 元素（实际类型为 CjExtend，使用 Any 避免模块依赖）
     */
    fun registerPendingPsi(typeName: String, psi: Any)


}

