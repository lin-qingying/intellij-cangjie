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
import org.cangnova.cangjie.model.CjDependency
import org.cangnova.cangjie.project.extension.ProjectBuildSystemId
import java.nio.file.Path

/**
 * 包管理器扩展点
 *
 * 负责管理包的下载和安装
 */
interface CjPackageManager {
    companion object {
        val EP_NAME = ExtensionPointName<CjPackageManager>(
            "org.cangnova.cangjie.dependency.packageManager"
        )
    }

    /**
     * 获取此解析器关联的构建系统 ID
     *
     * @return 构建系统 ID
     */
    fun getBuildSystemId(): ProjectBuildSystemId

    /**
     * 包管理器名称
     */
    val name: String


    /**
     * 下载包
     *
     * @param dependency 依赖信息
     * @param targetDir 目标目录
     * @return 下载是否成功
     */
    fun downloadPackage(dependency: CjDependency, targetDir: Path): Boolean

    /**
     * 检查包是否已下载
     *
     * @param dependency 依赖信息
     * @return 如果已下载返回 true
     */
    fun isPackageDownloaded(dependency: CjDependency): Boolean

    /**
     * 获取包的本地路径
     *
     * @param dependency 依赖信息
     * @return 包的本地路径，如果不存在返回 null
     */
    fun getPackagePath(dependency: CjDependency): Path?

    /**
     * 清除缓存
     */
    fun clearCache()

    /**
     * 获取包的缓存目录
     */
    fun getCacheDirectory(): Path
}