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

package org.cangnova.cangjie.toolchain.api
/**
 * CangJie包信息
 */
interface CjPackageInfo {
    /**
     * 包名称
     */
    val name: String

    /**
     * 包版本
     */
    val version: String

    /**
     * 依赖类型
     */
    val dependencyType: CjPackageOptions.DependencyType

    /**
     * 包状态
     */
    val status: PackageStatus

    /**
     * 包状态枚举
     */
    enum class PackageStatus {
        /**
         * 已安装
         */
        INSTALLED,

        /**
         * 已更新
         */
        UPDATED,

        /**
         * 已移除
         */
        REMOVED,

        /**
         * 未变化
         */
        UNCHANGED,

        /**
         * 已过时
         */
        OUTDATED
    }
}
