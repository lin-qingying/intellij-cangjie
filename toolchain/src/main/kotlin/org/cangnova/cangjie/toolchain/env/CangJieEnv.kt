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

package org.cangnova.cangjie.toolchain.env

import com.intellij.openapi.util.SystemInfo
import java.nio.file.Path

/**
 * 仓颉运行环境
 */
sealed class CangJieEnv(val cangjieHome: Path) {
    val ENV = mutableMapOf<String, String>().apply {
        put("CANGJIE_HOME", cangjieHome.toString())
    }

    //        架构名称
    val archName = getArchName()

    val userHome = System.getProperty("user.home")

    abstract fun getEnvVars(): Map<String, String>

    companion object {
        fun getInstance(cangjieHome: Path): CangJieEnv {
            return when {
                SystemInfo.isWindows -> WindowsCangJieEnv(cangjieHome)
                SystemInfo.isMac -> MacCangJieEnv(cangjieHome)
                SystemInfo.isUnix -> UnixCangJieEnv(cangjieHome)

                else -> error("不支持的操作系统")
            }
        }


    }

    /**
     * windows环境
     */
    class WindowsCangJieEnv(cangjieHome: Path) : CangJieEnv(cangjieHome) {
        override fun getEnvVars(): Map<String, String> {
            ENV["PATH"] = listOf(
                "${cangjieHome}\\runtime\\lib\\windows_x86_64_llvm",
                "${cangjieHome}\\bin",
                "${cangjieHome}\\tools\\bin",
                "${cangjieHome}\\tools\\lib",
                "$userHome\\.cjpm\\bin",
                System.getenv("PATH"),
            ).joinToString(";") { it }
            return ENV
        }


    }

    class UnixCangJieEnv(cangjieHome: Path) : CangJieEnv(cangjieHome) {
        override fun getEnvVars(): Map<String, String> {
            ENV["PATH"] = listOf(
                "$cangjieHome/bin",
                "$cangjieHome/tools/bin",
                "$userHome/.cjpm/bin",
                System.getenv("PATH")
            ).joinToString(":") { it }

            ENV["LD_LIBRARY_PATH"] = listOfNotNull(
                "${cangjieHome}/runtime/lib/linux_${archName}_llvm",
                "${cangjieHome}/tools/lib",
                System.getenv("LD_LIBRARY_PATH")

            ).joinToString(":") { it }

            return ENV
        }


    }

    class MacCangJieEnv(cangjieHome: Path) : CangJieEnv(cangjieHome) {
        override fun getEnvVars(): Map<String, String> {
            ENV["PATH"] = listOf(
                "$cangjieHome/bin",
                "$cangjieHome/tools/bin",
                "$userHome/.cjpm/bin",
                System.getenv("PATH")
            ).joinToString(":") { it }

            ENV["DYLD_LIBRARY_PATH"] = listOfNotNull(
                "${cangjieHome}/runtime/lib/darwin_${archName}_llvm",
                "${cangjieHome}/tools/lib",
                System.getenv("DYLD_LIBRARY_PATH")

            ).joinToString(":") { it }

            return ENV
        }


    }


}

/**
 * 获取架构对应的名称
 */
fun getArchName(): String {
    val arch = SystemInfo.OS_ARCH
//    如果是amd64则返回x86_64
//    如果是arm64则返回aarch64

    if (arch == "amd64" || arch == "x86_64") {
        return "x86_64"
    }
    if (arch == "arm64" || arch == "aarch64") {
        return "aarch64"
    }


    throw UnsupportedOperationException("不支持的架构")
}
