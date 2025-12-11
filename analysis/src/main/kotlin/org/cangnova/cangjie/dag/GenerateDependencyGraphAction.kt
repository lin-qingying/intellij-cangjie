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

package org.cangnova.cangjie.dag

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class GenerateDependencyGraphAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val editor = FileEditorManager.getInstance(project).selectedTextEditor

        if (editor != null) {
            val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)

            // 获取当前文件路径和内容，进行包依赖关系分析
            val filePath = virtualFile?.path ?: return
            val packageGraph = generatePackageDependencyGraph(filePath)

            // 在插件界面上显示依赖关系图
            displayDependencyGraph(packageGraph)
        } else {
            Messages.showMessageDialog(project, "No editor found", "Error", Messages.getErrorIcon())
        }
    }

    // 模拟生成依赖关系图的逻辑
    private fun generatePackageDependencyGraph(filePath: String): String {
        // 假设这里会分析文件并返回图形字符串
        return "Package Dependency Graph for $filePath"
    }

    private fun displayDependencyGraph(graph: String) {
        // 显示依赖关系图，可以在弹窗中或新的工具窗口中显示
        Messages.showMessageDialog(graph, "Dependency Graph", Messages.getInformationIcon())
    }
}
