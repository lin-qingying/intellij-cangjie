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

package cn.cangnova.cangjie.cjpm.project.model

import cn.cangnova.cangjie.cjpm.CjpmConstants
import cn.cangnova.cangjie.cjpm.project.pathAsPath
import cn.cangnova.cangjie.cjpm.project.toolwindow.CjpmToolWindow
import cn.cangnova.cangjie.ide.notifications.CjEditorNotificationPanel
import cn.cangnova.cangjie.ide.notifications.isCjpmToml
import cn.cangnova.cangjie.ide.run.cjpm.runconfig.buildtool.saveAllDocuments
import cn.cangnova.cangjie.messages.CangJieBundle
import cn.cangnova.cangjie.utils.isUnitTestMode
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path

abstract class CjpmProjectActionBase : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

/**
 * 将CJPM项目添加到[CjpmProjectsService]
 *
 * 可以从Project View、[CjpmToolWindow]和[CjEditorNotificationPanel]调用
 */
class AttachCjpmProjectAction : CjpmProjectActionBase() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        saveAllDocuments()

        val file = when (e.place) {
            CjpmToolWindow.CJPM_TOOLBAR_PLACE -> chooseFile(project, e)
            CjEditorNotificationPanel.NOTIFICATION_PANEL_PLACE -> {
                val file = e.getData(PlatformDataKeys.VIRTUAL_FILE)
                if (file?.isCjpmToml == true) file else chooseFile(project, e)
            }

            else -> e.getData(PlatformDataKeys.VIRTUAL_FILE)
        } ?: return

        val cjpmJson = file.findCjpmToml() ?: return

        if (!project.cjpmProjects.attachCjpmProject(cjpmJson.pathAsPath)) {
            Messages.showErrorDialog(
                project,
                CangJieBundle.message("dialog.message.this.cjpm.package.already.part.attached.workspace"),
                CangJieBundle.message("dialog.title.unable.to.attach.cjpm.project")
            )
        }
    }

    private fun chooseFile(project: Project, event: AnActionEvent): VirtualFile? {
        return if (isUnitTestMode) {
            event.getData(MOCK_CHOSEN_FILE_KEY)
        } else {
            val chooser =
                FileChooserFactory.getInstance().createFileChooser(CjpmProjectChooserDescriptor, project, null)
            return chooser.choose(project).singleOrNull()
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        e.presentation.isEnabledAndVisible = isActionEnabled(e, project)
    }

    private fun isActionEnabled(e: AnActionEvent, project: Project): Boolean {
        return when (e.place) {
            CjpmToolWindow.CJPM_TOOLBAR_PLACE, CjEditorNotificationPanel.NOTIFICATION_PANEL_PLACE -> true
            else -> {
                // 需要使用`ProjectFileIndex`来检查`cjpm.toml`是否在项目内容中
                // 所以在dumb模式下禁用该操作
                if (DumbService.isDumb(project)) return false
                val file = e.getData(PlatformDataKeys.VIRTUAL_FILE)
                val cjpmToml = file?.findCjpmToml() ?: return false

                canBeAttached(project, cjpmToml)
            }
        }
    }

    private fun VirtualFile.findCjpmToml(): VirtualFile? {
        return if (isDirectory) findChild(CjpmConstants.MANIFEST_FILE) else takeIf { it.isCjpmToml }
    }

    companion object {
        @VisibleForTesting
        val MOCK_CHOSEN_FILE_KEY: DataKey<VirtualFile> = DataKey.create("MOCK_CHOSEN_FILE_KEY")

        fun canBeAttached(project: Project, cjpmToml: VirtualFile): Boolean {
            require(cjpmToml.isCjpmToml)
            if (!ProjectFileIndex.getInstance(project).isInContent(cjpmToml)) return false

            val path = cjpmToml.pathAsPath

            // 项目模块已包含以该清单文件为配置的CJPM项目
            if (project.cjpmProjects.allProjects.any { it.manifest == path }) return false
            // 项目模块已包含以该清单文件为配置的CJPM包
            if (project.cjpmProjects.allProjects.any { it.containsWorkspaceManifest(path) }) return false
            return true

        }

        private fun CjpmProject.containsWorkspaceManifest(path: Path): Boolean {
            val rootDir = path.parent
            return workspace?.packages.orEmpty().any { it.rootDirectory == rootDir }
        }
    }
}

/**
 * 文件选择器描述符，用于配置CJPM项目文件的选择规则
 * FileChooserDescriptor构造函数参数说明：
 * @param chooseFiles 是否可以选择文件 (true)
 * @param chooseFolders 是否可以选择文件夹 (true)
 * @param chooseJars 是否可以选择jar文件 (false)
 * @param chooseJarsAsFiles 是否可以选择jar作为文件 (false)
 * @param chooseJarContents 是否可以选择jar内容 (false)
 * @param chooseMultiple 是否可以多选 (false)
 */
object CjpmProjectChooserDescriptor : FileChooserDescriptor(true, true, false, false, false, false) {

    init {
        // 设置文件过滤器，只允许选择包含CJPM配置文件的目录或CJPM配置文件本身
        // 如果是目录，检查是否包含CJPM配置文件
        // 如果是文件，检查是否是CJPM配置文件
        withFileFilter { file ->
            if (file.isDirectory) {
                file.findChild(CjpmConstants.MANIFEST_FILE) != null
            } else {
                file.isCjpmToml
            }
        }
        // 设置文件选择器对话框的标题
        @Suppress("DialogTitleCapitalization")
        withTitle(CangJieBundle.message("dialog.title.select.cjpm.toml"))
    }
}

