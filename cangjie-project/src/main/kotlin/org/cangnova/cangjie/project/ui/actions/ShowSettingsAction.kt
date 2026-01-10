/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.project.ui.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project


/**
 * 显示仓颉设置的动作
 *
 * 该动作打开 IDE 的设置对话框，并导航到仓颉语言相关的配置页面。
 * 用户可以在这里配置工具链、编译器选项、代码格式等设置。
 */
class ShowSettingsAction : CangJieProjectActionBase() {

    init {
        templatePresentation.text = "CangJie Settings"
        templatePresentation.description = "Open CangJie language settings"
        templatePresentation.icon = AllIcons.General.Settings
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return

        // 打开设置对话框并导航到仓颉配置页面
//        ShowSettingsUtil.getInstance().showSettingsDialog(project, CangJieConfigurable::class.java)
    }

    override fun updatePresentation(e: AnActionEvent, presentation: Presentation) {
        val project = e.project
        presentation.isEnabled = project != null
    }
}