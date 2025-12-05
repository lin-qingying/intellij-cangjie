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

package org.cangnova.cangjie.extension

import com.intellij.openapi.extensions.ExtensionPointName
import org.cangnova.cangjie.project.extension.ProjectBuildSystemId
import org.cangnova.cangjie.project.model.CjPackageMetadata
import org.cangnova.cangjie.project.model.CjVersion
import java.nio.file.Path

/**
 * 仓库提供者扩展点
 *
 * 负责提供包仓库访问功能
 */
interface CjRepositoryProvider {
    companion object {
        val EP_NAME = ExtensionPointName<CjRepositoryProvider>(
            "org.cangnova.cangjie.dependency.repositoryProvider"
        )
    }

    /**
     * 获取此解析器关联的构建系统 ID
     *
     * @return 构建系统 ID
     */
    fun getBuildSystemId(): ProjectBuildSystemId

    /**
     * 仓库名称
     */
    val repositoryName: String

    /**
     * 仓库URL
     */
    val repositoryUrl: String


    /**
     * 搜索包
     *
     * @param name 包名称
     * @return 搜索到的包列表
     */
    fun searchPackage(name: String): List<CjPackageMetadata>

    /**
     * 获取包信息
     *
     * @param name 包名称
     * @param version 包版本
     * @return 包信息，如果不存在返回 null
     */
    fun getPackageInfo(name: String, version: CjVersion): CjPackageMetadata?

    /**
     * 下载包
     *
     * @param pkg 包信息
     * @param targetDir 目标目录
     * @return 下载是否成功
     */
    fun downloadPackage(pkg: CjPackageMetadata, targetDir: Path): Boolean

    /**
     * 获取包的所有可用版本
     *
     * @param name 包名称
     * @return 可用版本列表
     */
    fun getAvailableVersions(name: String): List<CjVersion>
}