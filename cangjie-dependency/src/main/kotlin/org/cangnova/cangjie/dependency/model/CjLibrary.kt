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

package org.cangnova.cangjie.dependency.model

import java.nio.file.Path

/**
 * 库抽象接口
 *
 * 代表一个已解析的库文件
 */
interface CjLibrary {
    /**
     * 库名称
     */
    val name: String

    /**
     * 库版本
     */
    val version: CjVersion

    /**
     * 库文件路径
     */
    val libraryPath: Path

    /**
     * 库类型 (静态库/动态库等)
     */
    val libraryType: CjLibraryType

    /**
     * 源码路径 (可选)
     */
    val sourcePath: Path?

    /**
     * 文档路径 (可选)
     */
    val documentationPath: Path?
}

/**
 * 库类型
 */
enum class CjLibraryType {
    /**
     * 静态库
     */
    STATIC,

    /**
     * 动态库
     */
    DYNAMIC,

    /**
     * 编译产物 (如 .cjo 文件)
     */
    COMPILED
}