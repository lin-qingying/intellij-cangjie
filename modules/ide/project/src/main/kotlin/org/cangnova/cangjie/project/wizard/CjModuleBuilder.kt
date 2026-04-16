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

package org.cangnova.cangjie.project.wizard

import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.project.service.CjProjectsService
import org.cangnova.cangjie.project.service.GeneratedFilesHolder
import org.cangnova.cangjie.toolchain.api.CjSdk

data class ConfigurationData(
    val settings: Data,
    val projectType: String
) {
    data class Data(
        val toolchain: CjSdk?,
//        val explicitPathToStdlib: String?
    )
}

/**
 * 仓颉项目模块构建器基类
 *
 * 提供创建新项目/模块的通用框架，具体实现由子类提供
 */
abstract class CjModuleBuilder : ModuleBuilder() {
    companion object {
        val LOG = Logger.getInstance(CjModuleBuilder::class.java)

    }

    /**
     * 项目配置数据，由 UI 层设置
     */
    abstract var configurationData: ConfigurationData?

    /**
     * 设置根模型
     */
    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        val contentEntry = doAddContentEntry(modifiableRootModel)
        val rootDir = contentEntry?.file ?: return

        // 继承 SDK
        modifiableRootModel.inheritSdk()

        // 刷新文件系统
        rootDir.refresh(false, true)

        // 创建项目结构
        val success = createProjectStructure(rootDir, modifiableRootModel)

        if (success) {
            // 将项目注册到 CjProjectsService
            val project = modifiableRootModel.project
            val projectsService = CjProjectsService.getInstance(project)

            // 发现并注册新创建的项目
            projectsService.discoverProject(rootDir)


        }
    }

    /**
     * 创建项目结构
     *
     * @param rootDir 项目根目录
     * @param model 模块根模型
     * @return 是否创建成功
     */
    protected abstract fun createProjectStructure(
        rootDir: VirtualFile,
        model: ModifiableRootModel
    ): Boolean

    /**
     * 获取模块类型
     */
    open override fun getModuleType(): ModuleType<*> {
        return CangJieModuleType()
    }


    /**
     * 是否可用
     */
    override fun isAvailable(): Boolean = false
}

fun Project.openFiles(files: GeneratedFilesHolder) = invokeLater {
    if (!ApplicationManager.getApplication().isHeadlessEnvironment) {
        val navigation = PsiNavigationSupport.getInstance()
        navigation.createNavigatable(this, files.manifest, -1).navigate(false)
        for (file in files.sourceFiles) {
            navigation.createNavigatable(this, file, -1).navigate(true)
        }
    }
}
