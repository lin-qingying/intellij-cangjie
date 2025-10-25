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

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.dependency.extension.CjPackageManager
import org.cangnova.cangjie.dependency.model.CjPackage
import org.cangnova.cangjie.dependency.model.CjVersion
import java.nio.file.Path

/**
 * CJPM 包管理器实现
 */
class CjpmPackageManager : CjPackageManager {

    override val managerName: String = "CJPM Package Manager"

    override val priority: Int = 100

    override fun installPackage(pkg: CjPackage, project: Project): Boolean {
        // TODO: 实现包安装逻辑
        // 1. 下载包
        // 2. 解压到本地
        // 3. 注册到包注册表
        return false
    }

    override fun uninstallPackage(name: String, version: CjVersion, project: Project): Boolean {
        // TODO: 实现包卸载逻辑
        return false
    }

    override fun updatePackage(name: String, version: CjVersion, project: Project): Boolean {
        // TODO: 实现包更新逻辑
        return false
    }

    override fun getInstalledPackages(project: Project): List<CjPackage> {
        // TODO: 获取已安装的包列表
        return emptyList()
    }

    override fun getPackageInstallPath(pkg: CjPackage, project: Project): Path? {
        // TODO: 返回包的安装路径
        return null
    }
}