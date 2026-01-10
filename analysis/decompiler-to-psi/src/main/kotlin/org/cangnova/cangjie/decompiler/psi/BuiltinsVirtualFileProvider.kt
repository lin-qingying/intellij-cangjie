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

package org.cangnova.cangjie.decompiler.psi

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.application
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.builtins.StandardNames.ALL_NAMES
import org.cangnova.cangjie.serialization.deserialization.BuiltInSerializerFlatbuffers
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import java.nio.file.Path
import kotlin.io.path.Path

abstract class BuiltinsVirtualFileProvider {
    abstract fun getBuiltinVirtualFiles(project: Project): Set<VirtualFile>

    abstract fun createBuiltinsScope(project: Project): GlobalSearchScope

    companion object {
        fun getInstance( ): BuiltinsVirtualFileProvider =
            application.getService(BuiltinsVirtualFileProvider::class.java)
    }
}

abstract class BuiltinsVirtualFileProviderBaseImpl : org.cangnova.cangjie.decompiler.psi.BuiltinsVirtualFileProvider() {

    private fun getBuiltInUrls(project: Project): Set<Path> {

        return ALL_NAMES.filter { it != StandardNames.BASIC_PACKAGE_FQ_NAME }
            .mapNotNull { builtInPackageFqName ->
                val resourcePath = BuiltInSerializerFlatbuffers.getBuiltInsFilePath(
                    builtInPackageFqName,
                    CjProjectSdkConfig.getInstance(project).getProjectSdk()
                )
                resourcePath?.let { Path(it) }
            }.toSet()
    }

    override fun createBuiltinsScope(project: Project): GlobalSearchScope {
        val builtInFiles = getBuiltinVirtualFiles(project)
        return GlobalSearchScope.filesScope(project, builtInFiles)
    }

    protected abstract fun findVirtualFile(url: Path): VirtualFile?

    override fun getBuiltinVirtualFiles(project: Project): Set<VirtualFile> {
        val builtInUrls = getBuiltInUrls(project)
        return builtInUrls.mapNotNull { url ->
            findVirtualFile(url)
        }.toSet()
    }
}

internal class IdeBuiltInsVirtualFileProviderImpl : BuiltinsVirtualFileProviderBaseImpl() {
    override fun findVirtualFile(url: Path): VirtualFile? {
        return VfsUtil.findFile(url, true)
    }
}