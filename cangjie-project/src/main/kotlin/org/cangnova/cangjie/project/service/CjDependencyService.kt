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

package org.cangnova.cangjie.project.service

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.project.model.CjDependency
import org.cangnova.cangjie.project.model.CjPackage
import org.cangnova.cangjie.project.model.ResolvedGraph

/**
 * 依赖解析服务（Application 级别）
 *
 * 提供完整的依赖图解析功能，包括：
 * - 递归依赖解析
 * - 版本选择
 * - 特性传播
 * - 循环依赖检测
 *
 *
 * CjDependencyResolver提供具体单个包解析逻辑
 */

interface CjDependencyService {
    companion object {
        /**
         * 获取服务实例
         */
        fun getInstance(project: Project): CjDependencyService {
            return project.service<CjDependencyService>()
        }
    }

    /**
     * 解析完整依赖图
     *
     * 从根包开始，递归解析所有依赖，构建完整的依赖图。
     * 包括：
     * - 传递依赖解析
     * - 版本冲突检测
     * - 循环依赖检测
     * - 特性传播
     *
     * @param root 根包（通常是项目本身）
     * @return 成功时返回完整的依赖图，失败时返回异常
     */
    suspend fun resolveGraph(root: CjPackage): Result<ResolvedGraph>

    /**
     * 解析单个依赖（内部使用）
     *
     * 根据依赖声明解析出具体的包。
     * 通常由 resolveGraph() 内部调用，也可被 Workspace Model 同步时直接使用。
     *
     * 支持缓存：如果之前已解析过相同的依赖，将直接返回缓存结果。
     *
     * @param dependency 依赖声明
     * @param enabledFeatures 父包启用的特性（用于特性传播）
     * @return 成功时返回解析后的包，失败时返回异常
     */
    suspend fun resolveSingle(
        dependency: CjDependency,
        enabledFeatures: Set<String> = emptySet()
    ): Result<CjPackage>

    /**
     * 清除依赖解析缓存
     *
     * 当项目刷新或依赖配置发生变更时应该调用此方法
     */
    fun clearCache()


}