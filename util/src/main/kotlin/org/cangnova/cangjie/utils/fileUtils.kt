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

package org.cangnova.cangjie.utils


import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/**
 * 替换文件扩展名。
 * 如果当前文件以指定的旧扩展名结尾，则将其替换为新扩展名。
 *
 * @param oldExt 旧扩展名（包含点号，例如 ".kt"）
 * @param newExt 新扩展名（包含点号，例如 ".java"）
 * @return 如果成功替换则返回新的 File 对象，否则返回 null
 *
 * 示例：
 * ```kotlin
 * val file = File("Test.kt")
 * file.withReplacedExtensionOrNull(".kt", ".java")
 * // 返回 File("Test.java")
 *
 * val file2 = File("Test.java")
 * file2.withReplacedExtensionOrNull(".kt", ".java")
 * // 返回 null（因为文件不以 .kt 结尾）
 * ```
 */
fun File.withReplacedExtensionOrNull(oldExt: String, newExt: String): File? {
    if (name.endsWith(oldExt)) {
        val path = path
        val pathWithoutExt = path.substring(0, path.length - oldExt.length)
        val pathWithNewExt = pathWithoutExt + newExt
        return File(pathWithNewExt)
    }

    return null
}

/**
 * 计算当前文件相对于基准目录的相对路径。
 * 注意：基准文件会被视为目录。
 *
 * 行为说明：
 * - 如果当前文件与基准目录匹配，返回空路径
 * - 如果当前文件不属于基准目录，返回当前文件本身
 * - 如果当前文件属于基准目录，返回相对路径
 *
 * @param base 基准目录（必须是绝对路径且是目录）
 * @return 相对路径 File 对象，如果不在基准目录下则返回原文件
 *
 * @throws AssertionError 如果 base 不是绝对路径或不是目录
 *
 * 示例：
 * ```kotlin
 * val base = File("/home/user/project")
 * val file = File("/home/user/project/src/Main.kt")
 * file.descendantRelativeTo(base)
 * // 返回 File("src/Main.kt")
 *
 * val file2 = File("/other/path/file.kt")
 * file2.descendantRelativeTo(base)
 * // 返回 File("/other/path/file.kt")（原文件）
 * ```
 */
fun File.descendantRelativeTo(base: File): File {
    assert(base.isAbsolute) { "$base" }
    assert(base.isDirectory) { "$base" }
    val cwd = base.normalize()
    val filePath = this.absoluteFile.normalize()
    return if (filePath.startsWith(cwd)) filePath.relativeTo(cwd) else this
}

/**
 * 完全刷新虚拟文件目录。
 * 异步且递归地重新加载目录内容，包括子目录和文件。
 * 这会触发 VFS 重新扫描目录，确保所有更改都被检测到。
 *
 * @param directory 要刷新的虚拟文件目录
 *
 * 该方法执行以下操作：
 * - 异步刷新（不阻塞当前线程）
 * - 递归刷新所有子目录
 * - 重新加载子节点
 *
 * 示例：
 * ```kotlin
 * val directory: VirtualFile = ...
 * fullyRefreshDirectory(directory)
 * // 目录及其所有子内容将被重新扫描
 * ```
 */
fun fullyRefreshDirectory(directory: VirtualFile) {
    VfsUtil.markDirtyAndRefresh(/* async = */ true, /* recursive = */ true, /* reloadChildren = */ true, directory)
}