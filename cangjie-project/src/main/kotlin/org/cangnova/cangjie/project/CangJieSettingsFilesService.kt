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

package org.cangnova.cangjie.project

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.model.cjProject

@Service
class CangJieSettingsFilesService(private val project: Project) {


    @Volatile
    private var settingsFilesCache: Map<String, SettingFileType>? = null
    private fun collectSettingsFiles(): Map<String, SettingFileType> {

        val result = mutableMapOf<String, SettingFileType>()

        project
            .cjProject.collectSettingsFiles(result)


        settingsFilesCache = result

        return result
    }

    private fun CjProject.collectSettingsFiles(out: MutableMap<String, SettingFileType>) {
        val rootPath = rootDir.path

        // 先获取到局部变量，避免多次访问属性导致的竞态条件
        val currentWorkspace = workspace
        val currentModule = module

        if (currentWorkspace != null) {
            currentWorkspace.configFile?.let {
                out[it.path] = SettingFileType.CONFIG
            }
            currentWorkspace.modules.forEach {
                it.configFile?.let {
                    out[it.path] = SettingFileType.CONFIG
                }
            }
        } else if (currentModule != null) {
            currentModule.configFile?.let {
                out[it.path] = SettingFileType.CONFIG
            }
        }
    }

    fun collectSettingsFiles(useCache: Boolean): Map<String, SettingFileType> {
        return if (useCache) {
            settingsFilesCache ?: collectSettingsFiles()
        } else {
            collectSettingsFiles()
        }
    }

    companion object {
        fun getInstance(project: Project): CangJieSettingsFilesService = project.service()

    }

    enum class SettingFileType {
        CONFIG,
        IMPLICIT_TARGET
    }
}
