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

package cn.cangnova.cangjie.cjpm.project.workspace

import CjpmWorkspaceData
import PackageId
import cn.cangnova.cangjie.cjpm.project.model.CjcInfo
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import cn.cangnova.cangjie.utils.FileUtils
import cn.cangnova.cangjie.utils.FileUtils.getTopLevelDirectories
import java.nio.file.Path

//
data class StandardLibrary(
    val workspaceData: CjpmWorkspaceData,
    val isHardcoded: Boolean,
    val isPartOfCjpmProject: Boolean = false
) {
    val packages: List<CjpmWorkspaceData.Package> get() = workspaceData.packages

    companion object {
        fun fromFileStdlib(path: Path, version: String): StandardLibrary {
// TODO 标准库验证
            if (!FileUtils.verifyFileList(path)) {
                throw IllegalArgumentException("Invalid standard library file")
            }


            val packages = mutableMapOf<PackageId, CjpmWorkspaceData.Package>()
            val currentFileList = FileUtils.generateFileList(path, false)
            val nameSet = currentFileList.getTopLevelDirectories()
//            val dependencies = mutableMapOf<PackageId, MutableSet<CjpmWorkspaceData.Dependency>>()
//            val version = "0.53.4"

            for (name in nameSet) {
                packages[name] = CjpmWorkspaceData.Package(
                    path.resolve(name).toVirtualFile().toString(),
                    name, version, PackageOrigin.STDLIB
                )


            }
            val data = CjpmWorkspaceData(packages.values.toList(), path.toVirtualFile().toString())
            return StandardLibrary(workspaceData = data, isHardcoded = true)

        }
    }
}

fun Path.toVirtualFile(): VirtualFile? {
    return VfsUtil.findFile(this, true)
}

object AutoInjectedCrates {
    const val STD: String = "std"
    const val CORE: String = "core"
    const val TEST: String = "test"
}
