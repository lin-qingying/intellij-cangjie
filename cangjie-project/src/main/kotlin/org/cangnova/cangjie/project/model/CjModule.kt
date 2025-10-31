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

package org.cangnova.cangjie.project.model

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.workspace.jps.entities.ModuleEntity

/**
 * 模块抽象
 *
 * 代表项目中的一个模块/包，每个CjModule应对应一个实际的IntelliJ Module
 */
interface CjModule {
    /**
     * 模块名称
     */
    val name: String

    /**
     * 模块根目录
     */
    val rootDir: VirtualFile

    /**
     * 所属项目
     */
    val project: CjProject

    /**
     * 模块配置文件
     */
    val configFile: VirtualFile?


    /**
     * 构建目标列表
     */
    val targets: List<CjTarget>

    /**
     * 源码集列表
     */
    val sourceSets: List<CjSourceSet>

    /**
     * 模块依赖列表
     *
     * 返回此模块直接依赖的其他模块列表。
     * 这用于在 Project Model 和 Workspace Model 中建立模块间的依赖关系。
     *
     * 默认返回空列表，具体实现需要根据项目配置(如 cjpm.toml)解析依赖关系。
     */
    val dependencies: List<CjModuleDependency>
        get() = emptyList()
}

/**
 * 模块依赖信息
 *
 * 描述一个模块对另一个模块的依赖关系
 *
 * @property moduleName 依赖的模块名称
 * @property scope 依赖作用域(编译时/测试时等)
 * @property exported 是否导出此依赖(传递依赖)
 */
data class CjModuleDependency(
    val moduleName: String,
    val scope: CjDependencyScope = CjDependencyScope.COMPILE,
    val exported: Boolean = false
)

/**
 * 依赖作用域
 */
enum class CjDependencyScope {
    /** 编译时依赖 */
    COMPILE,
    /** 测试时依赖 */
    TEST,
    /** 运行时依赖 */
    RUNTIME,
    /** 仅提供(编译时可见，运行时不包含) */
    PROVIDED
}