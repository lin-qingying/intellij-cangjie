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

package cn.cangnova.cangjie.toolchain.impl

import cn.cangnova.cangjie.toolchain.api.CjToolchain
import cn.cangnova.cangjie.toolchain.api.CjToolchainProvider
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

/**
 * 官方CangJie工具链提供者
 */
class OfficialCjToolchainProvider : CjToolchainProvider {
    
    override val id: String = "official"
    
    override val displayName: String = "Official CangJie Toolchain"
    
    override fun detectToolchains(): List<CjToolchain> {
        val toolchains = mutableListOf<CjToolchain>()
        
        // 尝试从环境变量中检测
        detectFromEnvironment()?.let { toolchains.add(it) }
        
        // 尝试从标准安装路径中检测
        toolchains.addAll(detectFromStandardPaths())
        
        return toolchains
    }
    
    override fun createToolchain(homePath: Path): CjToolchain? {
        if (!isValidToolchainHome(homePath)) {
            return null
        }
        
        return OfficialCjToolchain(homePath)
    }
    
    private fun detectFromEnvironment(): CjToolchain? {
        val cjHome = System.getenv("CANGJIE_HOME")
        if (cjHome != null) {
            val path = Paths.get(cjHome)
            if (isValidToolchainHome(path)) {
                return OfficialCjToolchain(path)
            }
        }
        return null
    }
    
    private fun detectFromStandardPaths(): List<CjToolchain> {
        val toolchains = mutableListOf<CjToolchain>()
        
        // 检查常见安装路径
        val standardPaths = listOf(
            // Unix-like系统
            Paths.get("/usr/local/cangjie"),
            Paths.get("/opt/cangjie"),
            // Windows系统
            Paths.get(System.getProperty("user.home"), "cangjie"),
            Paths.get(System.getenv("ProgramFiles") ?: "C:\\Program Files", "CangJie")
        )
        
        for (path in standardPaths) {
            if (isValidToolchainHome(path)) {
                toolchains.add(OfficialCjToolchain(path))
            }
        }
        
        // 检查版本目录
        val versionDirs = standardPaths
            .filter { Files.isDirectory(it) }
            .flatMap { 
                try {
                    Files.list(it)
                        .filter { dir -> Files.isDirectory(dir) && dir.fileName.toString().matches(Regex("\\d+\\.\\d+\\.\\d+")) }
                        .collect(Collectors.toList())
                } catch (e: Exception) {
                    emptyList<Path>()
                }
            }
        
        for (versionDir in versionDirs) {
            if (isValidToolchainHome(versionDir)) {
                toolchains.add(OfficialCjToolchain(versionDir))
            }
        }
        
        return toolchains
    }
    
    private fun isValidToolchainHome(path: Path): Boolean {
        if (!Files.isDirectory(path)) {
            return false
        }
        
        val binDir = path.resolve("bin")
        if (!Files.isDirectory(binDir)) {
            return false
        }
        val toolDir = path.resolve("tools")
        if (!Files.isDirectory(toolDir)) {
            return false
        }
        // 检查编译器可执行文件
        val compiler = binDir.resolve(getExecutableName("cjc"))
        if (!Files.exists(compiler) || !Files.isExecutable(compiler)) {
            return false
        }
        val toolsDirBin = toolDir.resolve("bin")
        if (!Files.isDirectory(toolsDirBin)) {
            return false
        }
        // 检查包管理器可执行文件
        val packageManager = toolsDirBin.resolve(getExecutableName("cjpm"))
        if (!Files.exists(packageManager) || !Files.isExecutable(packageManager)) {
            return false
        }
        
        return true
    }
    
    private fun getExecutableName(name: String): String {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        return if (isWindows) "$name.exe" else name
    }
} 