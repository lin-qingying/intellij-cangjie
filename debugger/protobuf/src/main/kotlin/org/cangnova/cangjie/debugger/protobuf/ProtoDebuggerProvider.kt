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

package org.cangnova.cangjie.debugger.protobuf

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cangnova.cangjie.debugger.DebuggerNotFoundException
import org.cangnova.cangjie.debugger.DebuggerProvider
import org.cangnova.cangjie.debugger.toolchain.DebuggerDownloadInfo
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Protobuf 调试器提供者
 *
 * 使用 LLDB 前端和框架文件进行调试。
 * 这是传统的基于 protobuf 通信的调试器实现。
 */
object ProtoDebuggerProvider : DebuggerProvider {

    private val LOG = Logger.getInstance(ProtoDebuggerProvider::class.java)

    override val name: String = "Proto LLDB Debugger"
    override val debuggerType: String = "lldbproto"

    /**
     * 获取调试器可执行文件名
     */
    private val executableName: String
        get() = DebuggerExe.getFileName()

    /** 调试器目录 */
    private const val DEBUGGER_DIR = ".cangjie/debugger"


    /**
     * 获取调试器目录路径
     */
    val debuggerDirectory: Path
        get() = Paths.get(System.getProperty("user.home"), DEBUGGER_DIR)

    /**
     * 获取调试器可执行文件路径
     */
    val executablePath: Path
        get() = debuggerDirectory.resolve(executableName)


    override suspend fun getServerPath(): Path {
        return withContext(Dispatchers.IO) {
            val path = executablePath

            if (!Files.exists(path)) {
                throw DebuggerNotFoundException(
                    "LLDB Frontend not found at: $path. " +
                            "Please ensure the debugger is installed in ~/.cangjie/debugger/"
                )
            }

            if (!Files.isExecutable(path) && !SystemInfo.isWindows) {
                LOG.warn("Frontend file is not executable, attempting to set permissions")
                try {
                    path.toFile().setExecutable(true)
                } catch (e: Exception) {
                    LOG.error("Failed to set executable permission", e)
                }
            }

            LOG.info("Using LLDB frontend at: $path")
            path
        }
    }

    override suspend fun isAvailable(): Boolean {
        return withContext(Dispatchers.IO) {
            val path = executablePath
            Files.exists(path) && (SystemInfo.isWindows || Files.isExecutable(path))
        }
    }

    override suspend fun ensureReady(): Result<Path> {
        return withContext(Dispatchers.IO) {
            try {
                // 确保目录存在
                val dir = debuggerDirectory
                if (!Files.exists(dir)) {
                    Files.createDirectories(dir)
                    LOG.info("Created debugger directory: $dir")
                }

                // 检查前端是否存在
                val frontend = executablePath
                if (!Files.exists(frontend)) {
                    return@withContext Result.failure(
                        DebuggerNotFoundException(
                            "LLDB Frontend not found at: $frontend. " +
                                    "Please copy the frontend executable to this location."
                        )
                    )
                }


                // 设置可执行权限（非Windows）
                if (!SystemInfo.isWindows && !Files.isExecutable(frontend)) {
                    frontend.toFile().setExecutable(true)
                    LOG.info("Set executable permission for: $frontend")
                }

                LOG.info("Proto debugger ready - frontend: $frontend")
                Result.success(frontend)
            } catch (e: Exception) {
                LOG.error("Failed to ensure proto debugger ready", e)
                Result.failure(e)
            }
        }
    }

    override fun getDownloadInfo(): DebuggerDownloadInfo {
        // 根据平台选择对应的文件名和校验信息
        val (fileName, sha1) = when {
            SystemInfo.isWindows -> {
                Pair(
                    DebuggerExe.Windows.fileName,
                    "6a56664ae0c55b6720b174af514bf4a16517fc56",
                )
            }

            SystemInfo.isMac && SystemInfo.isAarch64 -> {
                Pair(
                    DebuggerExe.MacOS_aarch64.fileName,
                    "0b1536e692de85bccc8db195b7c8f3851ed0225a",
                )
            }

            SystemInfo.isMac -> {
                Pair(
                    DebuggerExe.MacOS_x64.fileName,
                    "b0b8b45611d9a3468580380cd7c66fc8ac4811ef",
                )
            }

            SystemInfo.isLinux && SystemInfo.isAarch64 -> {
                Pair(
                    DebuggerExe.Linux_aarch64.fileName,
                    "f099734893e58cc7e2dcb5d10ccdea1e086fff40",
                )
            }

            SystemInfo.isLinux -> {
                Pair(
                    DebuggerExe.Linux_x64.fileName,
                    "8165794c6b4f2f939157dc2fbdf031dfe6ab3cb0",
                )
            }

            else -> {
                throw UnsupportedOperationException(
                    "Unsupported platform: ${SystemInfo.OS_NAME} ${SystemInfo.OS_ARCH}"
                )
            }
        }


        // SourceForge 下载链接格式
        val downloadUrl = "https://downloads.sourceforge.net/project/intellij-cangjie-debugger/lldb-adapter/$fileName"

        return DebuggerDownloadInfo(
            downloadUrl = downloadUrl,
            targetPath = executablePath,
            needExtract = false, // 直接下载可执行文件，不需要解压

            checksum = sha1,
            checksumType = "SHA1"
        )
    }

}


private enum class DebuggerExe(val fileName: String) {
    Windows("CangJieLLDBAdapter_windows_amd64.exe"),
    MacOS_x64("CangJieLLDBAdapter_macos_amd64"),
    MacOS_aarch64("CangJieLLDBAdapter_macos_arm64"),
    Linux_x64("CangJieLLDBAdapter_linux_amd64"),
    Linux_aarch64("CangJieLLDBAdapter_linux_arm64");


    companion object {
        /**
         * 根据当前系统返回
         */
        fun getFileName(): String {
            return when {
                SystemInfo.isWindows -> DebuggerExe.Windows.fileName
                SystemInfo.isMac && SystemInfo.isAarch64 -> DebuggerExe.MacOS_aarch64.fileName
                SystemInfo.isMac -> DebuggerExe.MacOS_x64.fileName
                SystemInfo.isLinux && SystemInfo.isAarch64 -> DebuggerExe.Linux_aarch64.fileName
                SystemInfo.isLinux -> DebuggerExe.Linux_x64.fileName
                else -> {
                    throw UnsupportedOperationException(
                        "Unsupported platform: ${SystemInfo.OS_NAME} ${SystemInfo.OS_ARCH}"
                    )
                }
            }
        }


    }
}