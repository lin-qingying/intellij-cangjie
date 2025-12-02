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

package org.cangnova.cangjie.debugger.toolchain

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.HttpRequests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.cangnova.cangjie.debugger.DebuggerDownloadException
import org.cangnova.cangjie.debugger.DebuggerProvider
import org.cangnova.cangjie.debugger.messages.DebuggerBundle
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import kotlin.io.path.absolutePathString

/**
 * 仓颉调试器工具链服务
 *
 * 负责管理调试器的下载、安装和版本检查
 */
@Service(Service.Level.APP)
class CjDebuggerToolchainService {

    companion object {
        private val LOG = Logger.getInstance(CjDebuggerToolchainService::class.java)

        fun getInstance(): CjDebuggerToolchainService = service()
    }

    /**
     * 检查调试器可用性
     *
     * @param debuggerProvider 调试器提供者
     * @return 调试器可用性状态
     */
    suspend fun debuggerAvailability(debuggerProvider: DebuggerProvider): DebuggerAvailability {
        return withContext(Dispatchers.IO) {
            val isAvailable = debuggerProvider.isAvailable()

            when {
                isAvailable -> DebuggerAvailability.Available
                else -> DebuggerAvailability.NeedToDownload
            }
        }
    }

    /**
     * 下载调试器
     *
     * @param project 当前项目（可选）
     * @param debuggerProvider 调试器提供者
     * @return 下载结果
     */
    fun downloadDebugger(
        project: Project ,
        debuggerProvider: DebuggerProvider
    ): DownloadResult {
        // 使用 runProcessWithProgressSynchronously 在后台线程中执行下载
        val result = ProgressManager.getInstance().runProcessWithProgressSynchronously(
            ThrowableComputable<DownloadResult, Nothing> {
                downloadDebuggerSynchronously(debuggerProvider)
            },
            DebuggerBundle.message("debugger.downloading", debuggerProvider.name),
            true, // 可取消
            project
        )

        return result
    }

    /**
     * 同步下载调试器（在后台线程中执行）
     */
    private fun downloadDebuggerSynchronously(debuggerProvider: DebuggerProvider): DownloadResult {
        try {
            // 获取下载信息
            val downloadInfo = getDownloadInfo(debuggerProvider)

            // 获取进度指示器
            val indicator = ProgressManager.getInstance().progressIndicator

            // 下载并提取文件
            val targetPath = downloadAndExtract(downloadInfo, indicator)

            // 调用 ensureReady 设置权限等
            val ensureResult = runBlocking {
                debuggerProvider.ensureReady()
            }

            return if (ensureResult.isSuccess) {
                DownloadResult.Ok(targetPath)
            } else {
                DownloadResult.Failed(
                    ensureResult.exceptionOrNull()
                        ?: DebuggerDownloadException("Failed to prepare debugger")
                )
            }
        } catch (e: Exception) {
            LOG.error("Failed to download debugger", e)
            return DownloadResult.Failed(e)
        }
    }

    /**
     * 获取下载信息（从 Provider 获取）
     */
    private fun getDownloadInfo(debuggerProvider: DebuggerProvider): DebuggerDownloadInfo {
        return debuggerProvider.getDownloadInfo()
    }

