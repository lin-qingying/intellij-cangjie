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

package org.cangnova.cangjie.cjpm.build

import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.cjpm.project.model.toml.CjpmTomlConfig
import org.cangnova.cangjie.model.CjBuildConfiguration
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * CJPM 构建配置实现
 */
class CjpmBuildConfigurationImpl(
    private val config: CjpmTomlConfig?,
    private val projectDir: VirtualFile
) : CjBuildConfiguration {

    override val name: String = "CJPM Configuration"

    override val sourceDirs: List<Path>
        get() {
            val srcDir = config?.`package`?.srcDir ?: "src"
            return listOf(Path(projectDir.path, srcDir))
        }

    override val resourceDirs: List<Path> = emptyList()

    override val outputDir: Path
        get() {
            val targetDir = config?.`package`?.targetDir ?: "target"
            return Path(projectDir.path, targetDir)
        }

    override val testSourceDirs: List<Path> = emptyList()

    override val testResourceDirs: List<Path> = emptyList()

    override val testOutputDir: Path
        get() = Path(outputDir.toString(), "test")

    override val compilerOptions: Map<String, String>
        get() {
            val options = mutableMapOf<String, String>()
            config?.`package`?.compileOption?.let {
                options["compile-option"] = it
            }
            return options
        }

    override val linkerOptions: Map<String, String>
        get() {
            val options = mutableMapOf<String, String>()
            config?.`package`?.linkOption?.let {
                options["link-option"] = it
            }
            return options
        }

    override fun validate(): Boolean {
        return config != null
    }
}