package com.huawei.cangjie.idea.project.tools.projectWizard.wizard

import com.huawei.cangjie.idea.icons.CangJieIcons
import com.huawei.cangjie.lang.sdk.CangJieSdkType
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel

class NewProjectWizardModuleBuilder : ModuleBuilder() {
    lateinit var wizardContext: WizardContext
    private var finishButtonClicked: Boolean = false

    var projectSdk: Sdk? = null


    override fun createWizardSteps(
        wizardContext: WizardContext,
        modulesProvider: ModulesProvider
    ): Array<ModuleWizardStep> {
        this.wizardContext = wizardContext
        val disposable = wizardContext.disposable
//        return arrayOf(ModuleNewWizardSecondStep(wizardContext, disposable))
        return super.createWizardSteps(wizardContext, modulesProvider)
    }

    override fun commit(
        project: Project,
        model: ModifiableModuleModel?,
        modulesProvider: ModulesProvider?
    ): MutableList<Module>? {
        if (projectSdk == null) {
            return super.commit(project, model, modulesProvider)
        }



        runWriteAction {
//更改项目使用的sdk
            val rootManager = ProjectRootManagerEx.getInstanceEx(project)
            rootManager.projectSdk = projectSdk
        }



        ApplicationManager.getApplication().executeOnPooledThread {

            // 执行cjpm init
            val commandLine = GeneralCommandLine().apply {
                exePath = (projectSdk!!.sdkType as CangJieSdkType).sdkAdditionalData.cjpmPath.toString()


                addParameter("init")
                addParameter("a")
                addParameter("b")
            }
            commandLine.setWorkDirectory(project.basePath)
            val processHandler = CapturingProcessHandler(commandLine)
            val output = processHandler.runProcess()

            if (output.exitCode != 0) {
                // 处理错误
                println("Error: ${output.stderr}")
            }


        }


//        将项目类型更改为CangJie


        return super.commit(project, model, modulesProvider)
    }

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {


//        将src目录添加为源文件目录
        val contentEntry = doAddContentEntry(modifiableRootModel)
        contentEntry?.addSourceFolder(contentEntry.url + "/src", false)

        val projectSdk = projectSdk
        if (projectSdk != null) {
            modifiableRootModel.sdk = projectSdk
        }
//        // 打开文件
//        ApplicationManager.getApplication().invokeLater {
//            val project = modifiableRootModel.project
//            val basePath = project.basePath
//            if (basePath != null) {
//                val filePath = basePath + "/src/main.cj"
//                val file = LocalFileSystem.getInstance().findFileByPath(filePath)
//                if (file != null) {
//                    FileEditorManager.getInstance(project).openFile(file, true)
//                }
//            }
//        }


    }

    override fun isSuitableSdkType(sdkType: SdkTypeId?): Boolean {
        return false
    }

    override fun getModuleType(): ModuleType<*> {
        return NewProjectWizardModuleType()
    }


}

class NewProjectWizardModuleType : ModuleType<NewProjectWizardModuleBuilder>("id1") {
    override fun getName(): String = "abc"
    override fun getDescription(): String = name
    override fun getNodeIcon(isOpened: Boolean): Icon = CangJieIcons.SMALL_LOGO
    override fun createModuleBuilder(): NewProjectWizardModuleBuilder = NewProjectWizardModuleBuilder()
}

class ModuleNewWizardSecondStep(

    private val wizardContext: WizardContext,
    disposable: Disposable
) : WizardStep() {


    override fun getComponent(): JComponent {
        return JLabel("abc")
    }

}

abstract class WizardStep : ModuleWizardStep() {
    override fun getHelpId(): String = HELP_ID

    override fun updateDataModel() = Unit // model is updated on every UI action
    override fun validate(): Boolean =
        false

//    protected open fun handleErrors(error: ValidationResult.ValidationError) {
//        throw ConfigurationException(error.asHtml(), KotlinNewProjectWizardUIBundle.message("dialog.title.validation.error"))
//    }

    companion object {
        private const val HELP_ID = "new_project_wizard_kotlin"
    }
}
