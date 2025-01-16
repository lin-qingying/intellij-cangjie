package com.linqingying.cangjie.download.stdlib

import com.linqingying.cangjie.cjpm.project.model.impl.DownloadResult
import com.linqingying.cangjie.cjpm.toolchain.CjToolchainBase
import java.io.File
import java.io.FileOutputStream
import java.net.URL

fun downloadStdlib(/*owner: Disposable? = null, listener: ProcessListener? = null*/version:String): DownloadResult<File> {
    // 假设下载链接为 CjToolchainBase.STDLIB_DOWNLOAD_URL
    val downloadUrl = CjToolchainBase.getStdlibDowloadUrl(version)
    val targetFile = CjToolchainBase.stdlibPath.resolve("downloaded_stdlib.zip").toFile()

    return try {
        // 使用 URL 下载文件
        URL(downloadUrl).openStream().use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        DownloadResult.Ok(targetFile) // 返回下载的文件
    } catch (e: Exception) {
        DownloadResult.Err("下载标准库失败: ${e.message}")
    }
}