    /**
     * 下载文件（支持可选解压）
     */
    private fun downloadAndExtract(
        downloadInfo: DebuggerDownloadInfo,
        indicator: ProgressIndicator
    ): Path {
        indicator.text = DebuggerBundle.message("debugger.downloading.file")
        indicator.isIndeterminate = false

        // 确定临时文件扩展名
        val tempExt = if (downloadInfo.needExtract) ".zip" else ".tmp"
        val tempFile = FileUtil.createTempFile("cj-debugger", tempExt, true)

        try {
            // 下载文件
            HttpRequests.request(downloadInfo.downloadUrl)
                .productNameAsUserAgent()
                .connect { request ->
                    request.saveToFile(tempFile, indicator)
                }

            // 验证校验和
            if (downloadInfo.checksum != null && downloadInfo.checksumType != null) {
                indicator.text = DebuggerBundle.message("debugger.verifying")
                indicator.isIndeterminate = true

                val actualChecksum = calculateChecksum(tempFile.toPath(), downloadInfo.checksumType)
                if (!actualChecksum.equals(downloadInfo.checksum, ignoreCase = true)) {
                    throw DebuggerDownloadException(
                        "Checksum verification failed. Expected: ${downloadInfo.checksum}, Actual: $actualChecksum"
                    )
                }
                LOG.info("Checksum verification passed: $actualChecksum")
            }

            val targetPath = downloadInfo.targetPath

            // 根据是否需要解压执行不同操作
            if (downloadInfo.needExtract) {
                // 解压到目标目录
                indicator.text = DebuggerBundle.message("debugger.extracting")
                indicator.isIndeterminate = true

                Files.createDirectories(targetPath)
                extractZip(tempFile.toPath(), targetPath)

                LOG.info("Debugger downloaded and extracted to: ${targetPath.absolutePathString()}")
            } else {
                // 直接保存到目标文件
                Files.createDirectories(targetPath.parent)
                Files.copy(tempFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING)

                LOG.info("Debugger downloaded to: ${targetPath.absolutePathString()}")
            }

            return targetPath
        } finally {
            tempFile.delete()
        }
    }

    /**
     * 计算文件校验和
     *
     * @param file 文件路径
     * @param algorithm 算法名称（如 "SHA1", "SHA-256", "MD5"）
     * @return 校验和的十六进制字符串
     */
    private fun calculateChecksum(file: Path, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        Files.newInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * 解压 ZIP 文件
     */
    private fun extractZip(zipFile: Path, targetDir: Path) {
        ZipInputStream(Files.newInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val targetFile = targetDir.resolve(entry.name)

                if (entry.isDirectory) {
                    Files.createDirectories(targetFile)
                } else {
                    Files.createDirectories(targetFile.parent)
                    Files.copy(zis, targetFile, StandardCopyOption.REPLACE_EXISTING)
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    /**
     * 下载结果
     */
    sealed class DownloadResult {
        /**
         * 下载成功
         */
        data class Ok(val path: Path) : DownloadResult()

        /**
         * 下载失败
         */
        data class Failed(val error: Throwable) : DownloadResult()

        /**
         * 用户取消
         */
        object Cancelled : DownloadResult()
    }
}

/**
 * 调试器可用性状态
 */
sealed class DebuggerAvailability {
    /**
     * 不可用（无法恢复）
     */
    object Unavailable : DebuggerAvailability()

    /**
     * 需要下载
     */
    object NeedToDownload : DebuggerAvailability()

    /**
     * 需要更新
     */
    object NeedToUpdate : DebuggerAvailability()

    /**
     * 已可用
     */
    object Available : DebuggerAvailability()
}

/**
 * 调试器下载信息
 */
data class DebuggerDownloadInfo(
    /**
     * 下载 URL（SourceForge 固定链接格式）
     * 格式: https://downloads.sourceforge.net/project/intellij-cangjie-debugger/[文件路径]
     * 例如: https://downloads.sourceforge.net/project/intellij-cangjie-debugger/dap/v1.0/cjdb-dap-server.exe
     */
    val downloadUrl: String,

    /**
     * 目标文件/目录路径
     * - 如果 needExtract=false: 下载后直接保存到此路径（文件）
     * - 如果 needExtract=true: 解压到此目录
     */
    val targetPath: Path,

    /**
     * 是否需要解压（通常不需要）
     * - true: 下载的是压缩文件，需要解压
     * - false: 直接下载可执行文件
     */
    val needExtract: Boolean = false,



    /**
     * 文件校验和（可选）
     */
    val checksum: String? = null,

    /**
     * 校验和类型（如 "SHA256", "MD5"）
     */
    val checksumType: String? = null
)