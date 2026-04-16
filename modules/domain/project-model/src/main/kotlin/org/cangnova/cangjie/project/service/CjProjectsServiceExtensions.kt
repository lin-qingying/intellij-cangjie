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

package org.cangnova.cangjie.project.service

import org.cangnova.cangjie.project.model.*

/**
 * CjProjectsService 的包管理扩展方法
 *
 * 为 CjProjectsService 添加 V2 版本的包管理功能，包括：
 * - 模块到包的转换
 * - 依赖图解析
 * - 包查找和管理
 */

/**
 * 将 CjModule 转换为 CjPackage.LocalModule
 *
 * @param module 要转换的模块
 * @return 转换后的 LocalModule 包
 */
fun CjProjectsService.toPackage(module: CjModule): CjPackage.LocalModule {
    return module.toPackage()
}

/**
 * 解析项目的依赖图
 *
 * 从项目的根模块开始，解析完整的依赖图。
 *
 * @return 成功时返回完整的依赖图，失败时返回异常
 */
suspend fun CjProjectsService.resolveProjectDependencies(): Result<ResolvedGraph> {
    val project = cjProject

    // 获取根模块
    val rootModule = if (project.isWorkspace) {
        // 对于工作空间，选择第一个模块作为根
        project.workspace?.modules?.firstOrNull()
    } else {
        project.module
    }

    if (rootModule == null) {
        return Result.failure(IllegalStateException("No root module found in project"))
    }

    // 转换为包并解析依赖
    val rootPackage = toPackage(rootModule)
    return CjDependencyService.getInstance(intellijProject).resolveGraph(rootPackage)
}

/**
 * 解析指定模块的依赖图
 *
 * @param module 要解析的模块
 * @return 成功时返回依赖图，失败时返回异常
 */
suspend fun CjProjectsService.resolveModuleDependencies(module: CjModule): Result<ResolvedGraph> {
    val pkg = toPackage(module)
    return CjDependencyService.getInstance(intellijProject).resolveGraph(pkg)
}

/**
 * 获取所有包（本地模块 + 外部依赖）
 *
 * 首先解析项目依赖，然后返回所有包的映射。
 *
 * @return 成功时返回包映射（PackageId -> CjPackage），失败时返回异常
 */
suspend fun CjProjectsService.getAllPackages(): Result<Map<PackageId, CjPackage>> {
    return resolveProjectDependencies().map { graph ->
        graph.packages
    }
}

/**
 * 查找指定 ID 的包
 *
 * 首先解析依赖图，然后查找指定的包。
 *
 * @param id 包标识
 * @return 找到的包，如果不存在则返回 null
 */
suspend fun CjProjectsService.findPackage(id: PackageId): CjPackage? {
    return getAllPackages().getOrNull()?.get(id)
}

/**
 * 获取项目的所有模块包
 *
 * 返回项目中所有本地模块对应的包。
 *
 * @return 所有本地模块包的列表
 */
fun CjProjectsService.getAllModulePackages(): List<CjPackage.LocalModule> {
    val project = cjProject

    return if (project.isWorkspace) {
        // 工作空间：返回所有模块
        project.workspace?.modules?.map { it.toPackage() } ?: emptyList()
    } else {
        // 单模块：返回单个模块
        project.module?.let { listOf(it.toPackage()) } ?: emptyList()
    }
}

/**
 * 检查项目是否有依赖冲突
 *
 * 解析依赖图并检查是否存在循环依赖或版本冲突。
 *
 * @return 成功时返回冲突列表（空列表表示无冲突），失败时返回异常
 */
suspend fun CjProjectsService.checkDependencyConflicts(): Result<List<String>> {
    return resolveProjectDependencies().map { graph ->
        val conflicts = mutableListOf<String>()

        // 检查循环依赖
        val cycles = graph.detectCycles()
        if (cycles.isNotEmpty()) {
            cycles.forEach { cycle ->
                val cycleStr = cycle.joinToString(" -> ") { it.name }
                conflicts.add("Circular dependency: $cycleStr")
            }
        }

        // TODO: 添加版本冲突检测

        conflicts
    }
}

/**
 * 获取项目的依赖树（用于调试和展示）
 *
 * @return 成功时返回依赖树的字符串表示，失败时返回错误信息
 */
suspend fun CjProjectsService.getDependencyTree(): String {
    return resolveProjectDependencies().fold(
        onSuccess = { graph ->
            graph.toDependencyTree()
        },
        onFailure = { error ->
            "Failed to resolve dependencies: ${error.message}"
        }
    )
}

