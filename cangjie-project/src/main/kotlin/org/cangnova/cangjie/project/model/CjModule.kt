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
 * 模块抽象
 *
 * 代表项目中的一个模块/包
 */
interface CjModule {
    /**
     * 模块名称
     */
    val name: String

    /**
     * 模块根目录
     */
    val rootDir: VirtualFile

    /**
     * 所属项目
     */
    val project: CjProject

    /**
     * 模块配置文件
     */
    val configFile: VirtualFile?

    /**
     * 构建目标列表
     */
    val targets: List<CjTarget>

    /**
     * 源码集列表
     */
    val sourceSets: List<CjSourceSet>
}