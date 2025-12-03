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

package org.cangnova.cangjie.cjpm.config.lock

import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.cjpm.project.model.toml.TOML_MAPPER
import java.io.InputStream
import java.nio.file.Path

/**
 * cjpm.lock 文件解析器
 */
object CjpmLockParser {
    private val LOG: Logger = logger<CjpmLockParser>()


    /**
     * 解析 cjpm.lock 文件
     *
     * @param lockFile lock 文件的 VirtualFile
     * @return 解析后的 CjpmLockFile 对象，解析失败返回 null
     */
    fun parse(lockFile: VirtualFile): CjpmLockFile? {
        return try {
            val content = String(lockFile.contentsToByteArray(), Charsets.UTF_8)
            parse(content)
        } catch (e: Exception) {
            LOG.error("Failed to parse lock file: ${lockFile.path}", e)
            null
        }
    }

    /**
     * 从字符串解析 lock 文件
     */
    fun parse(content: String): CjpmLockFile? {
        return try {
            val rawData = TOML_MAPPER.readValue<Map<String, Any>>(content)
            parseLockData(rawData)
        } catch (e: Exception) {
            LOG.error("Failed to parse lock file content", e)
            null
        }
    }

    /**
     * 从路径解析 lock 文件
     */
    fun parse(path: Path): CjpmLockFile? {
        return try {
            val rawData = TOML_MAPPER.readValue<Map<String, Any>>(path.toFile())
            parseLockData(rawData)
        } catch (e: Exception) {
            LOG.error("Failed to parse lock file: $path", e)
            null
        }
    }

    /**
     * 从输入流解析 lock 文件
     */
    fun parse(input: InputStream): CjpmLockFile? {
        return try {
            val rawData = TOML_MAPPER.readValue<Map<String, Any>>(input)
            parseLockData(rawData)
        } catch (e: Exception) {
            LOG.error("Failed to parse lock file from input stream", e)
            null
        }
    }

    /**
     * 解析原始数据为 CjpmLockFile
     */
    private fun parseLockData(rawData: Map<String, Any>): CjpmLockFile {
        val version = (rawData["version"] as? Number)?.toInt() ?: 0
        val requiresMap = rawData["requires"] as? Map<*, *> ?: emptyMap<String, Any>()

        val requires = mutableMapOf<String, LockedDependency>()

        for ((name, value) in requiresMap) {
            if (name is String && value is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val depData = value as Map<String, Any>
                val lockedDep = parseLockedDependency(name, depData)
                if (lockedDep != null) {
                    requires[name] = lockedDep
                }
            }
        }

        return CjpmLockFile(version = version, requires = requires)
    }

    /**
     * 解析单个锁定依赖
     */
    private fun parseLockedDependency(name: String, data: Map<String, Any>): LockedDependency? {
        return try {
            when {
                // Git 依赖: 包含 git 和 commitId 字段
                data.containsKey("git") && data.containsKey("commitId") -> {
                    LockedDependency.Git(
                        name = name,
                        git = data["git"] as? String ?: "",
                        commitId = data["commitId"] as? String ?: "",
                        branch = data["branch"] as? String,
                        tag = data["tag"] as? String,
                        rev = data["rev"] as? String
                    )
                }

                // 注册表依赖: 包含 version 字段
                data.containsKey("version") -> {
                    LockedDependency.Registry(
                        name = name,
                        version = data["version"] as? String ?: "",
                        registry = data["registry"] as? String ?: "default",
                        checksum = data["checksum"] as? String
                    )
                }

                // 路径依赖: 包含 path 字段
                data.containsKey("path") -> {
                    LockedDependency.Path(
                        name = name,
                        path = data["path"] as? String ?: ""
                    )
                }

                else -> {
                    LOG.warn("Unknown dependency format for: $name")
                    null
                }
            }
        } catch (e: Exception) {
            LOG.error("Failed to parse locked dependency: $name", e)
            null
        }
    }
}