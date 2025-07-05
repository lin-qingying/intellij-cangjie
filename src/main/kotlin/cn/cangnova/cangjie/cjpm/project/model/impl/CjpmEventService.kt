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

package cn.cangnova.cangjie.cjpm.project.model.impl

import cn.cangnova.cangjie.cjpm.project.model.CjpmProjectsService
import cn.cangnova.cangjie.cjpm.project.model.CjpmProjectsService.Companion.CJPM_PROJECTS_TOPIC
import cn.cangnova.cangjie.utils.mapToSet
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap


/**
 * 监听module-lock.json 以更新项目结构
 */
@Service
class CjpmEventService(project: Project) {

    private val metadataCallTimestamps: ConcurrentMap<Path, Long> = ConcurrentHashMap()

    init {
        project.messageBus.connect().subscribe(CJPM_PROJECTS_TOPIC,
            CjpmProjectsService.CjpmProjectsListener { _, projects ->
                val projectDirs = projects.mapToSet { it.workingDirectory }
                metadataCallTimestamps.keys.retainAll(projectDirs)
            })
    }

    fun onMetadataCall(projectDirectory: Path) {
        metadataCallTimestamps[projectDirectory] = System.currentTimeMillis()
    }

    fun extractTimestamp(projectDirectory: Path): Long? = metadataCallTimestamps.remove(projectDirectory)

    companion object {
        fun getInstance(project: Project): CjpmEventService = project.service()
    }
}
