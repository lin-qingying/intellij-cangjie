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

package org.cangnova.cangjie.toolchain.impl

import com.intellij.openapi.util.SystemInfo
import org.cangnova.cangjie.CangJieTestBase
import org.cangnova.cangjie.toolchain.api.CjSdkDiscoverer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class CjSdkDiscovererImplTest : CangJieTestBase() {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var discoverer: CjSdkDiscoverer

    @Before
   public override fun setUp() {
        super.setUp()
        discoverer = CjSdkDiscoverer.getInstance()!!
    }

    @Test
    fun `test priority is 0`() {
        assertEquals(0, discoverer.priority)
    }

    @Test
    fun `test displayName is not empty`() {
        assertTrue(discoverer.displayName.isNotEmpty())
    }

    @Test
    fun `test searchSdkPaths with non-existent path`() {
        val nonExistentPath = tempFolder.root.toPath().resolve("non-existent")

        val results = discoverer.searchSdkPaths(nonExistentPath)

        assertTrue(results.isEmpty())
    }

    @Test
    fun `test searchSdkPaths with file instead of directory`() {
        val file = tempFolder.newFile("test.txt")

        val results = discoverer.searchSdkPaths(file.toPath())

        assertTrue(results.isEmpty())
    }

    @Test
    fun `test searchSdkPaths finds SDK in root path`() {
        // 创建有效的 SDK 结构
        val sdkDir = tempFolder.newFolder("cangjie-sdk")
        val binDir = sdkDir.toPath().resolve("bin")
        Files.createDirectories(binDir)

        // 创建必需的可执行文件
        val executableName = if (SystemInfo.isWindows) "cjc.exe" else "cjc"
        val cjcPath = binDir.resolve(executableName)
        Files.createFile(cjcPath)
        cjcPath.toFile().setExecutable(true)

        val results = discoverer.searchSdkPaths(sdkDir.toPath(), recursive = false)

        assertEquals(1, results.size)
        assertEquals(sdkDir.toPath(), results[0])
    }

    @Test
    fun `test searchSdkPaths with recursive search`() {
        // 创建嵌套的 SDK 结构
        val rootDir = tempFolder.newFolder("sdks")
        val sdk1Dir = rootDir.toPath().resolve("sdk1")
        val sdk2Dir = rootDir.toPath().resolve("subdir/sdk2")

        // 创建 SDK1
        Files.createDirectories(sdk1Dir.resolve("bin"))
        val execName = if (SystemInfo.isWindows) "cjc.exe" else "cjc"
        val cjc1 = sdk1Dir.resolve("bin").resolve(execName)
        Files.createFile(cjc1)
        cjc1.toFile().setExecutable(true)

        // 创建 SDK2
        Files.createDirectories(sdk2Dir.resolve("bin"))
        val cjc2 = sdk2Dir.resolve("bin").resolve(execName)
        Files.createFile(cjc2)
        cjc2.toFile().setExecutable(true)

        val results = discoverer.searchSdkPaths(rootDir.toPath(), recursive = true, maxDepth = 3)

        assertTrue(results.size >= 1) // 至少找到一个
    }

    @Test
    fun `test searchSdkPaths respects maxDepth`() {
        val rootDir = tempFolder.newFolder("sdks")
        val deepSdkDir = rootDir.toPath().resolve("a/b/c/d/sdk")

        // 创建深层 SDK
        Files.createDirectories(deepSdkDir.resolve("bin"))
        val execName = if (SystemInfo.isWindows) "cjc.exe" else "cjc"
        val cjcPath = deepSdkDir.resolve("bin").resolve(execName)
        Files.createFile(cjcPath)
        cjcPath.toFile().setExecutable(true)

        // maxDepth=2 不应该找到深度为 4 的 SDK
        val results = discoverer.searchSdkPaths(rootDir.toPath(), recursive = true, maxDepth = 2)

        assertFalse(results.contains(deepSdkDir))
    }

    @Test
    fun `test searchSdkPaths without recursion only checks root`() {
        val rootDir = tempFolder.newFolder("sdks")
        val subSdkDir = rootDir.toPath().resolve("subsdk")

        // 在子目录创建 SDK
        Files.createDirectories(subSdkDir.resolve("bin"))
        val execName = if (SystemInfo.isWindows) "cjc.exe" else "cjc"
        val cjcPath = subSdkDir.resolve("bin").resolve(execName)
        Files.createFile(cjcPath)
        cjcPath.toFile().setExecutable(true)

        val results = discoverer.searchSdkPaths(rootDir.toPath(), recursive = false)

        // 不递归搜索，不应该找到子目录中的 SDK
        assertFalse(results.contains(subSdkDir))
    }

    @Test
    fun `test discoverSdkPaths returns list`() {
        // 这个测试只验证方法不抛异常
        val results = discoverer.discoverSdkPaths()

        assertNotNull(results)
        // 结果可能为空，因为测试环境可能没有安装 SDK
    }
}
