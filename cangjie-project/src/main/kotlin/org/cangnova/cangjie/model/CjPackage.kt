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

package org.cangnova.cangjie.model

import java.nio.file.Path

/**
 * 包抽象接口
 */
interface CjPackage {
    /**
     * 包名称
     */
    val name: String

    /**
     * 包组 (可选)
     */
    val group: String?

    /**
     * 包版本
     */
    val version: CjVersion

    /**
     * 包描述
     */
    val description: String?

    /**
     * 包作者
     */
    val authors: List<String>

    /**
     * 包许可证
     */
    val license: String?

    /**
     * 包仓库URL
     */
    val repositoryUrl: String?

    /**
     * 包依赖列表
     */
    val dependencies: List<CjDependency>

    /**
     * 包的本地路径 (如果已下载)
     */
    val localPath: Path?
}