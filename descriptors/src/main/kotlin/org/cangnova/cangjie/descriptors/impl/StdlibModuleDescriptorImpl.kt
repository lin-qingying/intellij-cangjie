/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.descriptors.ModuleCapability
import org.cangnova.cangjie.descriptors.ProjectDescriptor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.storage.StorageManager

/**
 * 库模块描述符实现
 *
 * 用于表示所有外部依赖库（stdlib, binary libraries, external libraries 等）。
 * 参考 Java/Kotlin 的 Library 机制：
 * - classes (编译后的 .cjo 文件) - 用于类型解析
 * - sources (源文件) - 用于代码导航和查看实现
 *
 * ## 使用场景
 *
 * 1. **标准库 (CjDependency.Stdlib)**:
 *    - 从 SDK 加载 .cjo 文件（二进制模式）
 *
 * 2. **二进制依赖 (CjDependency.Binary)**:
 *    - 从指定的 .cjo 文件加载（二进制模式）
 *
 * 3. **外部库 (CjDependency.Library)**:
 *    - 优先从 .cjo 文件加载（如果存在）
 *    - 回退到源码加载（如果 .cjo 不存在）
 *
 * 4. **路径依赖 (CjDependency.Path)**:
 *    - 通常作为源码模块处理
 *
 * ## 自动加载策略
 *
 * 模块根据 AnalysisContext.isSourceContext 自动选择加载方式：
 * - **isSourceContext = false（二进制）**:
 *   1. 尝试使用 BuiltInsLoader 加载 .cjo 文件
 *   2. 失败则回退到 DelegatingPackageFragmentProvider
 * - **isSourceContext = true（源码）**:
 *   直接使用 DelegatingPackageFragmentProvider
 *
 * @property projectDescriptor 所属的仓颉项目
 * @property moduleName 模块名称（内部标识）
 * @property displayName 模块显示名称（用于 UI 展示）
 * @property storageManager 存储管理器
 * @property capabilities 模块能力映射
 * @property stableName 模块的稳定名称（用于 ABI）
 *
 * @see org.cangnova.cangjie.builtins.BuiltInsLoader
 * @see org.cangnova.cangjie.resolve.DelegatingPackageFragmentProvider
 */
class StdlibModuleDescriptorImpl(
    projectDescriptor: ProjectDescriptor,
    moduleName: Name,
    displayName: String? = null,
    storageManager: StorageManager,
    capabilities: Map<ModuleCapability<*>, Any?> = emptyMap(),
    stableName: Name? = null
) : ModuleDescriptorImpl(
    projectDescriptor,
    moduleName,
    displayName,
    storageManager,
    capabilities,
    stableName
){
    // 必须在父类构造函数调用后立即初始化，不能用延迟初始化
    // 因为父类 init 块会调用 getCapability()
    private var _capabilities: MutableMap<ModuleCapability<*>, Any?>? = null

    fun addCapability(capability: ModuleCapability<*>, value: Any?) {
        if (_capabilities == null) {
            _capabilities = mutableMapOf()
        }
        _capabilities!![capability] = value
    }

    override fun <T> getCapability(capability: ModuleCapability<T>): T? {
        // 安全检查：父类构造函数执行时 _capabilities 可能还未初始化
        return super.getCapability(capability) ?: _capabilities?.get(capability) as? T
    }
}
