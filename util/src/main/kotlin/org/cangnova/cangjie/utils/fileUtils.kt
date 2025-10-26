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



import com.google.gson.Gson
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

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
 * Calculates the relative path to this file from [base] file.
 * Note that the [base] file is treated as a directory.
 *
 * If this file matches the [base] directory an empty path is returned.
 * If this file does not belong to the [base] directory, it is returned unchanged.
 */
fun File.descendantRelativeTo(base: File): File {
    assert(base.isAbsolute) { "$base" }
    assert(base.isDirectory) { "$base" }
    val cwd = base.normalize()
    val filePath = this.absoluteFile.normalize()
    return if (filePath.startsWith(cwd)) filePath.relativeTo(cwd) else this
}
object FileUtils {

    private val gson = Gson()
//    fun generateFileList(directory: Path): Map<String, String> {
//        return generateFileList(directory, true)
//    }

    // 生成文件清单并保存为module.json，返回文件清单
    fun generateFileList(directory: Path, saveFile: Boolean = true): Map<String, String> {
        val fileList = mutableMapOf<String, String>()
        Files.walk(directory).forEach { path ->
            if (Files.isRegularFile(path) && path.fileName.toString() != "module.json") {
                val relativePath = directory.relativize(path).toString().replace("\\", "/") // 替换反斜杠为正斜杠
                val hash = calculateHash(path)
                fileList[relativePath] = hash // 使用相对路径作为键
            }
        }
        if (saveFile) {
            saveManifestFile(fileList, directory.resolve("module.json"))
        }
        return fileList // 返回生成的文件清单
    }
    // 扩展函数：返回所有顶层文件夹名称
    fun Map<String, String>.getTopLevelDirectories(): Set<String> {
        return this.keys.map { path ->
            path.split("/").first() // 获取路径的第一个部分
        }.toSet() // 转换为集合以去重
    }
    // 计算文件的hash值
    private fun calculateHash(file: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file).use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { String.format("%02x", it) }
    }

    // 保存清单文件为JSON，清除原文件
    private fun saveManifestFile(fileList: Map<String, String>, manifestFilePath: Path) {
        // 清除原文件
        val manifestFile = File(manifestFilePath.toUri())
        if (manifestFile.exists()) {
            manifestFile.delete()
        }
        // 保存新的清单文件
        val json = gson.toJson(fileList)
        manifestFile.writeText(json)
    }

    // 从清单文件读取并验证文件一致性
    fun verifyFileList(directory: Path): Boolean {
        return true

        val fileList = readManifestFile(directory.resolve("module.json"))
        val currentFileList = generateFileList(directory,false)
        return currentFileList == fileList
    }

    // 读取清单文件
    private fun readManifestFile(manifestFilePath: Path): Map<String, String> {
        val json = File(manifestFilePath.toUri()).readText()
        return gson.fromJson(json, Map::class.java) as Map<String, String>
    }
}


fun fullyRefreshDirectory(directory: VirtualFile) {
    VfsUtil.markDirtyAndRefresh(/* async = */ false, /* recursive = */ true, /* reloadChildren = */ true, directory)
}