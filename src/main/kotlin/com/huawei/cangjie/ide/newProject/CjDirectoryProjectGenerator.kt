package com.huawei.cangjie.ide.newProject

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.cjpm.project.settings.cangjieSettings
import com.huawei.cangjie.cjpm.toolchain.cjpm
import com.huawei.cangjie.icon.CangJieIcons
import com.huawei.cangjie.ide.newProject.ui.ConfigurationData
import com.huawei.cangjie.ide.project.tools.projectWizard.wizard.makeProject
import com.huawei.cangjie.ide.project.tools.projectWizard.wizard.openFiles
import com.huawei.cangjie.ide.run.cjpm.runconfig.computeWithCancelableProgress
import com.huawei.cangjie.ide.run.cjpm.runconfig.unwrapOrThrow
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

    private var peer: CjProjectGeneratorPeer? = null

    override fun getName(): String = CangJieBundle.message("cangjie")

    override fun getLogo(): Icon = CangJieIcons.CANGJIE_16

    override fun generateProject(project: Project, baseDir: VirtualFile, data: ConfigurationData, module: Module) {
        val (settings, projectType) = data
        val cjpm = settings.toolchain?.cjpm() ?: return

        val name = project.name.replace(' ', '_')
        val generatedFiles =
            project.computeWithCancelableProgress(CangJieBundle.message("progress.title.generating.cjpm.project")) {
                cjpm.makeProject(project, module, baseDir, name, projectType =  projectType).unwrapOrThrow() // TODO throw? really??
            }

        project.cangjieSettings.modify {
            it.toolchain = settings.toolchain
//            it.explicitPathToStdlib = settings.explicitPathToStdlib
        }


        project.openFiles(generatedFiles)
    }

    override fun createPeer(): ProjectGeneratorPeer<ConfigurationData> = CjProjectGeneratorPeer().also { peer = it }

    override fun validate(baseDirPath: String): ValidationResult {
//        TODO()
//        val crateName = PathUtil.getFileName(baseDirPath)
//
//
//           return ValidationResult(message)
        return ValidationResult.OK
    }

    override fun createStep(
        projectGenerator: DirectoryProjectGenerator<ConfigurationData>,
        callback: AbstractNewProjectStep.AbstractCallback<ConfigurationData>
    ): AbstractActionWithPanel = CjProjectSettingsStep(projectGenerator)

}
