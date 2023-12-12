package com.huawei.cangjie.idea.project.tools.projectWizard.wizard

import com.huawei.cangjie.idea.run.cjpm.CjpmCommandConfiguration
import com.huawei.cangjie.idea.run.cjpm.CjpmCommandConfigurationType

import com.huawei.cangjie.idea.run.cjpm.CjpmCommand
import com.huawei.cangjie.lang.sdk.CangJieSdkType
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.roots.ui.configuration.ModulesProvider


class CangJieModuleBuilder : ModuleBuilder() {


    var projectSdk: Sdk? = null

    //    模块名
    var moduleName: String? = null

    //    组织名
    var organizationName: String? = null

    //    项目类型
    var projectType: String? = null


//    override fun createWizardSteps(
//        wizardContext: WizardContext,
//        modulesProvider: ModulesProvider
//    ): Array<ModuleWizardStep> {
//        this.wizardContext = wizardContext
//        val disposable = wizardContext.disposable
////        return arrayOf(ModuleNewWizardSecondStep(wizardContext, disposable))
//        return super.createWizardSteps(wizardContext, modulesProvider)
//    }

    override fun commit(
        project: Project,
        model: ModifiableModuleModel?,
        modulesProvider: ModulesProvider?
    ): List<Module>? {
        if (projectSdk == null) {
            return emptyList()
        }


//        val modulesModel = model ?: ModuleManager.getInstance(project).getModifiableModel()

        ApplicationManager.getApplication().executeOnPooledThread {

            // 执行cjpm init
            val commandLine = GeneralCommandLine().apply {
                exePath = (projectSdk!!.sdkType as CangJieSdkType).sdkAdditionalData.cjpmPath.toString()


                addParameter("init")
                addParameter(if (moduleName.isNullOrEmpty()) project.name else moduleName!!)
                addParameter(if (organizationName.isNullOrEmpty()) project.name else organizationName!!)
                addParameter("--type=${if (projectType.isNullOrEmpty()) "executable" else projectType!!}")
            }
            commandLine.setWorkDirectory(project.basePath)
            val processHandler = CapturingProcessHandler(commandLine)
            val output = processHandler.runProcess()

            if (output.exitCode != 0) {
                // 处理错误
                println("Error: ${output.stderr}")
            }

//            为该项目添加一个运行配置  com.huawei.cangjie.idea.run.action.cjpm.configurations.CjpmCommandConfigurationType
            val runManager = RunManager.getInstance(project)
            val runnerAndConfigurationSettings = runManager.createConfiguration(
                "run",
                CjpmCommandConfigurationType::class.java
            )
//            修改运行配置的命令为run
            (runnerAndConfigurationSettings.configuration as CjpmCommandConfiguration).apply {
                command = CjpmCommand.RUN

            }

            runManager.addConfiguration(runnerAndConfigurationSettings)

//            更改项目默认使用该运行配置
            runManager.selectedConfiguration = runnerAndConfigurationSettings

        }
        runWriteAction {
//更改项目使用的sdk
            val rootManager = ProjectRootManagerEx.getInstanceEx(project)
            rootManager.projectSdk = projectSdk
            // 强制刷新项目结构

//            ProjectManager.getInstance().reloadProject(project)
        }




//        return emptyList()
//        return modulesModel.modules.toList().onEach { setupModule(it) }
        return super.commit(project, model, modulesProvider)
    }

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
//        更改模块sdk为继承
        modifiableRootModel.inheritSdk()
//        将src目录添加为源文件目录
        val contentEntry = doAddContentEntry(modifiableRootModel)
        contentEntry?.addSourceFolder("${contentEntry.url}/src", false)
//        设置build目录为输出目录
        contentEntry?.addExcludeFolder("${contentEntry.url}/build")


    }

    override fun isSuitableSdkType(sdkType: SdkTypeId?): Boolean {
        return false
    }

    override fun getModuleType(): ModuleType<*> {
        return CangJieModuleType()
    }


}

//class NewProjectWizardModuleType : ModuleType<CangJieModuleBuilder>(CangJieModuleType.ID) {
//    override fun getName(): String = "CangJieModuleType"
//    override fun getDescription(): String = name
//    override fun getNodeIcon(isOpened: Boolean): Icon = CangJieIcons.SMALL_LOGO
//    override fun createModuleBuilder(): CangJieModuleBuilder = CangJieModuleBuilder()
//
//    override fun isSupportedRootType(type: JpsModuleSourceRootType<*>?): Boolean {
//        return super.isSupportedRootType(type)
//    }
//
//
//}


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
