package cn.cangnova.cangjie.download.stdlib

import cn.cangnova.cangjie.CangJieBundle
import cn.cangnova.cangjie.cjpm.project.model.CjcInfo
import cn.cangnova.cangjie.cjpm.project.model.impl.CjpmProjectImpl
import cn.cangnova.cangjie.cjpm.project.model.impl.CjpmSyncTask
import cn.cangnova.cangjie.cjpm.project.model.impl.DownloadResult
import cn.cangnova.cangjie.cjpm.project.model.impl.TaskResult
import cn.cangnova.cangjie.cjpm.project.model.impl.workingDirectory
import cn.cangnova.cangjie.cjpm.project.workspace.StandardLibrary
import cn.cangnova.cangjie.cjpm.toolchain.CjToolchainBase
import cn.cangnova.cangjie.cjpm.toolchain.cjc
import cn.cangnova.cangjie.configurable.services.CangJieLanguageServerServices
import kotlinx.html.currentTimeMillis
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.exists
import com.google.gson.JsonParser

fun downloadStdlib(version: String): DownloadResult<File> {
    return try {
        // Fetch and parse index file
        val indexJson = URL(STDLIB_INDEX_URL).readText()
        val downloadUrl = JsonParser.parseString(indexJson)
            .asJsonObject
            .get(version)
            ?.asJsonObject
            ?.get("url")
            ?.asString
            ?: return DownloadResult.Err(CangJieBundle.message("error.message.stdlib.version.not.found", version))

        val tempFile = File.createTempFile("cangjie-stdlib${currentTimeMillis()}-$version", ".zip")

        // Download stdlib using URL from index
        URL(downloadUrl).openStream().use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        DownloadResult.Ok(tempFile)
    } catch (e: Exception) {
        DownloadResult.Err(CangJieBundle.message("error.message.download.stdlib", e.message ?: ""))
    }
}


fun fetchStdlib(
    context: CjpmSyncTask.SyncContext,
    cjpmProject: CjpmProjectImpl,
    rustcInfo: CjcInfo?
): TaskResult<StandardLibrary>? {
    return if (CangJieLanguageServerServices.getInstance().astConfig.enabled) {
        context.runWithChildProgress(CangJieBundle.message("progress.text.getting.cangjie.stdlib")) { childContext ->

            val workingDirectory = cjpmProject.workingDirectory
            val toolchain = childContext.toolchain
            val version = toolchain.cjc().version ?.semver ?.rawVersion ?: return@runWithChildProgress    TaskResult.Err("get sdk version error")
            val stdlibPath = STDLIB_PATH_LOCAL.resolve(version)

            val stdlibPathIndex = STDLIB_PATH_LOCAL.resolve("$version-intellij_cangjie_stdlib")

            // 验证
            if (!stdlibPathIndex.exists()) {
                stdlibPath.toFile().mkdirs()
                // 下载标准库到 stdlibPath
                return@runWithChildProgress when (val downloadResult =
                    downloadStdlib(toolchain.cjc().version !!.semver.rawVersion)) {
                    is DownloadResult.Ok -> {
                        val downloadedFile = downloadResult.value
                        try {
                            unzip(downloadedFile, stdlibPath.toFile())
                            stdlibPathIndex.toFile().apply {
                                createNewFile()
//                                写入
                                writeText(toolchain.cjc().version!!.semver.rawVersion)
                            }
                            TaskResult.Ok(StandardLibrary.fromFileStdlib(stdlibPath, version))
                        } catch (e: IllegalArgumentException) {
                            TaskResult.Err(e.toString())
                        } finally {
                            downloadedFile.delete()
                        }
                    }

                    is DownloadResult.Err -> {
                        TaskResult.Err(downloadResult.error)
                    }
                }
            }
            try {
                TaskResult.Ok(StandardLibrary.fromFileStdlib(stdlibPath, version))
            } catch (e: IllegalArgumentException) {
                TaskResult.Err(e.toString())
            }

        }

    } else {
        null
    }
}

// 解压缩文件的辅助方法
private fun unzip(zipFile: File, destDir: File) {
    ZipInputStream(FileInputStream(zipFile)).use { zip ->
        var entry: ZipEntry?
        while (zip.nextEntry.also { entry = it } != null) {
            val newFile = File(destDir, entry!!.name)
            if (entry.isDirectory) {
                newFile.mkdirs()
            } else {
                newFile.parentFile.mkdirs()
                FileOutputStream(newFile).use { output ->
                    zip.copyTo(output)
                }
            }
            zip.closeEntry()
        }
    }


}