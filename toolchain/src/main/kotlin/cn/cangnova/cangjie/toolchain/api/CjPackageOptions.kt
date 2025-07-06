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
/**
 * CangJie包管理选项
 */
interface CjPackageOptions {
    /**
     * 安装范围
     */
    val scope: InstallScope

    /**
     * 依赖类型
     */
    val dependencyType: DependencyType

    /**
     * 是否保存到项目配置
     */
    val saveMode: SaveMode

    /**
     * 额外的包管理器参数
     */
    val extraArgs: List<String>

    /**
     * 安装范围枚举
     */
    enum class InstallScope {
        /**
         * 全局安装
         */
        GLOBAL,

        /**
         * 项目安装
         */
        PROJECT
    }

    /**
     * 依赖类型枚举
     */
    enum class DependencyType {
        /**
         * 生产依赖
         */
        PRODUCTION,

        /**
         * 开发依赖
         */
        DEVELOPMENT,

        /**
         * 可选依赖
         */
        OPTIONAL,

        /**
         * 同级依赖
         */
        PEER
    }

    /**
     * 保存模式枚举
     */
    enum class SaveMode {
        /**
         * 不保存
         */
        NONE,

        /**
         * 保存精确版本
         */
        EXACT,

        /**
         * 保存兼容版本
         */
        COMPATIBLE
    }
}
