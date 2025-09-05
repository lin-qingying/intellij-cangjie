/*
 * Copyright 2024 LinQingYing. and contributors.
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

package org.cangnova.cangjie.cjpm.project.model.impl

import org.cangnova.cangjie.cjpm.CjpmConstants
import org.cangnova.cangjie.cjpm.project.model.CjpmProject
import org.cangnova.cangjie.cjpm.project.model.cjpmProjects
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service
class CjpmSettingsFilesService(private val project: Project) {


    @Volatile
    private var settingsFilesCache: Map<String, SettingFileType>? = null
    private fun collectSettingsFiles(): Map<String, SettingFileType> {
        val result = mutableMapOf<String, SettingFileType>()
        for (cjpmProject in project.cjpmProjects.allProjects) {
            cjpmProject.collectSettingsFiles(result)
        }

        settingsFilesCache = result

        return result
    }

    private fun CjpmProject.collectSettingsFiles(out: MutableMap<String, SettingFileType>) {
        val rootPath = rootDir?.path
        if (rootPath != null) {
            CjpmConstants.MANIFEST_FILE.forEach {
                out["$rootPath/$it"] = SettingFileType.CONFIG
            }
            CjpmConstants.LOCK_FILE.forEach {
                out["$rootPath/$it"] = SettingFileType.CONFIG
            }

//            out["$rootPath/${CjpmConstants.MANIFEST_FILE}"] = SettingFileType.CONFIG
//            out["$rootPath/${CjpmConstants.LOCK_FILE}"] = SettingFileType.CONFIG

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
        fun getInstance(project: Project): CjpmSettingsFilesService = project.service()

    }

    enum class SettingFileType {
        CONFIG,
        IMPLICIT_TARGET
    }
}
