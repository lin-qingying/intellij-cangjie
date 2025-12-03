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

package org.cangnova.cangjie.toolchain

import com.intellij.util.text.SemVer

/**
 * 仓颉 SDK 版本信息
 *
 * @property semver 语义化版本号
 * @property target 目标平台（如 x86_64-w64-mingw32）
 * @property type SDK 类型（如 cjnative），可选
 */
data class CangJieSdkVersion(
    val semver: SemVer,
    val target: String,
    val type: String? = null
) {
    override fun toString(): String {
        return semver.parsedVersion
    }
}