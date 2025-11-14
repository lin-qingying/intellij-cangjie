/*
 * Copyright 2025 LinQingYing.
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
 */

package org.cangnova.cangjie.protodebugger.path

import com.intellij.concurrency.ConcurrentCollectionFactory
import com.intellij.ide.plugins.PluginManagerCore.getPlugin
import com.intellij.ide.plugins.cl.PluginAwareClassLoader
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.system.CpuArch
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.PosixFilePermission
import java.util.concurrent.ConcurrentHashMap

// =====================================================
// ⚙️ 全局日志与缓存
// =====================================================

private val LOG = Logger.getInstance("#org.cangnova.cangjie.protodebugger.path")
private val pluginPathsCache = ConcurrentHashMap<String, Ref<Path>>()
private val executableMarkedFiles = ConcurrentCollectionFactory.createConcurrentSet<Path>()

// =====================================================
// 🧭 平台路径计算
// =====================================================

@NlsSafe
fun getPlatformRelativePath(relativePath: String, archSpecific: Boolean = true): String {
    val osPart = when {
        SystemInfo.isWindows -> "win"
        SystemInfo.isMac -> "mac"
        else -> "linux"
    }
    val archPart = if (archSpecific) {
        (if (CpuArch.isArm64()) "aarch64" else "x64") + "/"
    } else ""
    return "$osPart/$archPart$relativePath"
}

// =====================================================
// 📦 二进制与资源文件定位
// =====================================================

/**
 * 获取 LLDB 主二进制文件（自动处理架构）
 */
fun getBinFile(
    relativePathToMainFile: String,
    relativePathToAdditionalBinaries: String? = null,
    archSpecific: Boolean = false
): File {
    val platformPath = getPlatformRelativePath("lldb/$relativePathToMainFile", archSpecific)
    val additional = relativePathToAdditionalBinaries?.let {
        getPlatformRelativePath("lldb/$it", archSpecific)
    }
    return getBinPath("cidr-debugger/bin", platformPath, additional).toFile()
}

/** 捆绑的 LLDB STL 渲染器目录 */
val bundledLLDBSTLPrettyPrinters: File
    get() = getBinPath("cidr-debugger/bin", "lldb/renderers").toFile()


// =====================================================
// 🗺️ 插件路径与工作路径解析
// =====================================================

fun getBinPath(
    inSourcesPath: String,
    relativePath: String,
    relativePathToAdditionalBinaries: String? = null,
    ensureIsExecutable: Boolean = true
): Path {
    val pluginPath = getActivePluginPath()
    val baseDir = pluginPath ?: Paths.get(System.getProperty("user.dir")).resolve(inSourcesPath)

    val binFile = baseDir.resolve(relativePath)
    val additionalDir = relativePathToAdditionalBinaries?.let { baseDir.resolve(it) }

    if (ensureIsExecutable) {
        ensureFileIsExecutable(binFile)
        additionalDir?.let { ensureFilesInDirAreExecutable(it) }
    }

    return binFile
}

/**
 * 自动检测当前插件路径（从 ClassLoader 推断）
 */
private fun getActivePluginPath(): Path? {
    val loader = Thread.currentThread().contextClassLoader
    val key = loader::class.java.name
    pluginPathsCache[key]?.get()?.let { return it }

    val ref = Ref<Path>()
    (loader as? PluginAwareClassLoader)?.pluginId?.let { pluginId ->
        getPlugin(pluginId)?.pluginPath?.let { ref.set(it) }
    }
    pluginPathsCache[key] = ref
    return ref.get()
}

// =====================================================
// 🔒 文件可执行权限控制
// =====================================================

private fun ensureFilesInDirAreExecutable(dir: Path) {
    try {
        Files.newDirectoryStream(dir).use { stream ->
            for (child in stream) ensureFileIsExecutable(child)
        }
    } catch (e: IOException) {
        LOG.warn("Unable to traverse directory for exec flag: $dir", e)
    }
}

private fun ensureFileIsExecutable(file: Path): Path {
    if (executableMarkedFiles.contains(file)) return file

    if (Files.isRegularFile(file) && !Files.isExecutable(file)) {
        try {
            val perms = Files.getPosixFilePermissions(file).toMutableSet()
            perms += setOf(
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_EXECUTE
            )
            Files.setPosixFilePermissions(file, perms)
            LOG.info("Marked as executable: $file")
        } catch (e: IOException) {
            LOG.warn("Cannot set executable flag for $file", e)
        }
    }

    executableMarkedFiles.add(file)
    return file
}

// =====================================================
// 🌍 路径映射数据类
// =====================================================

data class PathMapping(val from: String, val to: String)
