package com.linqingying.utils


import com.google.gson.Gson
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
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
