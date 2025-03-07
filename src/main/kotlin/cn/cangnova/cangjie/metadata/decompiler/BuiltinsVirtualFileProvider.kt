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

package cn.cangnova.cangjie.metadata.decompiler

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import cn.cangnova.cangjie.builtins.StandardNames.ALL_NAMES
import cn.cangnova.cangjie.cjpm.toolchain.CjToolchainBase
import cn.cangnova.cangjie.cjpm.toolchain.cjc
import cn.cangnova.cangjie.ide.run.cjpm.toolchain
import cn.cangnova.cangjie.name.StandardClassIds
import cn.cangnova.cangjie.serialization.deserialization.BuiltInSerializerProtocol
import cn.cangnova.cangjie.utils.exceptions.errorWithAttachment
import java.net.URL
import java.nio.file.Path


abstract class BuiltInsVirtualFileProvider(val project: Project) {
    abstract fun getBuiltinVirtualFiles(): Set<VirtualFile>

    abstract fun createBuiltinsScope(project: Project): GlobalSearchScope

    companion object {
        fun getInstance(project: Project): BuiltInsVirtualFileProvider = project.service<BuiltInsVirtualFileProvider>()
//            ApplicationManager.getApplication().getService(BuiltInsVirtualFileProvider::class.java)
    }
}

internal class IdeBuiltInsVirtualFileProviderImpl(project: Project) : BuiltInsVirtualFileProviderBaseImpl(project) {
    override fun findVirtualFile(url: URL): VirtualFile? {
        return VfsUtil.findFileByURL(url)
    }
}

fun getProject(): Project {

    return ProjectManager.getInstance().openProjects[0]
}

abstract class BuiltInsVirtualFileProviderBaseImpl(project: Project) : BuiltInsVirtualFileProvider(project) {
    private val stdlibPathByVersion: Path get() = CjToolchainBase.stdlibPath.resolve(toolchainVsrsion)
    private fun getURL(resourcePath: String): URL {
        return CjToolchainBase.stdlibPath.resolve(resourcePath).toUri().toURL()
    }

    private val toolchainVsrsion: String
        get() {
            return runReadAction {
                project.toolchain?.cjc()?.version?.semver?.rawVersion ?: ""
            }
        }

    private val builtInUrls: Set<URL> by lazy {

        val classLoader = this::class.java.classLoader
        ALL_NAMES.drop(1).mapTo(mutableSetOf()) { builtInPackageFqName ->
            val resourcePath = BuiltInSerializerProtocol.getBuiltInsFilePath(builtInPackageFqName)

//            getURL(resourcePath)
            classLoader.getResource("stdlib/$resourcePath")
                ?: errorWithAttachment("Resource for builtin $builtInPackageFqName not found") {
                    withEntry("resourcePath", resourcePath)
                }
        }
    }

    override fun createBuiltinsScope(project: Project): GlobalSearchScope {
        val builtInFiles = getBuiltinVirtualFiles()
        return GlobalSearchScope.filesScope(project, builtInFiles)
    }

    protected abstract fun findVirtualFile(url: URL): VirtualFile?

    override fun getBuiltinVirtualFiles(): Set<VirtualFile> = builtInUrls.mapNotNull { url ->
        findVirtualFile(url)
//            ?: errorWithAttachment("Virtual file for builtin is not found") {
//                withEntry("resourceUrl", url) { it.toString() }
//            }
    }.toSet()
}
