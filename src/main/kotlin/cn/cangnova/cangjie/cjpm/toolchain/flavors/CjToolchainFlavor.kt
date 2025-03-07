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

package cn.cangnova.cangjie.cjpm.toolchain.flavors

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.SystemInfo
import cn.cangnova.cangjie.cjpm.toolchain.flavors.CjToolchainFlavor.Companion.getApplicableFlavors
import cn.cangnova.cangjie.cjpm.toolchain.tools.Cjc
import cn.cangnova.cangjie.cjpm.toolchain.tools.Cjpm
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory

fun Path.pathToExecutable(toolName: String): Path {
    val exeName = if (SystemInfo.isWindows) "$toolName.exe" else toolName
    return resolve(exeName).toAbsolutePath()
}

fun Path.hasExecutable(toolName: String): Boolean = pathToExecutable(toolName).isExecutable()
fun Path.isExecutable(): Boolean = Files.isExecutable(this)

abstract class CjToolchainFlavor {

    fun suggestHomePaths(): Sequence<Path> = getHomePathCandidates().filter { isValidToolchainPath(it) }
    protected abstract fun getHomePathCandidates(): Sequence<Path>
    protected open fun pathToExecutable(
        path: Path,
        toolName: String
    ): Path {
        return path.pathToExecutable(toolName)

    }


    protected open fun isValidToolchainPath(path: Path): Boolean {
        return path.isDirectory() &&
                hasExecutable(path, Cjc.NAME) &&
                hasExecutable(path, Cjpm.NAME)
    }

    protected open fun hasExecutable(path: Path, toolName: String): Boolean = path.hasExecutable(toolName)

    /**
     * Flavor is added to result in [getApplicableFlavors] if this method returns true.
     * @return whether this flavor is applicable.
     */
    protected open fun isApplicable(): Boolean = true

    companion object {
        private val EP_NAME: ExtensionPointName<CjToolchainFlavor> =
            ExtensionPointName.create("cn.cangnova.cangjie.toolchainFlavor")

        fun getApplicableFlavors(): List<CjToolchainFlavor> =
            EP_NAME.extensionList.filter { it.isApplicable() }

        fun getFlavor(path: Path): CjToolchainFlavor? =
            getApplicableFlavors().find { flavor -> flavor.isValidToolchainPath(path) }
    }
}
