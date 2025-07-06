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

package cn.cangnova.cangjie.toolchain.api

import java.nio.file.Path

/**
 * CangJie包管理器工具接口
 */
interface CjPackageManager : CjTool {
    /**
     * 安装依赖包
     *
     * @param packageName 包名称
     * @param version 版本要求
     * @param options 安装选项
     * @return 安装结果
     */
    fun install(packageName: String, version: String?, options: CjPackageOptions): CjPackageResult

    /**
     * 从项目配置文件安装所有依赖
     *
     * @param projectPath 项目路径
     * @param options 安装选项
     * @return 安装结果
     */
    fun installDependencies(projectPath: Path, options: CjPackageOptions): CjPackageResult

    /**
     * 更新依赖包
     *
     * @param packageName 包名称
     * @param options 更新选项
     * @return 更新结果
     */
    fun update(packageName: String?, options: CjPackageOptions): CjPackageResult

    /**
     * 移除依赖包
     *
     * @param packageName 包名称
     * @param options 移除选项
     * @return 移除结果
     */
    fun uninstall(packageName: String, options: CjPackageOptions): CjPackageResult

    companion object {
        /**
         * 包管理器名称
         */
        const val NAME = "packageManager"
    }
}
