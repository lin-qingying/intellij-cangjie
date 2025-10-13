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

package org.cangnova.cangjie


import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class CangJieStartupActivity : ProjectActivity {
    companion object {
        val LOG = Logger.getInstance(CangJieStartupActivity::class.java)
    }

    override suspend fun execute(project: Project) {
        LOG.info("Cangjie plugin has been launched")


//        插件打开时发送遥测事件
//        val telemetryService = TelemetryService.getInstance()
//        telemetryService.sendEvent(
//            IDE_OPEN, "ide_open", 0
//        )


//        TelemetryNotifications.showNotification()
//        try {
//            (project.serviceAsync<WorkspaceFileIndex>() as WorkspaceFileIndexEx).initialize()
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
    }
}
