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

package org.cangnova.cangjie.cjpm.project.model.toml

import org.cangnova.cangjie.cjpm.config.toml.CjpmTomlParser
import java.io.File
import java.nio.file.Path

/**
 * CJPM TOML 配置文件工具类
 */
object CjpmTomlUtils {
    private const val DEFAULT_FILENAME = "cjpm.toml"

    /**
     * 在指定目录中查找 cjpm.toml 文件
     *
     * @param directory 要搜索的目录
     * @return 找到的 cjpm.toml 文件，如果不存在则返回 null
     */
    fun findCjpmToml(directory: File): File? {
        val tomlFile = directory.resolve(DEFAULT_FILENAME)
        return if (tomlFile.exists() && tomlFile.isFile) tomlFile else null
    }

    /**
     * 在指定路径中查找 cjpm.toml 文件
     *
     * @param path 要搜索的路径
     * @return 找到的 cjpm.toml 文件，如果不存在则返回 null
     */
    fun findCjpmToml(path: Path): File? {
        return findCjpmToml(path.toFile())
    }

    /**
     * 创建一个新的 cjpm.toml 文件，包含基本的包配置
     *
     * @param directory 创建文件的目录
     * @param packageName 包名
     * @param version 包版本
     * @param cjcVersion CJC 版本
     * @param outputType 输出类型
     * @return 创建的配置文件
     */
    fun createBasicCjpmToml(
        directory: File,
        packageName: String,
        version: String = "0.0.1",
        cjcVersion: String = "0.55.3",
        outputType: OutputType = OutputType.DYNAMIC
    ): File {
        val config = CjpmTomlConfig(
            `package` = PackageConfig(
                name = packageName,
                version = version,
                cjcVersion = cjcVersion,
                outputType = outputType,
                description = "",
                srcDir = "src",
                targetDir = ""
            )
        )
        
        val file = directory.resolve(DEFAULT_FILENAME)
        CjpmTomlParser.writeToFile(config, file)
        return file
    }

    /**
     * 验证 TOML 配置文件的基本有效性
     *
     * @param config TOML 配置对象
     * @return 验证结果，包含可能的错误信息
     */
    fun validate(config: CjpmTomlConfig): ValidationResult {
        val errors = mutableListOf<String>()

        // 检查必需的包配置
        if (config.`package` == null) {
            errors.add("Missing [package] section")
        } else {
            with(config.`package`) {
                if (name.isBlank()) errors.add("Package name is required")
                if (version.isBlank()) errors.add("Package version is required")
                if (cjcVersion.isBlank()) errors.add("CJC version is required")
            }
        }

        // 检查工作空间配置
        if (config.workspace != null && config.`package` != null) {
            errors.add("Cannot have both [workspace] and [package] sections")
        }

        // 检查依赖配置
        config.dependencies.forEach { (name, dep) ->
            if (dep.path == null && dep.git == null) {
                errors.add("Dependency '$name' must specify either 'path' or 'git'")
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * 配置验证结果
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>
    )
} 