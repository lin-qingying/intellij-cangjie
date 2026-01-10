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
import org.cangnova.cangjie.CangJieNoPlatformTestBase
import org.cangnova.cangjie.toolchain.api.CjSdkDetector
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class CjSdkDetectorImplTest : CangJieNoPlatformTestBase() {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var detector: CjSdkDetector

    @Before
 public   override fun setUp() {
        detector = CjSdkDetectorImpl()
    }

    @Test
    fun `test requiredExecutables contains cjc`() {
        assertTrue(detector.requiredExecutables.contains("cjc"))
    }

    @Test
    fun `test isValidSdk with non-existent path`() {
        val nonExistentPath = tempFolder.root.toPath().resolve("non-existent")

        assertFalse(detector.isValidSdk(nonExistentPath))
    }

    @Test
    fun `test isValidSdk with file instead of directory`() {
        val file = tempFolder.newFile("test.txt")

        assertFalse(detector.isValidSdk(file.toPath()))
    }

    @Test
    fun `test isValidSdk with directory but no bin folder`() {
        val sdkDir = tempFolder.newFolder("sdk")

        assertFalse(detector.isValidSdk(sdkDir.toPath()))
    }

    @Test
    fun `test isValidSdk with bin folder but missing executables`() {
        val sdkDir = tempFolder.newFolder("sdk")
        val binDir = sdkDir.toPath().resolve("bin")
        Files.createDirectories(binDir)

        assertFalse(detector.isValidSdk(sdkDir.toPath()))
    }

    @Test
    fun `test isValidSdk with valid SDK structure`() {
        val sdkDir = tempFolder.newFolder("sdk")
        val binDir = sdkDir.toPath().resolve("bin")
        Files.createDirectories(binDir)

        // 创建必需的可执行文件
        val executableName = if (SystemInfo.isWindows) "cjc.exe" else "cjc"
        val cjcPath = binDir.resolve(executableName)
        Files.createFile(cjcPath)
        cjcPath.toFile().setExecutable(true)

        assertTrue(detector.isValidSdk(sdkDir.toPath()))
    }

    @Test
    fun `test checkRequiredExecutables with missing all`() {
        val sdkDir = tempFolder.newFolder("sdk")
        val binDir = sdkDir.toPath().resolve("bin")
        Files.createDirectories(binDir)

        val missing = detector.checkRequiredExecutables(sdkDir.toPath())

        assertEquals(detector.requiredExecutables.size, missing.size)
        assertTrue(missing.containsAll(detector.requiredExecutables))
    }

    @Test
    fun `test checkRequiredExecutables with all present`() {
        val sdkDir = tempFolder.newFolder("sdk")
        val binDir = sdkDir.toPath().resolve("bin")
        Files.createDirectories(binDir)

        // 创建所有必需的可执行文件
        detector.requiredExecutables.forEach { execName ->
            val executableName = if (SystemInfo.isWindows) "$execName.exe" else execName
            val execPath = binDir.resolve(executableName)
            Files.createFile(execPath)
            execPath.toFile().setExecutable(true)
        }

        val missing = detector.checkRequiredExecutables(sdkDir.toPath())

        assertTrue(missing.isEmpty())
    }

    @Test
    fun `test createSdk with invalid path`() {
        val invalidPath = tempFolder.root.toPath().resolve("invalid")

        val sdk = detector.createSdk(invalidPath)

        assertNull(sdk)
    }

    @Test
    fun `test createSdk with valid SDK`() {
        val sdkDir = tempFolder.newFolder("sdk")
        val binDir = sdkDir.toPath().resolve("bin")
        Files.createDirectories(binDir)

        // 创建必需的可执行文件
        detector.requiredExecutables.forEach { execName ->
            val executableName = if (SystemInfo.isWindows) "$execName.exe" else execName
            val execPath = binDir.resolve(executableName)
            Files.createFile(execPath)
            execPath.toFile().setExecutable(true)
        }

        val sdk = detector.createSdk(sdkDir.toPath())

        assertNotNull(sdk)
        assertEquals(sdkDir.toPath(), sdk!!.homePath)
        assertTrue(sdk.name.isNotEmpty())
    }

    @Test
    fun `test createSdk with custom name`() {
        val sdkDir = tempFolder.newFolder("sdk")
        val binDir = sdkDir.toPath().resolve("bin")
        Files.createDirectories(binDir)

        // 创建必需的可执行文件
        detector.requiredExecutables.forEach { execName ->
            val executableName = if (SystemInfo.isWindows) "$execName.exe" else execName
            val execPath = binDir.resolve(executableName)
            Files.createFile(execPath)
            execPath.toFile().setExecutable(true)
        }

        val customName = "My Custom SDK"
        val sdk = detector.createSdk(sdkDir.toPath(), customName)

        assertNotNull(sdk)
        assertEquals(customName, sdk!!.name)
    }
}
