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
 * CangJie包管理结果
 */
interface CjPackageResult {
    /**
     * 操作是否成功
     */
    val success: Boolean

    /**
     * 受影响的包列表
     */
    val packages: List<CjPackageInfo>

    /**
     * 包管理器输出信息
     */
    val output: String

    /**
     * 操作类型
     */
    val operationType: OperationType

    /**
     * 操作状态
     */
    val status: OperationStatus

    /**
     * 操作类型枚举
     */
    enum class OperationType {
        /**
         * 初始化项目
         */
        INIT,
        
        /**
         * 构建项目
         */
        BUILD,
        
        /**
         * 运行项目
         */
        RUN,
        
        /**
         * 测试项目
         */
        TEST,
        
        /**
         * 清理项目
         */
        CLEAN,

        /**
         * 安装
         */
        INSTALL,

        /**
         * 更新
         */
        UPDATE,

        /**
         * 卸载
         */
        UNINSTALL,

        /**
         * 列表
         */
        LIST
    }

    /**
     * 操作状态枚举
     */
    enum class OperationStatus {
        /**
         * 成功
         */
        SUCCESS,

        /**
         * 部分成功
         */
        PARTIAL_SUCCESS,

        /**
         * 失败
         */
        FAILURE,

        /**
         * 无变化
         */
        NO_CHANGE
    }
}
