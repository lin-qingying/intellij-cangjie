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

package org.cangnova.cangjie.toolchain

import org.cangnova.cangjie.toolchain.flavors.hasExecutable
import org.cangnova.cangjie.toolchain.flavors.isExecutable
import org.cangnova.cangjie.toolchain.flavors.pathToExecutable
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.nio.file.Path
import kotlin.io.path.isDirectory


open class CjLocalToolchain(location: Path) : CjToolchainBase(location) {
    companion object {
        fun create(homePath: Path): CjLocalToolchain? {
            val tool = CjLocalToolchain(homePath)

            if (tool.toolsPath.isDirectory()) {
                return tool

            }
            return null
        }
    }

    override fun pathToExecutable(toolName: String): Path = location.pathToExecutable(toolName)


    override val fileSeparator: String
        get() = File.separator
    override val executionTimeoutInMilliseconds: Int
        get() = 1000

    override fun hasExecutable(exec: String): Boolean {
        return location.hasExecutable(exec)
    }

    override fun hasCjpmExecutable(exec: String): Boolean = pathToCjpmExecutable(exec).isExecutable()
    override val platformType: String = "local"

    override fun patchCommandLine(commandLine: GeneralCommandLine): GeneralCommandLine = commandLine
    override fun toLocalPath(remotePath: String): String {
        return remotePath
    }

    override fun toRemotePath(localPath: String): String = localPath

    override fun expandUserHome(remotePath: String): String = FileUtil.expandUserHome(remotePath)

    override fun getExecutableName(toolName: String): String = if (SystemInfo.isWindows) "$toolName.exe" else toolName
}

