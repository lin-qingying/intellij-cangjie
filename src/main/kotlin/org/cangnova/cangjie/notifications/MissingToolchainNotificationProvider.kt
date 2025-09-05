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

package org.cangnova.cangjie.notifications


import org.cangnova.cangjie.cjpm.project.model.CjpmProjectsService
import org.cangnova.cangjie.cjpm.project.model.cjpmProjects
import org.cangnova.cangjie.cjpm.project.model.guessAndSetupCangJieProject
import org.cangnova.cangjie.ide.project.settings.CjProjectSettingsServiceBase
import org.cangnova.cangjie.ide.project.settings.CjProjectSettingsServiceBase.Companion.CANGJIE_SETTINGS_TOPIC
import org.cangnova.cangjie.ide.project.settings.cangjieSettings
import org.cangnova.cangjie.ide.run.cjpm.toolchain
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.messages.CangJieBundle
import org.cangnova.cangjie.utils.isUnitTestMode
import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile


class MissingToolchainNotificationProvider(project: Project) : CjNotificationProvider(project), DumbAware {

    override val VirtualFile.disablingKey: String get() = NOTIFICATION_STATUS_KEY

    init {
        project.messageBus.connect().apply {
            subscribe(CANGJIE_SETTINGS_TOPIC, object : CjProjectSettingsServiceBase.CjSettingsListener {
                override fun <T : CjProjectSettingsServiceBase.CjProjectSettingsBase<T>> settingsChanged(e: CjProjectSettingsServiceBase.SettingsChangedEventBase<T>) {
                    updateAllNotifications()
                }
            })

            subscribe(CjpmProjectsService.CJPM_PROJECTS_TOPIC, CjpmProjectsService.CjpmProjectsListener { _, _ ->
                updateAllNotifications()
            })
        }
    }

    override fun createNotificationPanel(
        file: VirtualFile,
        editor: FileEditor,
        project: Project
    ): CjEditorNotificationPanel? {
        if (isUnitTestMode) return null
        if (!(file.isCangJieFile) || isNotificationDisabled(file)) return null
        @Suppress("UnstableApiUsage")
        if (!project.isTrusted()) return null
        if (guessAndSetupCangJieProject(project)) return null

        val toolchain = project.toolchain
        if (toolchain == null || !toolchain.looksLikeValidToolchain()) {
            return createBadToolchainPanel(file)
        }

        val cjpmProjects = project.cjpmProjects

        if (!cjpmProjects.initialized) return null


        return null
    }

    private fun createBadToolchainPanel(file: VirtualFile): CjEditorNotificationPanel =
        CjEditorNotificationPanel(NO_CANGJIE_TOOLCHAIN).apply {
            text = CangJieBundle.message("notification.no.toolchain.configured")
            createActionLabel(CangJieBundle.message("notification.action.set.up.toolchain.text")) {
                project.cangjieSettings.configureToolchain()
            }
            createActionLabel(CangJieBundle.message("notification.action.do.not.show.again.text")) {
                disableNotification(file)
                updateAllNotifications()
            }
        }


    companion object {
        private const val NOTIFICATION_STATUS_KEY = "org.cangnova.cangjie.hideToolchainNotifications"
        const val NO_CANGJIE_TOOLCHAIN = "NoCangjieToolchain"

    }
}

val VirtualFile.isCangJieFile: Boolean get() = fileType == CangJieFileType.INSTANCE
fun VirtualFile.isCangJieFileType(): Boolean {
    val nameSequence = nameSequence
    if (nameSequence.endsWith(CangJieFileType.DOT_DEFAULT_EXTENSION)) return true


    return FileTypeRegistry.getInstance().isFileOfType(this, CangJieFileType.INSTANCE)
}
