package com.huawei.cangjie.idea.project.tools.projectWizard.wizard


import com.huawei.cangjie.cjpm.CjpmConstants
import com.huawei.cangjie.cjpm.findChild
import com.huawei.cangjie.cjpm.toolchain.cjpm
import com.huawei.cangjie.cjpm.toolchain.tools.Cjpm

import com.huawei.cangjie.idea.newProject.CjCustomTemplate
import com.huawei.cangjie.idea.newProject.CjGenericTemplate
import com.huawei.cangjie.idea.newProject.CjProjectTemplate
import com.huawei.cangjie.idea.newProject.ui.ConfigurationData
import com.huawei.cangjie.idea.run.cjpm.CjpmCommandConfiguration
import com.huawei.cangjie.idea.run.cjpm.CjpmCommandConfigurationType
import com.huawei.cangjie.idea.run.cjpm.runconfig.CjProcessExecutionException
import com.huawei.cangjie.idea.run.cjpm.runconfig.CjResult
import com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool.isHeadlessEnvironment
import com.huawei.cangjie.idea.run.cjpm.runconfig.toPath
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.vfs.VirtualFile

class CangJieModuleBuilder(
    //    模块名
    var moduleName: String? = null,

    //    组织名
//    var organizationName: String? = null,

    //    项目类型
    var projectType: String? = null,

    ) : ModuleBuilder() {
    companion object {
        val LOG = Logger.getInstance(CangJieModuleBuilder::class.java)
    }

    var configurationData: ConfigurationData? = null

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) = createProject(modifiableRootModel)

    fun createProject(modifiableRootModel: ModifiableRootModel, vcs: String? = null) {
        val root = doAddContentEntry(modifiableRootModel)?.file ?: return
        modifiableRootModel.inheritSdk()
        val toolchain = configurationData?.settings?.toolchain
        root.refresh(/* async = */ false, /* recursive = */ true)
        // Just work if user "creates new project" over an existing one.
        if (toolchain != null && root.findChild(CjpmConstants.MANIFEST_FILE) == null) {
            // TODO: rewrite this somehow to fix `Synchronous execution on EDT` exception
            // The problem is that `setupRootModel` is called on EDT under write action
            // so `$ cjpm init` invocation blocks UI thread

            val template = configurationData?.template ?: return
            val cjpm = toolchain.cjpm()
            val project = modifiableRootModel.project
            val name = project.name.replace(' ', '_')

            val generatedFiles = cjpm.makeProject(
                project,
                modifiableRootModel.module,
                root,
                name,
                if (moduleName.isNullOrEmpty()) name else moduleName!!,
//                if (organizationName.isNullOrEmpty()) name else organizationName!!,
                projectType ?: "executable",
//                cjcVersion = toolchain.cjc().version

            ).unwrapOrElse {
                LOG.error(it)
                throw ConfigurationException(it.message)
            }

            project.makeDefaultRunConfiguration(template)
            project.openFiles(generatedFiles)
        }
    }


    override fun isSuitableSdkType(sdkType: SdkTypeId?): Boolean {
        return false
    }

    override fun getModuleType(): ModuleType<*> {
        return CangJieModuleType()
    }

}

