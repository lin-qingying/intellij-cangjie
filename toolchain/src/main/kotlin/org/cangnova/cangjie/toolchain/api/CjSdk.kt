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

import org.cangnova.cangjie.toolchain.CangJieSdkVersion
import org.cangnova.cangjie.toolchain.env.CangJieEnv
import java.nio.file.Path

/**
 * 仓颉 SDK 信息
 *
 * 表示一个已注册的仓颉 SDK 实例,包含 SDK 的路径和版本信息
 */
data class CjSdk(
    /**
     * SDK 的唯一标识符
     * 通常使用 "CangJie-{version}" 格式
     */
    val id: String,

    /**
     * SDK 的显示名称
     */
    val name: String,

    /**
     * SDK 的根目录路径
     */
    val homePath: Path,

    /**
     * SDK 版本信息
     * 如果无法解析版本则为 null
     */
    val version: CangJieSdkVersion?,

    /**
     * SDK 是否有效
     * 检查必要的可执行文件是否存在
     */
    val isValid: Boolean
) {
    /**
     * 获取 SDK 的工具目录路径
     */
    val binPath: Path
        get() = homePath.resolve("bin")

    /**
     * 获取标准库路径
     */
    val stdlibPath: Path
        get() = homePath.resolve("modules")

    override fun toString(): String = "$name ($version) at $homePath"


    fun getEnvironment(): Map<String, String> = CangJieEnv.getInstance(homePath).getEnvVars()

}
