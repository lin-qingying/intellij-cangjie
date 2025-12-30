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

package org.cangnova.cangjie.cache

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

/**
 * 模块级缓存工具函数
 *
 * 这些扩展函数为 [Module] 提供了便捷的缓存机制，基于 IntelliJ Platform 的
 * [CachedValuesManager] 系统。缓存会自动根据依赖项的变化进行失效和重新计算。
 */

/**
 * 在模块中缓存计算结果，当项目根目录发生修改时自动失效
 *
 * 该方法是一个便捷的缓存函数，专门用于缓存那些依赖于项目结构的计算结果。
 * 当项目根目录发生任何修改（如添加/删除依赖、修改模块结构）时，缓存会自动失效。
 *
 * ## 使用场景
 *
 * - **依赖解析**: 缓存模块的依赖列表，当项目结构变化时重新计算
 * - **库信息**: 缓存库的元数据，当库被添加/移除时更新
 * - **模块配置**: 缓存模块的编译配置，当配置文件变化时刷新
 * - **源码根路径**: 缓存源码根目录，当项目结构变化时更新
 *
 * ## 失效条件
 *
 * 缓存会在以下情况下自动失效：
 * - 添加或删除模块
 * - 修改模块的依赖关系
 * - 更改源码根目录
 * - 添加或删除库
 * - 修改项目的 SDK 配置
 *
 * ## 缓存键
 *
 * 使用 `classForKey` 作为缓存键，确保不同类型的缓存不会冲突。
 * 通常传入一个唯一的 Class 对象（如私有的伴生对象类）。
 *
 * ## 性能优化
 *
 * - 避免在 provider 中执行耗时的 I/O 操作，除非必要
 * - provider 应该是纯函数，不应有副作用
 * - 缓存的数据应该是不可变的，避免并发问题
 *
 * ## 示例
 *
 * ```kotlin
 * // 缓存模块的依赖列表
 * private object DependenciesKey
 *
 * fun Module.getDependencies(): List<Module> {
 *     return cacheByClassInvalidatingOnRootModifications(DependenciesKey::class.java) {
 *         // 计算依赖列表（仅在缓存失效时执行）
 *         ModuleRootManager.getInstance(this).getDependencies().toList()
 *     }
 * }
 * ```
 *
 * @param T 缓存值的类型
 * @param classForKey 用作缓存键的 Class 对象，应该是唯一的
 * @param provider 缓存值的提供函数，仅在缓存不存在或失效时调用
 * @return 缓存的值（可能是新计算的，也可能来自缓存）
 *
 * @see ProjectRootModificationTracker
 * @see cacheByClass
 */
fun <T> Module.cacheByClassInvalidatingOnRootModifications(classForKey: Class<*>, provider: () -> T): T {
    
    
    return cacheByClass(classForKey, ProjectRootModificationTracker.getInstance(project), provider = provider)
}

