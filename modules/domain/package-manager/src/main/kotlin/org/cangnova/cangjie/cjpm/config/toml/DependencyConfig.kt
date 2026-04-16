/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.cjpm.project.model.toml

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable

/**
 * 依赖配置类，支持本地路径依赖和远程 git 依赖
 */
@Serializable
data class DependencyConfig(
    /** 本地依赖路径 */
    val path: String? = null,
    
    /** git 仓库地址，必须包含 git 支持的任何格式的有效 url */
    val git: String? = null,
    
    /** git 分支名 */
    val branch: String? = null,
    
    /** git 标签名 */
    val tag: String? = null,

    /** git commit ID / revision */
    val rev: String? = null,
    
    /** 依赖版本号，用于检查依赖项是否具有正确的版本 */
    val version: String? = null,
    
    /** 指定编译产物类型，可以与源码依赖自身的编译产物类型不一致 */
    @field:JsonProperty("output-type")
    val outputType: OutputType? = null
)