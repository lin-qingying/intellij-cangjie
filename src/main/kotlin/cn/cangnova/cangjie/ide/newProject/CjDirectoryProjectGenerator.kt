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

package cn.cangnova.cangjie.ide.newProject

import cn.cangnova.cangjie.cjpm.project.settings.cangjieSettings
import cn.cangnova.cangjie.icon.CangJieIcons
import cn.cangnova.cangjie.ide.module.makeProject
import cn.cangnova.cangjie.ide.module.openFiles
import cn.cangnova.cangjie.ide.newProject.ui.ConfigurationData
import cn.cangnova.cangjie.ide.run.cjpm.runconfig.unwrapOrThrow
import cn.cangnova.cangjie.messages.CangJieBundle
import cn.cangnova.cangjie.toolchain.cjpm
import cn.cangnova.cangjie.utils.computeWithCancelableProgress
import com.intellij.facet.ui.ValidationResult
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep
import com.intellij.ide.util.projectWizard.CustomStepProjectGenerator
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.platform.DirectoryProjectGeneratorBase
import com.intellij.platform.ProjectGeneratorPeer
import javax.swing.Icon

open class CjProjectSettingsStep(generator: DirectoryProjectGenerator<ConfigurationData>) :
    ProjectSettingsStepBase<ConfigurationData>(generator, AbstractNewProjectStep.AbstractCallback()) {


    //    override fun createPanel(): JPanel  = panel {
//        row(CangJieUiBundle.message("action.new.project.projecttype.title")) {
//            cell(projectTypeComboBox)
//                .columns(COLUMNS_MEDIUM)
//                .component
//        }.bottomGap(BottomGap.SMALL)
//    }
}


//实现CustomStepProjectGenerator，否则在PyCharm中不会将组件添加到面板
class CjDirectoryProjectGenerator : DirectoryProjectGeneratorBase<ConfigurationData>(),
    CustomStepProjectGenerator<ConfigurationData> {

    private var peer: CjProjectGeneratorPeer = CjProjectGeneratorPeer()

    override fun getName(): String = CangJieBundle.message("cangjie")

    override fun getLogo(): Icon = CangJieIcons.CANGJIE

    override fun generateProject(project: Project, baseDir: VirtualFile, data: ConfigurationData, module: Module) {
        val (settings, projectType) = data
        val cjpm = settings.toolchain?.cjpm() ?: return

        val name = project.name.replace(' ', '_')
        val generatedFiles =
            project.computeWithCancelableProgress(CangJieBundle.message("progress.title.generating.cjpm.project")) {
                cjpm.makeProject(project, module, baseDir, name, projectType = projectType)
                    .unwrapOrThrow() // TODO throw? really??
            }

        project.cangjieSettings.modify {
            it.toolchain = settings.toolchain
        }


        project.openFiles(generatedFiles)
    }


    override fun createPeer(): ProjectGeneratorPeer<ConfigurationData> {
        return peer
    }

    override fun validate(baseDirPath: String): ValidationResult {

        return ValidationResult.OK
    }

    override fun createStep(
        projectGenerator: DirectoryProjectGenerator<ConfigurationData>,
        callback: AbstractNewProjectStep.AbstractCallback<ConfigurationData>
    ): AbstractActionWithPanel = CjProjectSettingsStep(projectGenerator)

}
