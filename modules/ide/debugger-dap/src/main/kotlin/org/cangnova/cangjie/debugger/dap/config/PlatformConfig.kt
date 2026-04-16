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

package org.cangnova.cangjie.debugger.dap.config

import com.intellij.openapi.util.SystemInfo
import java.io.File

/**
 * 平台配置
 *
 * 提供平台特定的配置和路径
 */
object PlatformConfig {

    /**
     * 获取平台信息
     */
    fun getPlatformInfo(): PlatformInfo {
        return PlatformInfo(
            os = getOS(),
            arch = getArchitecture(),
            separator = File.separator
        )
    }

    private fun getOS(): OS {
        return when {
            SystemInfo.isWindows -> OS.WINDOWS
            SystemInfo.isMac -> OS.MACOS
            SystemInfo.isLinux -> OS.LINUX
            else -> OS.UNKNOWN
        }
    }

    private fun getArchitecture(): Architecture {
        val arch = SystemInfo.OS_ARCH.lowercase()
        return when {
            arch.contains("aarch64") || arch.contains("arm64") -> Architecture.ARM64
            arch.contains("x86_64") || arch.contains("amd64") -> Architecture.X64
            else -> Architecture.UNKNOWN
        }
    }

    /**
     * 获取平台特定的库路径
     */
    fun getLibraryPath(sdkHome: String): String {
        return when (getOS()) {
            OS.WINDOWS -> "$sdkHome\\third_party\\llvm\\lldb\\lib"
            OS.MACOS, OS.LINUX -> "$sdkHome/third_party/llvm/lldb/lib"
            OS.UNKNOWN -> ""
        }
    }
}

/**
 * 平台信息
 */
data class PlatformInfo(
    val os: OS,
    val arch: Architecture,
    val separator: String
)

/**
 * 操作系统
 */
enum class OS {
    WINDOWS,
    MACOS,
    LINUX,
    UNKNOWN
}

/**
 * 架构
 */
enum class Architecture {
    X64,
    ARM64,
    UNKNOWN
}