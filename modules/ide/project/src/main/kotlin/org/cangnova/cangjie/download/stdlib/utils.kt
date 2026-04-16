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

package org.cangnova.cangjie.download.stdlib


import com.google.gson.JsonParser
import com.intellij.openapi.util.NlsContexts
import org.cangnova.cangjie.messages.CangJieBundle
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.System.currentTimeMillis
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

sealed class DownloadResult<out T> {
    class Ok<T>(val value: T) : DownloadResult<T>()
    class Err(@NlsContexts.NotificationContent val error: String) : DownloadResult<Nothing>()
}

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