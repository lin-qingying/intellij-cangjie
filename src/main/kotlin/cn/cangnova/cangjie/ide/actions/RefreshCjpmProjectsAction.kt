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

package cn.cangnova.cangjie.ide.actions

import cn.cangnova.cangjie.cjpm.project.model.CjpmProjectActionBase
import cn.cangnova.cangjie.cjpm.project.model.cjpmProjects
import cn.cangnova.cangjie.cjpm.project.model.guessAndSetupCangJieProject
import cn.cangnova.cangjie.cjpm.project.toolwindow.hasCjpmProject
import cn.cangnova.cangjie.ide.run.cjpm.runconfig.buildtool.saveAllDocuments
import cn.cangnova.cangjie.ide.run.cjpm.toolchain
import cn.cangnova.cangjie.messages.CangJieBundle
import com.intellij.ide.IdeBundle
import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class RefreshCjpmProjectsAction : CjpmProjectActionBase() {

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = project != null && project.toolchain != null && project.hasCjpmProject
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        if (!project.confirmLoadingUntrustedProject()) return

        saveAllDocuments()
        if (project.toolchain == null || !project.hasCjpmProject) {
            guessAndSetupCangJieProject(project, explicitRequest = true)
        } else {
            project.cjpmProjects.refreshAllProjects()
        }
    }
}

@Suppress("UnstableApiUsage")
fun Project.confirmLoadingUntrustedProject(): Boolean {
    return isTrusted() || com.intellij.ide.impl.confirmLoadingUntrustedProject(
        this,
        title = IdeBundle.message("untrusted.project.dialog.title", CangJieBundle.message("cjpm"), 1),
        message = IdeBundle.message("untrusted.project.dialog.text", CangJieBundle.message("cjpm"), 1),
        trustButtonText = IdeBundle.message("untrusted.project.dialog.trust.button"),
        distrustButtonText = IdeBundle.message("untrusted.project.dialog.distrust.button")
    )
}
