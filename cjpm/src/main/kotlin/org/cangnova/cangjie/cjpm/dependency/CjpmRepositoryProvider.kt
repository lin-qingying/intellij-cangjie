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

package org.cangnova.cangjie.cjpm.dependency

import org.cangnova.cangjie.cjpm.project.CjpmBuildSystemId
import org.cangnova.cangjie.dependency.extension.CjRepositoryProvider
import org.cangnova.cangjie.dependency.model.CjPackage
import org.cangnova.cangjie.dependency.model.CjVersion
import org.cangnova.cangjie.project.extension.ProjectBuildSystemId
import java.nio.file.Path

/**
 * CJPM 仓库提供者实现
 */
class CjpmRepositoryProvider : CjRepositoryProvider {
    override fun getBuildSystemId(): ProjectBuildSystemId = CjpmBuildSystemId

    override val repositoryName: String = "CJPM Official Repository"

    override val repositoryUrl: String = "https://repo.cangjie.org"


    override fun searchPackage(name: String): List<CjPackage> {
        // TODO: 实现包搜索逻辑
        // 1. 连接到 CJPM 仓库
        // 2. 搜索包
        // 3. 返回搜索结果
        return emptyList()
    }

    override fun getPackageInfo(name: String, version: CjVersion): CjPackage? {
        // TODO: 获取包信息
        return null
    }

    override fun downloadPackage(pkg: CjPackage, targetDir: Path): Boolean {
        // TODO: 下载包
        // 1. 从仓库下载包
        // 2. 保存到目标目录
        // 3. 验证完整性
        return false
    }

    override fun getAvailableVersions(name: String): List<CjVersion> {
        // TODO: 获取包的所有可用版本
        return emptyList()
    }
}