package com.linqingying.cangjie.byversion.ide.project.projectWizard

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.cjpm.project.toPathOrNull
import com.linqingying.cangjie.icon.CangJieIcons
import com.linqingying.cangjie.ide.newProject.CjProjectGeneratorPeer
import com.linqingying.cangjie.ide.project.tools.projectWizard.wizard.CangJieModuleBuilder
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.GitNewProjectWizardData.Companion.gitData
import com.intellij.ide.wizard.NewProjectWizardLanguageStep
import com.intellij.ide.wizard.NewProjectWizardMultiStepFactory
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.language.LanguageGeneratorNewProjectWizard
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.validation.DialogValidation
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.jetbrains.rd.util.getThrowableText
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.Icon

class CangJieGeneratorNewProjectWizard : LanguageGeneratorNewProjectWizard {

    companion object {
        private const val GITIGNORE: String = ".gitignore"

        private fun createGitIgnoreFile(projectDir: Path, module: Module) {
            val directory = VfsUtil.createDirectoryIfMissing(projectDir.toString()) ?: return
            val existingFile = directory.findChild(GITIGNORE)
            if (existingFile != null) return
            val file = directory.createChildData(module, GITIGNORE)
            VfsUtil.saveText(file, "/build\n")
        }

    }

    override val icon: Icon = CangJieIcons.CANGJIE_16
    override val name: String = CangJieBundle.message("cangjie")

    override fun createStep(parent: NewProjectWizardStep): NewProjectWizardStep =
        Step(parent as NewProjectWizardLanguageStep)

    class Step(parent: NewProjectWizardLanguageStep) : AbstractNewProjectWizardStep(parent) {


        private val peer: CjProjectGeneratorPeer = CjProjectGeneratorPeer(parent.path.toPathOrNull() ?: Paths.get("."))
        override fun setupProject(project: Project) {

            val builder = CangJieModuleBuilder(
//                moduleName = moduleNameTextField.text,
//                organizationName = groupIdTextField.text,
//                projectType = (projectTypeComboBox.selectedItem as CangJieProjectTypeItem).type
            )
            val module = builder.commit(project)?.firstOrNull() ?: return
//
            ModuleRootModificationUtil.updateModel(module) { rootModel ->
                builder.configurationData = peer.settings


                try {
                    builder.createProject(rootModel)
                } catch (e: ConfigurationException) {
//                    捕获cjpm执行错误
//                    弹窗向用户报告错误

                    var message =
                        e.getThrowableText().replace("com.intellij.openapi.options.ConfigurationException: ", "")
                    message = message.substring(0, message.indexOf("stderr : "))
                    MessageDialogBuilder.yesNo(e.title, message)


                    return@updateModel
                }


                if (gitData?.git == true) runWriteAction {
                    createGitIgnoreFile(context.projectDirectory, module)
                }
            }


        }

        override fun setupUI(builder: Panel) {


            with(builder) {

                row {
                    cell(peer.component).align(Align.FILL)

                        .validationRequestor { peer.checkValid = Runnable(it) }
                        .validation(DialogValidation { peer.validate() })

                }
//                row(CangJieUiBundle.message("action.new.project.projecttype.title")) {
//                    cell(projectTypeComboBox)
//                        .columns(COLUMNS_MEDIUM)
//                        .component
//                }.bottomGap(BottomGap.SMALL)
//                row(CangJieUiBundle.message("action.new.project.modulename.title")) {
//                    cell(moduleNameTextField)
//                        .columns(COLUMNS_MEDIUM)
//                        //                        .validationOnApply {
////                            validateModuleName(moduleNameTextField.text)
////                        }
//                        .component
//                }.bottomGap(BottomGap.SMALL)
//                row(CangJieUiBundle.message("action.new.project.groupname.title")) {
//                    cell(groupIdTextField)
//                        .columns(COLUMNS_MEDIUM)
////                        .validationOnApply {
////                            validateGroupName(groupIdTextField.text)
////                        }
//                        .component
//                }.bottomGap(BottomGap.SMALL)
            }


        }
    }
}


interface BuildSystemCangJieNewProjectWizard : NewProjectWizardMultiStepFactory<CangJieGeneratorNewProjectWizard.Step> {
    companion object {
        var EP_NAME =
            ExtensionPointName<BuildSystemCangJieNewProjectWizard>("com.intellij.newProjectWizard.CangJie.buildSystem")
    }
}