/**
 * 在模块中缓存计算结果，支持自定义依赖项
 *
 * 该方法提供了更灵活的缓存机制，允许指定任意数量的依赖项。
 * 当任何一个依赖项发生变化时，缓存都会失效并重新计算。
 *
 * ## 依赖项类型
 *
 * 支持的依赖项类型包括：
 * - **ModificationTracker**: 追踪特定变化（如 [ProjectRootModificationTracker]）
 * - **PsiElement**: 追踪 PSI 元素的变化
 * - **VirtualFile**: 追踪文件内容的变化
 * - **PsiFile**: 追踪整个文件的变化
 * - 其他实现了合适接口的对象
 *
 * ## 使用场景
 *
 * - **文件相关缓存**: 依赖于特定文件内容的计算
 * - **PSI 相关缓存**: 依赖于 PSI 树结构的分析结果
 * - **复杂依赖**: 需要多个不同类型依赖项的缓存
 * - **细粒度控制**: 需要精确控制缓存失效条件
 *
 * ## 缓存键的选择
 *
 * 推荐使用以下方式创建唯一的缓存键：
 * ```kotlin
 * // 方式 1: 使用私有对象
 * private object MyDataKey
 * cacheByClass(MyDataKey::class.java, ...) { ... }
 *
 * // 方式 2: 使用数据类的 Class
 * data class MyData(...)
 * cacheByClass(MyData::class.java, ...) { MyData(...) }
 * ```
 *
 * ## 示例
 *
 * ```kotlin
 * // 示例 1: 依赖于特定文件的缓存
 * private object ImportsKey
 *
 * fun Module.getImportsFromFile(file: PsiFile): List<Import> {
 *     return cacheByClass(ImportsKey::class.java, file) {
 *         // 解析文件中的导入语句
 *         parseImports(file)
 *     }
 * }
 *
 * // 示例 2: 依赖于多个条件的缓存
 * private object ConfigKey
 *
 * fun Module.getConfig(configFile: VirtualFile): Config {
 *     return cacheByClass(
 *         ConfigKey::class.java,
 *         configFile,  // 依赖于配置文件内容
 *         ProjectRootModificationTracker.getInstance(project)  // 依赖于项目结构
 *     ) {
 *         // 解析配置
 *         parseConfig(configFile)
 *     }
 * }
 * ```
 *
 * @param T 缓存值的类型
 * @param classForKey 用作缓存键的 Class 对象，应该是唯一的
 * @param dependencies 可变数量的依赖项，任何一个变化都会导致缓存失效
 * @param provider 缓存值的提供函数，仅在缓存不存在或失效时调用
 * @return 缓存的值（可能是新计算的，也可能来自缓存）
 *
 * @see CachedValuesManager
 * @see CachedValueProvider
 */
fun <T> Module.cacheByClass(classForKey: Class<*>, vararg dependencies: Any, provider: () -> T): T {
    return CachedValuesManager.getManager(project).cache(this, dependencies, classForKey, provider)
}

/**
 * CachedValuesManager 的内部缓存实现
 *
 * 该私有方法是缓存机制的核心实现，负责与 IntelliJ Platform 的
 * [CachedValuesManager] 系统交互。
 *
 * ## 工作原理
 *
 * 1. **获取缓存管理器**: 从 CachedValuesManager 获取缓存实例
 * 2. **生成缓存键**: 基于 classForKey 生成唯一的键
 * 3. **检查缓存**: 检查缓存是否存在且有效
 * 4. **计算或返回**:
 *    - 缓存有效: 直接返回缓存值
 *    - 缓存失效/不存在: 调用 provider 计算新值并缓存
 * 5. **注册依赖**: 将依赖项注册到缓存系统，用于失效检测
 *
 * ## 线程安全
 *
 * IntelliJ Platform 的缓存系统是线程安全的，多个线程可以同时访问缓存。
 * 如果多个线程同时请求同一个不存在的缓存，provider 可能会被调用多次，
 * 但最终只有一个结果会被缓存。
 *
 * ## 内存管理
 *
 * 缓存使用软引用（Soft Reference）存储，当内存不足时会被自动清理。
 * 因此，即使缓存未失效，在内存压力下也可能需要重新计算。
 *
 * @param T 缓存值的类型
 * @param holder 缓存的持有者（通常是 Module、PsiFile 等）
 * @param dependencies 依赖项数组，用于失效检测
 * @param classForKey 用作缓存键的 Class 对象
 * @param provider 缓存值的提供函数
 * @return 缓存的值
 */
private fun <T> CachedValuesManager.cache(
    holder: UserDataHolder,
    dependencies: Array<out Any>,
    classForKey: Class<*>,
    provider: () -> T
): T {
    return getCachedValue(
        holder,
        getKeyForClass(classForKey),
        { CachedValueProvider.Result.create(provider(), *dependencies) },
        false
    )
}