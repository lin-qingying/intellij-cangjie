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

package org.cangnova.cangjie.cjpm1.build

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.build.model.*
import org.cangnova.cangjie.cjpm1.config.CjpmTomlParserAdapter

/**
 * CJPM 构建系统实现
 */
class CjpmBuildSystemImpl(
    override val projectDir: VirtualFile,
    override val project: Project,
    private val manifestFile: VirtualFile
) : CjBuildSystem {

    private val config by lazy {
        CjpmTomlParserAdapter.parse(manifestFile)
    }

    override val name: String = "CJPM"

    override val version: String? = "1.0.0"

    override val configuration: CjBuildConfiguration by lazy {
        _root_ide_package_.org.cangnova.cangjie.cjpm1.build.CjpmBuildConfigurationImpl(config, projectDir)
    }

    override fun execute(task: CjBuildTask, context: CjBuildContext): CjBuildResult {
        // TODO: 执行构建任务
        // 1. 调用 cjpm build 命令
        // 2. 解析输出
        // 3. 返回构建结果
        throw NotImplementedError("Build execution not implemented yet")
    }

    override fun getAvailableTasks(): List<CjBuildTask> {
        return listOf(
            CjpmBuildTaskImpl("build", CjBuildTaskType.COMPILE, "Build the project"),
            CjpmBuildTaskImpl("test", CjBuildTaskType.TEST, "Run tests"),
            CjpmBuildTaskImpl("clean", CjBuildTaskType.CLEAN, "Clean build artifacts")
        )
    }

    override fun validate(): Boolean {
        return config != null && manifestFile.exists()
    }

    override fun refresh() {
        // 刷新配置
    }
}

/**
 * CJPM 构建任务实现
 */
class CjpmBuildTaskImpl(
    override val name: String,
    override val type: CjBuildTaskType,
    override val description: String?
) : CjBuildTask {
    override val dependencies: List<CjBuildTask> = emptyList()
}