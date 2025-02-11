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

package com.linqingying.cangjie.ide.module


import com.linqingying.cangjie.cjpm.CjpmConstants
import com.linqingying.cangjie.cjpm.project.settings.cangjieSettings
import com.linqingying.cangjie.cjpm.toolchain.cjpm
import com.linqingying.cangjie.cjpm.toolchain.tools.Cjpm


import com.linqingying.cangjie.ide.newProject.ui.ConfigurationData
import com.linqingying.cangjie.ide.run.cjpm.CjpmCommandConfiguration
import com.linqingying.cangjie.ide.run.cjpm.CjpmCommandConfigurationType
import com.linqingying.cangjie.ide.run.cjpm.runconfig.CjProcessExecutionException
import com.linqingying.cangjie.ide.run.cjpm.runconfig.CjResult
import com.linqingying.cangjie.ide.run.cjpm.runconfig.buildtool.isHeadlessEnvironment
import com.linqingying.cangjie.ide.run.cjpm.runconfig.toPath
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
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.cangjie.cjpm.toolchain.tools.Cjpm.GeneratedFilesHolder

class CangJieModuleBuilder : ModuleBuilder() {
    companion object {
        val LOG = Logger.getInstance(CangJieModuleBuilder::class.java)
    }

    var configurationData: ConfigurationData? = null

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) = createProject(modifiableRootModel)

    fun createProject(modifiableRootModel: ModifiableRootModel, vcs: String = "git") {
        // 添加内容入口
        val root = doAddContentEntry(modifiableRootModel)?.file ?: return
//        继承sdk
        modifiableRootModel.inheritSdk()
        val toolchain = configurationData?.settings?.toolchain

        val projectType = configurationData?.projectType
        root.refresh(/* async = */ false, /* recursive = */ true)
        // 仅当用户在现有项目上“创建新项目”时才起作用。
        if (toolchain != null && root.findChild(CjpmConstants.MANIFEST_FILE) == null) {
            // TODO: 以某种方式重写此代码以修复 `在EDT上的同步执行` 异常
            // 问题在于 `setupRootModel` 在写操作下在EDT上被调用
            // 所以 `$ cjpm init` 调用会阻塞UI线程


            val cjpm = toolchain.cjpm()
            val project = modifiableRootModel.project
            val name = project.name.replace(' ', '_')


                val generatedFiles = cjpm.makeProject(
                    project,
                    modifiableRootModel.module,
                    root,
                    name,
                    name,
                    projectType ?: "executable",

                    ).unwrapOrElse {
                    LOG.error(it)
                    throw ConfigurationException(it.message)
                }


//            设置工具链
                project.cangjieSettings.modify {
                    it.toolchain = toolchain
                }

                project.openFiles(generatedFiles)

        }
    }

    override fun isAvailable(): Boolean = false


    override fun getModuleType(): ModuleType<*> {
        return CangJieModuleType()
    }

}


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
    projectType: String? = null,
): CjProcessResult<Cjpm.GeneratedFilesHolder> {
    return init(project, module, baseDir, name, moduleName/*, organizationName*/, projectType)

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
