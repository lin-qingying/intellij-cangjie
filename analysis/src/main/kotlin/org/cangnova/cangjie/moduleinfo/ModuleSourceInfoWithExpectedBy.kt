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

package org.cangnova.cangjie.moduleinfo

import org.cangnova.cangjie.cache.cacheByClassInvalidatingOnRootModifications
import org.cangnova.cangjie.utils.SourceCangJieRootType
import org.cangnova.cangjie.utils.TestSourceCangJieRootType

/**
 * 带 expectedBy 支持的模块源码信息基类
 *
 * 该 sealed class 为项目源码模块提供基础实现，主要处理：
 * 1. **模块依赖**: 收集和缓存模块的依赖关系
 * 2. **expectedBy 支持**: 仓颉语言当前不使用多平台，因此返回空列表
 *
 * ## 设计模式
 *
 * 使用 sealed class 限制继承，确保只有预定义的子类（如生产源码、测试源码）可以继承此类。
 *
 * ## 依赖缓存
 *
 * 依赖信息通过 [cacheByClassInvalidatingOnRootModifications] 进行缓存，
 * 当模块根目录发生变更时自动失效。
 *
 * ## 生产/测试区分
 *
 * 通过 [forProduction] 参数区分生产源码和测试源码：
 * - `true`: 生产源码模块
 * - `false`: 测试源码模块
 *
 * ## 继承层次
 *
 * ```
 * ModuleSourceInfo (接口)
 *   ↓
 * ModuleSourceInfoWithExpectedBy (sealed class，基础实现)
 *   ↓
 * ModuleProductionSourceInfo (data class，生产源码)
 * ```
 *
 * ## 示例
 *
 * ```kotlin
 * // 生产源码模块
 * val productionInfo = ModuleProductionSourceInfo(module)
 * val dependencies = productionInfo.dependencies
 * val expectedBy = productionInfo.expectedBy  // 空列表
 * ```
 *
 * @param forProduction 是否为生产源码模块（true）或测试源码模块（false）
 *
 * @see ModuleSourceInfo
 * @see ModuleProductionSourceInfo
 */
sealed class ModuleSourceInfoWithExpectedBy(private val forProduction: Boolean) : ModuleSourceInfo {



    /**
     * 模块依赖列表
     *
     * 返回此模块依赖的所有模块信息，包括：
     * - 当前模块自身
     * - 直接依赖的其他模块
     * - 依赖的库模块
     *
     * ## 缓存机制
     *
     * 依赖信息通过 [cacheByClassInvalidatingOnRootModifications] 缓存，
     * 仅在模块根目录变更时重新计算。
     *
     * ## 依赖收集
     *
     * 使用 [ModuleDependencyCollector] 收集模块依赖，该收集器会：
     * 1. 遍历模块的依赖配置
     * 2. 递归收集传递依赖（如果 includeExportedDependencies = true）
     * 3. 处理循环依赖
     * 4. 按正确的顺序返回依赖列表
     *
     * ## 生产/测试源码区分
     *
     * - **生产源码** (`forProduction = true`): 只包含生产依赖
     * - **测试源码** (`forProduction = false`): 包含生产依赖 + 测试依赖
     *
     * @return 模块依赖列表
     * @see ModuleDependencyCollector
     * @see SourceCangJieRootType
     * @see TestSourceCangJieRootType
     */
    override val dependencies: List<IdeaModuleInfo>
        get() = module.cacheByClassInvalidatingOnRootModifications(this::class.java) {
            val sourceRootType = if (forProduction) SourceCangJieRootType else TestSourceCangJieRootType
            ModuleDependencyCollector.getInstance(module.project)
                .collectModuleDependencies(
                    module = module,
                    sourceRootType = sourceRootType,
                    includeExportedDependencies = true
                )
                .toList()
        }
}
