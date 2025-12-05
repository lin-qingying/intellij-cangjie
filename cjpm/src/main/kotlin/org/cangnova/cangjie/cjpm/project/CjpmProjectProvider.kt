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
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.CjConstants
import org.cangnova.cangjie.cjpm.CjpmConstants
import org.cangnova.cangjie.project.extension.CjProjectProvider
import org.cangnova.cangjie.project.extension.ProjectBuildSystemId
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.model.roots
import org.cangnova.cangjie.project.service.GeneratedFilesHolder
import org.cangnova.cangjie.result.CjProcessExecutionException
import org.cangnova.cangjie.result.CjProcessResult
import org.cangnova.cangjie.result.CjResult
import org.cangnova.cangjie.result.unwrapOrElse
import org.cangnova.cangjie.toolchain.command.ToolchainCommandLine
import org.cangnova.cangjie.utils.invokeAndWaitIfNeeded
import org.cangnova.cangjie.utils.pathAsPath

/**
 * CJPM 项目提供者实现
 */
class CjpmProjectProvider : CjProjectProvider {
    companion object {
        private const val MANIFEST_FILE = "cjpm.toml"
    }

    override fun getBuildSystemId(): ProjectBuildSystemId = CjpmBuildSystemId

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
        projectType: String,
        name: String?
    ): CjProcessResult<GeneratedFilesHolder> {


        return createProjectFilesDirectly(
            project, directory, projectType, name, sdkId
        )

        //调用init命令创建

        val path = directory.pathAsPath
        val crateType = "--type=$projectType"
        val args = mutableListOf<String>()

        args.add(crateType)
        val projectName = name ?: project.name
        args.add("--name=${projectName}")

        // 执行命令时不阻塞EDT线程
        val commandLine = ToolchainCommandLine(sdkId, "tools/bin/cjpm", "init", path, args)
        val result = with(commandLine) {
            val generalCommandLine = toGeneralCommandLine(project)
            generalCommandLine.execute(
                owner = owner,
                stdIn = null,
                runner = {
                    // 使用当前的 progress indicator 而非全局 indicator
                    val indicator = ProgressManager.getInstance().progressIndicator
                    runProcess(indicator, null)
                },
                listener = null
            )
        }.unwrapOrElse { return CjResult.Err(it) }

        // 同步刷新目录,确保文件可见
        directory.refresh(/* asynchronous = */ false, /* recursive = */ true)

        val manifest = runWriteAction {
            checkNotNull(directory.findChild(CjpmConstants.MANIFEST_FILE)) { "Can't find the manifest file" }
        }

        val fileName = CjConstants.MAIN_CJ_FILE
        val sourceFiles = runWriteAction {
            listOfNotNull(directory.findFileByRelativePath("src/$fileName"))
        }

        return CjResult.Ok(GeneratedFilesHolder(manifest, sourceFiles))
    }

    private fun createProjectFilesDirectly(
        project: Project,
        directory: VirtualFile,
        projectType: String,
        name: String?,
        sdkId: String
    ): CjProcessResult<GeneratedFilesHolder> {
        val projectName = name ?: project.name

        // 获取SDK版本
        val sdk = org.cangnova.cangjie.toolchain.api.CjSdkRegistry.getInstance().getSdk(sdkId)
        val cjcVersion = sdk?.version?.semver?.toString() ?: "0.25.1"

        return try {
            // 使用 invokeAndWait 确保在 EDT 线程上执行 VFS 操作
            val holder: GeneratedFilesHolder = invokeAndWaitIfNeeded {
                runWriteAction {
                    // 创建 cjpm.toml 文件
                    val tomlContent = """
                        [package]
                          cjc-version = "$cjcVersion"
                          name = "$projectName"
                          description = "nothing here"
                          version = "1.0.0"
                          targetPlatform-dir = "targetPlatform"
                          src-dir = "src"
                          output-type = "$projectType"
                          compile-option = ""
                          override-compile-option = ""
                          link-option = ""
                          package-configuration = {}

                        [dependencies]

                    """.trimIndent()

                    val manifestFile = directory.findOrCreateChildData(this, CjpmConstants.MANIFEST_FILE)
                    manifestFile.setBinaryContent(tomlContent.toByteArray(Charsets.UTF_8))

                    // 创建 src 目录
                    val srcDir = directory.findChild("src") ?: directory.createChildDirectory(this, "src")

                    // 如果是可执行项目，创建 main.cj
                    val sourceFiles = if (projectType == "executable") {
                        val mainContent = """
                            package $projectName

                            main(): Int64 {
                                println("hello world")
                                return 0
                            }

                        """.trimIndent()

                        val mainFile = srcDir.findOrCreateChildData(this, "main.cj")
                        mainFile.setBinaryContent(mainContent.toByteArray(Charsets.UTF_8))
                        listOf(mainFile)
                    } else {
                        emptyList()
                    }

                    // 刷新目录
                    directory.refresh(false, true)

                    GeneratedFilesHolder(manifestFile, sourceFiles)
                }
            }
            CjResult.Ok(holder)
        } catch (e: Exception) {
            CjResult.Err(CjProcessExecutionException.FileCreation(e))
        }
    }

    override fun getIndexableDirectories(project: CjProject): List<VirtualFile> {
        val directories = mutableListOf<VirtualFile>()

        // 添加项目根目录
        directories.add(project.rootDir)

        // 先获取到局部变量，避免多次访问属性导致的竞态条件
        val workspace = project.workspace
        val module = project.module

        if (workspace != null) {
            // 添加所有工作空间模块的源码目录
            for (workspaceModule in workspace.modules) {
                for (sourceSet in workspaceModule.sourceSets) {
                    directories.addAll(sourceSet.roots)
                }
            }
        } else if (module != null) {
            // 添加单模块的源码目录
            for (sourceSet in module.sourceSets) {
                directories.addAll(sourceSet.roots)
            }
        }

        return directories
    }
}