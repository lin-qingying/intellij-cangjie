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

import cn.cangnova.cangjie.cjpm.project.toPath
import cn.cangnova.cangjie.cjpm.project.toPathOrNull
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.EnvironmentUtil
import java.nio.file.Path
import kotlin.io.path.isDirectory


internal class CjpmToolchainFlavor : CjToolchainFlavor() {
    override fun getHomePathCandidates(): Sequence<Path> {
        val cjpmHome = EnvironmentUtil.getValue("CANGJIE_HOME")?.toPathOrNull()
        val userHome = FileUtil.expandUserHome("~/.cangjie/").toPath()
        return sequenceOf(cjpmHome, userHome)
            .filterNotNull()
//            .map { it.resolve("tools") }
            .filter { it.isDirectory() }
    }
}
