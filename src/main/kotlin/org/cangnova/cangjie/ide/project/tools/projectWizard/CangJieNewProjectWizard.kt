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

package org.cangnova.cangjie.ide.project.tools.projectWizard

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.GitNewProjectWizardData.Companion.gitData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.language.LanguageGeneratorNewProjectWizard
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.validation.DialogValidation
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import org.cangnova.cangjie.icon.CangJieIcons
import org.cangnova.cangjie.messages.CangJieBundle
import org.cangnova.cangjie.project.extension.CjModuleBuilderProvider
import java.nio.file.Path
import javax.swing.Icon

/**
 * 仓颉新项目向导生成器
 */
internal class CangJieGeneratorNewProjectWizard : LanguageGeneratorNewProjectWizard {

    companion object {
        private const val GITIGNORE: String = ".gitignore"

        private fun createGitIgnoreFile(projectDir: Path) {
            val directory = VfsUtil.createDirectoryIfMissing(projectDir.toString()) ?: return
            val existingFile = directory.findChild(GITIGNORE)
            if (existingFile != null) return

            runWriteAction {
                val file = directory.createChildData(this, GITIGNORE)
                VfsUtil.saveText(file, "/build\n")
            }
        }
    }

    override val icon: Icon = CangJieIcons.CANGJIE
    override val name: String = CangJieBundle.message("cangjie")

    override fun createStep(parent: NewProjectWizardStep): NewProjectWizardStep =
        Step(parent)

    class Step(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {

        private val peer: CjProjectGeneratorPeer = CjProjectGeneratorPeer(parent.context.projectDirectory)

        override fun setupProject(project: Project) {
            // 通过扩展点获取 ModuleBuilder，按优先级排序
            val providers = CjModuleBuilderProvider.getModuleBuilderProvider()

            // 使用第一个可用的 ModuleBuilder
            val builder = providers?.createModuleBuilder()
                ?: throw IllegalStateException("No ModuleBuilder provider available")

            builder.configurationData = peer.settings

            // 提交模块创建
            val module = builder.commit(project)?.firstOrNull() ?: return

            // 如果启用了 Git，创建 .gitignore 文件
            if (gitData?.git == true) {
                createGitIgnoreFile(context.projectDirectory)
            }
        }

        override fun setupUI(builder: Panel) {
            with(builder) {
                row {
                    cell(peer.component)
                        .align(Align.FILL)
                        .validationRequestor { peer.checkValid = Runnable(it) }
                        .validation(DialogValidation { peer.validate() })
                }
            }
        }
    }
}


class CangJieProjectTypeItem(val type: String, val description: String) {
    override fun toString(): String {
        return description
    }
}
