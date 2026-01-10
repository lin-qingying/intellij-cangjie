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

package org.cangnova.cangjie.utils

import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths


/**
 * 将字符串转换为 [Path] 对象。
 * 使用 [Paths.get] 方法进行转换。
 *
 * @return 转换后的 Path 对象
 * @throws InvalidPathException 如果字符串不是有效的路径
 *
 * 示例：
 * ```kotlin
 * val path = "/usr/local/bin".toPath()
 * val winPath = "C:\\Program Files".toPath()
 * ```
 */
fun String.toPath(): Path = Paths.get(this)

/**
 * 将字符串安全地转换为 [Path] 对象。
 * 如果转换失败（例如路径格式不正确），则返回 null 而不是抛出异常。
 *
 * @return 转换后的 Path 对象，如果转换失败则返回 null
 *
 * 示例：
 * ```kotlin
 * val validPath = "/usr/local".toPathOrNull()  // 返回 Path 对象
 * val invalidPath = "\0invalid".toPathOrNull() // 返回 null
 * ```
 */
fun String.toPathOrNull(): Path? = pathOrNull(this::toPath)

/**
 * 安全地执行路径转换操作。
 * 捕获 [InvalidPathException] 异常并返回 null。
 *
 * @param block 执行路径转换的函数
 * @return 转换后的 Path 对象，如果转换失败则返回 null
 */
private inline fun pathOrNull(block: () -> Path): Path? {
    return try {
        block()
    } catch (e: InvalidPathException) {

        null
    }
}

/**
 * 获取 [VirtualFile] 对象的文件系统路径，并转换为 [Path] 对象。
 * 使用 [VirtualFile.path] 获取路径字符串，然后转换为 Path。
 *
 * @return VirtualFile 的文件系统路径对应的 Path 对象
 *
 * 示例：
 * ```kotlin
 * val virtualFile: VirtualFile = ...
 * val path = virtualFile.pathAsPath
 * ```
 */
val VirtualFile.pathAsPath: Path get() = Paths.get(path)