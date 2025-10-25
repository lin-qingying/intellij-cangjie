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

package org.cangnova.cangjie.cjpm.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.project.extension.CjProjectProvider
import org.cangnova.cangjie.project.model.CjProject

/**
 * CJPM 项目提供者实现
 */
class CjpmProjectProvider : CjProjectProvider {
    companion object {
        private const val MANIFEST_FILE = "cjpm.toml"
    }

    override val providerName: String = "CJPM"

    override val priority: Int = 100

    override fun canHandle(dir: VirtualFile): Boolean {
        // 检查目录下是否存在 cjpm.toml 文件
        return dir.findChild(MANIFEST_FILE) != null
    }

    override fun createProject(dir: VirtualFile, project: Project): CjProject? {
        val manifestFile = dir.findChild(MANIFEST_FILE) ?: return null

        return try {
            CjpmProjectImpl(
                rootDir = dir,
                intellijProject = project,
                manifestFile = manifestFile
            )
        } catch (e: Exception) {
            null
        }
    }
}