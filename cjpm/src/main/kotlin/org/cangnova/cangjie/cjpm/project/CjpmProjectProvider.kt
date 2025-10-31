/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.cjpm.project

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.CjConstants
import org.cangnova.cangjie.cjpm.CjpmConstants
import org.cangnova.cangjie.project.extension.CjProjectProvider
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.model.roots
import org.cangnova.cangjie.project.service.GeneratedFilesHolder
import org.cangnova.cangjie.result.CjProcessResult
import org.cangnova.cangjie.result.CjResult
import org.cangnova.cangjie.result.unwrapOrElse
import org.cangnova.cangjie.toolchain.command.ToolchainCommandLine
import org.cangnova.cangjie.utils.fullyRefreshDirectory
import org.cangnova.cangjie.utils.pathAsPath

/**
 * CJPM 项目提供者实现
 */
class CjpmProjectProvider : CjProjectProvider {
    companion object {
        private const val MANIFEST_FILE = "cjpm.toml"
    }

    override val providerName: String = "CJPM"


    override fun canHandle(dir: VirtualFile): Boolean {
        // 检查目录下是否存在 cjpm.toml 文件
        return dir.findChild(MANIFEST_FILE) != null
    }

    override fun createProject(dir: VirtualFile, project: Project): CjProject? {
        val manifestFile = dir.findChild(MANIFEST_FILE) ?: return null

        return try {
            CjpmProjectImpl(
                rootDir = dir,
                intellijProject = project,
                manifestFile = manifestFile
            )
        } catch (e: Exception) {
            null
        }
    }

    override fun createProjectFromPhysicalFile(

        sdkId: String,
        project: Project,
        owner: Disposable,
        directory: VirtualFile,
        projectType: String
    ): CjProcessResult<GeneratedFilesHolder> = runWriteAction {
//调用init命令创建

        val path = directory.pathAsPath
        val crateType = "--type=$projectType"
        val args = mutableListOf<String>()

        args.add(crateType)

        args.add("--name=${project.name}")


        ToolchainCommandLine(sdkId, "tools/bin/cjpm", "init", path, args).execute(project, owner)
            .unwrapOrElse { return@runWriteAction CjResult.Err(it) }
        fullyRefreshDirectory(directory)

        val manifest =
            checkNotNull(directory.findChild(CjpmConstants.MANIFEST_FILE)) { "Can't find the manifest file" }


        val fileName = CjConstants.MAIN_CJ_FILE
        val sourceFiles =
            listOfNotNull(directory.findFileByRelativePath("src/$fileName"))

        CjResult.Ok(GeneratedFilesHolder(manifest, sourceFiles))

    }

    override fun getIndexableDirectories(project: CjProject): List<VirtualFile> {
        val directories = mutableListOf<VirtualFile>()

        // 添加项目根目录
        directories.add(project.rootDir)



        if(project.isWorkspace){
            // 添加所有模块的源码目录和输出目录
            for (module in project.workspace!!.modules) {
                for (sourceSet in module.sourceSets) {
                    directories.addAll(sourceSet.roots)
                }
                for (target in module.targets) {
                    target.outputDirectory?.let { directories.add(it) }
                }
            }

        }else{
            for (sourceSet in project.module!!.sourceSets) {
                directories.addAll(sourceSet.roots)
            }
            for (target in project.module!!.targets) {
                target.outputDirectory?.let { directories.add(it) }
            }
        }

        return directories
    }
}