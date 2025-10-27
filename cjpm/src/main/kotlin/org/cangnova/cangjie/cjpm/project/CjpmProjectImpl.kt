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

package org.cangnova.cangjie.cjpm.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.cjpm.project.model.toml.CjpmTomlParser
import org.cangnova.cangjie.cjpm.config.CjpmConfigConverter
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.model.CjWorkspace
import org.cangnova.cangjie.project.model.roots

/**
 * 可重置的 Lazy 委托，支持缓存重置
 */
class ResettableLazy<T>(private val initializer: () -> T) {
    @Volatile
    private var _value: Any? = UNINITIALIZED_VALUE
    private val lock = Any()

    val value: T
        get() {
            val _v1 = _value
            if (_v1 !== UNINITIALIZED_VALUE) {
                @Suppress("UNCHECKED_CAST")
                return _v1 as T
            }

            return synchronized(lock) {
                val _v2 = _value
                if (_v2 !== UNINITIALIZED_VALUE) {
                    @Suppress("UNCHECKED_CAST")
                    _v2 as T
                } else {
                    val typedValue = initializer()
                    _value = typedValue
                    typedValue
                }
            }
        }

    fun reset() {
        synchronized(lock) {
            _value = UNINITIALIZED_VALUE
        }
    }

    private companion object {
        val UNINITIALIZED_VALUE = Any()
    }
}

/**
 * 创建可重置的 Lazy
 */
fun <T> resettableLazy(initializer: () -> T): ResettableLazy<T> = ResettableLazy(initializer)

/**
 * CJPM 项目实现
 */
class CjpmProjectImpl(
    override val rootDir: VirtualFile,
    override val intellijProject: Project,
    private val manifestFile: VirtualFile
) : CjProject {

    private val config = resettableLazy {
        val fullConfig = CjpmTomlParser.parse(manifestFile)
        fullConfig?.let { CjpmConfigConverter.convertToSimpleConfig(it) }
    }

    override val name: String
        get() = config.value?.`package`?.name ?: config.value?.workspace?.name ?: rootDir.name

    override val configFile: VirtualFile
        get() = manifestFile

    private val modulesCache = resettableLazy {
        buildModulesList()
    }

    override val modules: List<CjModule>
        get() = modulesCache.value

    private val workspaceCache = resettableLazy {
        buildWorkspace()
    }

    override val workspace: CjWorkspace?
        get() = workspaceCache.value

    override val version: String?
        get() = config.value?.`package`?.version

    override val isValid: Boolean
        get() = config.value != null

    private val indexableDirectoriesCache = resettableLazy {
        buildIndexableDirectories()
    }

    override val indexableDirectories: List<VirtualFile>
        get() = indexableDirectoriesCache.value

    override fun refresh() {
        // 清除配置缓存，强制重新加载
        config.reset()

        // 清除模块列表缓存，强制重新构建
        modulesCache.reset()

        // 清除工作空间缓存，强制重新构建
        workspaceCache.reset()

        // 清除索引目录缓存，强制重新计算
        indexableDirectoriesCache.reset()
    }

    override fun findModule(name: String): CjModule? {
        return modules.find { it.name == name }
    }

    private fun buildModulesList(): List<CjModule> {
        val config = this.config.value ?: return emptyList()
        val result = mutableListOf<CjModule>()

        // 如果是单模块项目
        config.`package`?.let { packageConfig ->
            result.add(
                CjpmModuleImpl(
                    name = packageConfig.name,
                    rootDir = rootDir,
                    project = this,
                    packageConfig = packageConfig
                )
            )
        }

        // 如果是工作空间项目
        config.workspace?.let { workspaceConfig ->
            workspaceConfig.members.forEach { memberPath ->
                val memberDir = rootDir.findFileByRelativePath(memberPath)
                val memberManifest = memberDir?.findChild("cjpm.toml")

                if (memberDir != null && memberManifest != null) {
                    val fullMemberConfig = CjpmTomlParser.parse(memberManifest)
                    val memberConfig = fullMemberConfig?.let { CjpmConfigConverter.convertToSimpleConfig(it) }
                    memberConfig?.`package`?.let { packageConfig ->
                        result.add(
                            CjpmModuleImpl(
                                name = packageConfig.name,
                                rootDir = memberDir,
                                project = this,
                                packageConfig = packageConfig
                            )
                        )
                    }
                }
            }
        }

        return result
    }

    private fun buildWorkspace(): CjWorkspace? {
        val config = this.config.value ?: return null

        return if (config.workspace != null) {
            CjpmWorkspaceImpl(
                name = config.workspace.name ?: name,
                rootDir = rootDir,
                modules = modules
            )
        } else {
            null
        }
    }

    private fun buildIndexableDirectories(): List<VirtualFile> {
        val directories = mutableListOf<VirtualFile>()

        // 添加项目根目录
        directories.add(rootDir)

        // 添加所有模块的源码目录和输出目录
        for (module in modules) {
            for (sourceSet in module.sourceSets) {
                directories.addAll(sourceSet.roots)
            }
            for (target in module.targets) {
                target.outputDirectory?.let { directories.add(it) }
            }
        }

        return directories
    }
}