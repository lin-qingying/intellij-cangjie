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

package com.linqingying.cangjie.ide.notifications


import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.cjpm.project.model.AttachCjpmProjectAction
import com.linqingying.cangjie.cjpm.project.model.CjpmProjectsService
import com.linqingying.cangjie.cjpm.project.model.cjpmProjects
import com.linqingying.cangjie.cjpm.toolchain.wsl.isDispatchThread
import com.linqingying.cangjie.ide.run.cjpm.isUnitTestMode
import com.intellij.ide.impl.isTrusted
import com.intellij.ide.scratch.ScratchUtil
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.LinkLabel
import com.intellij.openapi.vfs.VirtualFile


class NoCjpmProjectNotificationProvider(project: Project) : CjNotificationProvider(project) {

    init {
        project.messageBus.connect().apply {
            subscribe(CjpmProjectsService.CJPM_PROJECTS_TOPIC, CjpmProjectsService.CjpmProjectsListener { _, _ ->
                updateAllNotifications()
            })
        }
    }

    override val VirtualFile.disablingKey: String
        get() = NOTIFICATION_STATUS_KEY + path

    override fun createNotificationPanel(
        file: VirtualFile,
        editor: FileEditor,
        project: Project
    ): CjEditorNotificationPanel? {
        if (isUnitTestMode && !isDispatchThread) return null
        if (!(file.isCangJieFile || file.isCjpmToml) || isNotificationDisabled(file)) return null
        if (ScratchUtil.isScratch(file)) return null
        @Suppress("UnstableApiUsage")
        if (!project.isTrusted()) return null

        val cjpmProjects = project.cjpmProjects
        if (!cjpmProjects.initialized) return null
        if (!cjpmProjects.hasAtLeastOneValidProject) {
            return createNoCjpmProjectsPanel(file)
        }

        if (file.isCjpmToml) {
            if (AttachCjpmProjectAction.canBeAttached(project, file)) {
                return createNoCjpmProjectForFilePanel(file)
            }
        } else if (cjpmProjects.findProjectForFile(file) == null) {
            //TODO: more precise check here
            return createNoCjpmProjectForFilePanel(file)
        }

        return null
    }

    private fun createNoCjpmProjectsPanel(file: VirtualFile): CjEditorNotificationPanel =
        createAttachCjpmProjectPanel(
            NO_CJPM_PROJECTS,
            file,
            CangJieBundle.message("notification.no.cjpm.projects.found")
        )

    private fun createNoCjpmProjectForFilePanel(file: VirtualFile): CjEditorNotificationPanel =
        createAttachCjpmProjectPanel(
            FILE_NOT_IN_CJPM_PROJECT,
            file,
            CangJieBundle.message("notification.file.not.belong.to.cjpm.project")
        )

    @Suppress("UnstableApiUsage")
    private fun createAttachCjpmProjectPanel(
        debugId: String,
        file: VirtualFile,
        @LinkLabel message: String
    ): CjEditorNotificationPanel =
        CjEditorNotificationPanel(debugId).apply {
            text = message
            createActionLabel(CangJieBundle.message("notification.action.attach.text"), "Cjpm.AttachCjpmProject")
            createActionLabel(CangJieBundle.message("notification.action.do.not.show.again.text")) {
                disableNotification(file)
                updateAllNotifications()
            }
        }

    companion object {
        private const val NOTIFICATION_STATUS_KEY = "com.linqingying.cangjie.hideNoCjpmProjectNotifications"

        const val NO_CJPM_PROJECTS = "NoCjpmProjects"
        const val FILE_NOT_IN_CJPM_PROJECT = "FileNotInCjpmProject"
    }
}
