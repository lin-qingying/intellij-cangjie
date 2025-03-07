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

package cn.cangnova.cangjie.config

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import cn.cangnova.cangjie.cli.messages.CompilerMessageSeverity
import cn.cangnova.cangjie.cli.messages.MessageCollector
import cn.cangnova.cangjie.lang.CangJieFileType
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.cli.messages.CompilerMessageLocation
import cn.cangnova.cangjie.lang.declarations.CangJieDeclarationsFileType

import java.io.File


fun createSourceFilesFromSourceRoots(
    configuration: CompilerConfiguration,
    project: Project,
    sourceRoots: List<CangJieSourceRoot>,
    reportLocation: CompilerMessageLocation? = null
): MutableList<CjFile> {
    val psiManager = PsiManager.getInstance(project)
    val result = mutableListOf<CjFile>()
    sourceRoots.forAllFiles(configuration, project, reportLocation) { virtualFile, isCommon, moduleName ->
        psiManager.findFile(virtualFile)?.let {
            if (it is CjFile) {
//                it.isCommonSource = isCommon
//                if (moduleName != null) {
//                    it.hmppModuleName = moduleName
//                }
                result.add(it)
            }
        }
    }
    return result
}
fun getSourceRootsCheckingForDuplicates(configuration: CompilerConfiguration, messageCollector: MessageCollector?): List<CangJieSourceRoot> {
    val uniqueSourceRoots = hashSetOf<String>()
    val result = mutableListOf<CangJieSourceRoot>()

    for (root in configuration.cangjieSourceRoots) {
        if (!uniqueSourceRoots.add(root.path)) {
            messageCollector?.report(CompilerMessageSeverity.STRONG_WARNING, "Duplicate source root: ${root.path}")
        }
        result.add(root)
    }

    return result
}
fun checkFileByType(vFile:VirtualFile):Boolean {
    return vFile.fileType !is CangJieFileType    && vFile.fileType !is CangJieDeclarationsFileType
}
fun checkFileByExtension(vFile:VirtualFile):Boolean {
    return vFile.extension != CangJieFileType.EXTENSION   && vFile.extension != CangJieDeclarationsFileType.EXTENSION
}
fun List<CangJieSourceRoot>.forAllFiles(
    configuration: CompilerConfiguration,
    project: Project,
    reportLocation: CompilerMessageLocation? = null,
    body: (VirtualFile, Boolean, moduleName: String?) -> Unit
) {
    val localFileSystem = VirtualFileManager.getInstance()
        .getFileSystem(StandardFileSystems.FILE_PROTOCOL)

    val processedFiles = hashSetOf<VirtualFile>()

    val virtualFileCreator = PreprocessedFileCreator(project)

    var pluginsConfigured = false
    fun ensurePluginsConfigured() {
        if (!pluginsConfigured) {
            for (extension in CompilerConfigurationExtension.getInstances(project)) {
                extension.updateFileRegistry()
            }
            pluginsConfigured = true
        }
    }

    for ((sourceRootPath, isCommon, hmppModuleName) in this) {
        val sourceRoot = File(sourceRootPath)
        val vFile = localFileSystem.findFileByPath(sourceRoot.normalize().path)
        if (vFile == null) {
            val message = "Source file or directory not found: $sourceRootPath"


            configuration.report(CompilerMessageSeverity.ERROR, message, reportLocation)
            continue
        }

        if (!vFile.isDirectory && checkFileByExtension(vFile)) {
            ensurePluginsConfigured()
            if (checkFileByExtension(vFile) ) {
                configuration.report(CompilerMessageSeverity.ERROR, "Source entry is not a CangJie file: $sourceRootPath", reportLocation)
                continue
            }
        }

        for (file in sourceRoot.walkTopDown()) {
            if (!file.isFile) continue

            val virtualFile = localFileSystem.findFileByPath(file.absoluteFile.normalize().path)?.let(virtualFileCreator::create)
            if (virtualFile != null && processedFiles.add(virtualFile)) {
                if (checkFileByExtension(vFile)) {
                    ensurePluginsConfigured()
                }
                if (!checkFileByExtension(virtualFile)|| !checkFileByType(virtualFile)  ) {
                    body(virtualFile, isCommon, hmppModuleName)
                }
            }
        }
    }
}
fun CompilerConfiguration.report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation? = null) {
    messageCollector.report(severity, message, location)
}
var CompilerConfiguration.messageCollector: MessageCollector
    get() = get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    set(value) = put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, value)
