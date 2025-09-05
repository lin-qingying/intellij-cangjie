package org.cangnova.cangjie.utils

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


    throw UnsupportedOperationException("Unsupported architecture")
}

fun getOsName(): String {
    return when {
        SystemInfo.isWindows -> "windows"
        SystemInfo.isMac -> "darwin"
        SystemInfo.isUnix -> "linux"
        else -> error("不支持的操作系统")
    }
}