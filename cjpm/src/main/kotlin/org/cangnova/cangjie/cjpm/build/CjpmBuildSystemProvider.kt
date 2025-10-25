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

package org.cangnova.cangjie.cjpm.build

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.build.extension.CjBuildSystemProvider
import org.cangnova.cangjie.build.model.CjBuildSystem

/**
 * CJPM 构建系统提供者
 */
class CjpmBuildSystemProvider : CjBuildSystemProvider {

    override val providerName: String = "CJPM Build System"

    override val priority: Int = 100

    override fun canHandle(projectDir: VirtualFile): Boolean {
        // 检查是否存在 cjpm.toml
        return projectDir.findChild("cjpm.toml") != null
    }

    override fun createBuildSystem(projectDir: VirtualFile, project: Project): CjBuildSystem? {
        val manifestFile = projectDir.findChild("cjpm.toml") ?: return null

        return try {
            CjpmBuildSystemImpl(
                projectDir = projectDir,
                project = project,
                manifestFile = manifestFile
            )
        } catch (e: Exception) {
            null
        }
    }

    override fun getBuildSystemVersion(projectDir: VirtualFile): String? {
        // TODO: 获取 CJPM 版本
        return "1.0.0"
    }
}