/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.lsp.api

import com.intellij.openapi.project.BaseProjectDirectories
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe

/**
 * 一个 [LspServerDescriptor] 实现，假设单个 LSP 服务器将为整个项目提供服务，而不考虑项目结构。
 * 因此，它使用 [BaseProjectDirectories.getBaseDirectories] 返回的所有目录作为 LSP 服务器的根目录。
 *
 * @param project 当前项目实例
 * @param presentableName LSP 服务器的可展示名称
 */
abstract class ProjectWideLspServerDescriptor(
    project: Project,
    @NlsSafe presentableName: String
) : LspServerDescriptor(project, presentableName, *project.getBaseDirectories().toTypedArray())
