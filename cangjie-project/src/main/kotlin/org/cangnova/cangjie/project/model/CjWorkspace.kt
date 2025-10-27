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

package org.cangnova.cangjie.project.model

import com.intellij.openapi.vfs.VirtualFile

/**
 * 工作空间抽象
 *
 * 代表一个包含多个模块的工作空间
 */
interface CjWorkspace {
    /**
     * 工作空间名称
     */
    val name: String

    /**
     * 工作空间根目录
     */
    val rootDir: VirtualFile

    
    /**
     * 工作空间中的所有模块
     */
    val modules: List<CjModule>

    /**
     * 查找指定名称的模块
     */
    fun findModule(name: String): CjModule?
}