//class CangJieModuleBuilder : ModuleBuilder() {
//
//
//
//
//
//
//    companion object {
//        val LOG = Logger.getInstance(CangJieModuleBuilder::class.java)
//    }
//
//    var configurationData: ConfigurationData? = null
//
//    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) = createProject(modifiableRootModel )
//
//    fun createProject(modifiableRootModel: ModifiableRootModel, vcs: String? = null) {
//        val root = doAddContentEntry(modifiableRootModel)?.file ?: return
//        modifiableRootModel.inheritSdk()
//        val toolchain = configurationData?.settings?.toolchain
//        root.refresh(/* async = */ false, /* recursive = */ true)
//        // Just work if user "creates new project" over an existing one.
//        if (toolchain != null && root.findChild(CjpmConstants.MANIFEST_FILE) == null) {
//            // TODO: rewrite this somehow to fix `Synchronous execution on EDT` exception
//            // The problem is that `setupRootModel` is called on EDT under write action
//            // so `$ cjpm init` invocation blocks UI thread
//
//            val template = configurationData?.template ?: return
//            val cjpm = toolchain.cjpm()
//            val project = modifiableRootModel.project
//            val name = project.name.replace(' ', '_')
//
//            val generatedFiles = cjpm.makeProject(
//                project,
//                modifiableRootModel.module,
//                root,
//                name,
//                template,
//                vcs
//            )  .unwrapOrElse {
//                LOG.error(it)
//                throw ConfigurationException(it.message)
//            }
//
//            project.makeDefaultRunConfiguration(template)
//            project.openFiles(generatedFiles)
//        }
//    }
//
//
//    override fun isSuitableSdkType(sdkType: SdkTypeId?): Boolean {
//        return false
//    }
//
//    override fun getModuleType(): ModuleType<*> {
//        return CangJieModuleType()
//    }
//
//
//}

inline fun <T, E> CjResult<T, E>.unwrapOrElse(op: (E) -> T): T = when (this) {
    is CjResult.Ok -> ok
    is CjResult.Err -> op(err)
}

fun Cjpm.makeProject(
    project: Project,
    module: Module,
    baseDir: VirtualFile,
    name: String,
    moduleName: String = name,
//    organizationName: String = name,
    projectType: String? = null,
//    cjcVersion: CjcVersion? = null,
): CjProcessResult<Cjpm.GeneratedFilesHolder> {
    return init(project, module, baseDir, name, moduleName/*, organizationName*/, projectType)

}

fun Cjpm.makeProject(
    project: Project,
    module: Module,
    baseDir: VirtualFile,
    name: String,
    template: CjProjectTemplate,
    vcs: String? = null
): CjProcessResult<Cjpm.GeneratedFilesHolder> {
    return when (template) {
        is CjGenericTemplate -> init(project, module, baseDir, name, template.isBinary, vcs)
        is CjCustomTemplate -> generate(project, module, baseDir, name, template.url, vcs)
    }
}

typealias CjProcessResult<T> = CjResult<T, CjProcessExecutionException>


fun Project.openFiles(files: Cjpm.GeneratedFilesHolder) = invokeLater {
    if (!isHeadlessEnvironment) {
        val navigation = PsiNavigationSupport.getInstance()
        navigation.createNavigatable(this, files.manifest, -1).navigate(false)
        for (file in files.sourceFiles) {
            navigation.createNavigatable(this, file, -1).navigate(true)
        }
    }
}

fun Project.makeDefaultRunConfiguration(template: CjProjectTemplate) {
    val runManager = RunManager.getInstance(this)
    val configurationFactory = DefaultRunConfigurationFactory(runManager, this)

    val configuration = when (template) {
        is CjGenericTemplate.CjpmBinaryTemplate -> configurationFactory.createCjpmRunConfiguration()
        is CjGenericTemplate.CjpmLibraryTemplate -> configurationFactory.createCjpmTestConfiguration()

        is CjCustomTemplate -> return
    }

    runManager.addConfiguration(configuration)
    runManager.selectedConfiguration = configuration
}

private class DefaultRunConfigurationFactory(val runManager: RunManager, val project: Project) {
    private val cjpmProjectName = project.name.replace(' ', '_')

    fun createCjpmRunConfiguration(): RunnerAndConfigurationSettings =
        runManager.createConfiguration("Run", CjpmCommandConfigurationType.instance.factory).apply {
            (configuration as? CjpmCommandConfiguration)?.apply {
                command = "run"
                workingDirectory = project.basePath?.toPath()
            }
        }

    fun createCjpmTestConfiguration(): RunnerAndConfigurationSettings =
        runManager.createConfiguration("Test", CjpmCommandConfigurationType.instance.factory).apply {
            (configuration as? CjpmCommandConfiguration)?.apply {
                command = "test $cjpmProjectName"
                workingDirectory = project.basePath?.toPath()
            }
        }


}