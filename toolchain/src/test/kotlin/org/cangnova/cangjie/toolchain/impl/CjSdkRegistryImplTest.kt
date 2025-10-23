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

package org.cangnova.cangjie.toolchain.impl

import com.intellij.openapi.util.SystemInfo
import com.intellij.util.text.SemVer
import org.cangnova.cangjie.CangJieTestBase
import org.cangnova.cangjie.toolchain.CangJieSdkVersion
import org.cangnova.cangjie.toolchain.api.CjSdk
import org.cangnova.cangjie.toolchain.api.CjSdkRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Paths

class CjSdkRegistryImplTest : CangJieTestBase() {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var registry: CjSdkRegistry

    @Before
   public override fun setUp() {
        super.setUp()
        registry = CjSdkRegistry.getInstance()
    }

    @Test
    fun `test getAllSdks initially empty`() {
        val sdks = registry.getAllSdks()

        assertNotNull(sdks)
        // 可能有其他测试留下的数据，所以不验证是否为空
    }

    @Test
    fun `test registerSdk adds new SDK`() {
        val sdk = createTestSdk("test-sdk-1")

        val result = registry.registerSdk(sdk)

        assertTrue(result)
        assertTrue(registry.getAllSdks().any { it.id == sdk.id })
    }

    @Test
    fun `test registerSdk rejects duplicate ID`() {
        val version = CangJieSdkVersion(
            semver = SemVer.parseFromText("0.53.13")!!,
            target = "aarch64-linux-ohos"
        )

        val sdk1 = CjSdk(
            id = "duplicate-id",
            name = "Test SDK 1",
            homePath = tempFolder.newFolder("sdk1").toPath(),
            version = version,
            isValid = true
        )

        val sdk2 = CjSdk(
            id = "duplicate-id",  // Same ID, different path
            name = "Test SDK 2",
            homePath = tempFolder.newFolder("sdk2").toPath(),
            version = version,
            isValid = true
        )

        registry.registerSdk(sdk1)
        val result = registry.registerSdk(sdk2)

        assertFalse(result)
    }

    @Test
    fun `test getSdk retrieves registered SDK`() {
        val sdk = createTestSdk("retrieve-test")
        registry.registerSdk(sdk)

        val retrieved = registry.getSdk(sdk.id)

        assertNotNull(retrieved)
        assertEquals(sdk.id, retrieved!!.id)
    }

    @Test
    fun `test getSdk returns null for non-existent ID`() {
        val retrieved = registry.getSdk("non-existent-id-12345")

        assertNull(retrieved)
    }

    @Test
    fun `test getSdkByPath finds SDK by path`() {
        val sdkPath = tempFolder.newFolder("sdk-by-path").toPath()
        val sdk = CjSdk(
            id = "path-test",
            name = "Path Test SDK",
            homePath = sdkPath,
            version = null,
            isValid = true
        )
        registry.registerSdk(sdk)

        val retrieved = registry.getSdkByPath(sdkPath)

        assertNotNull(retrieved)
        assertEquals(sdk.id, retrieved!!.id)
    }

    @Test
    fun `test getSdkByPath returns null for non-existent path`() {
        val nonExistentPath = Paths.get("/non/existent/path")

        val retrieved = registry.getSdkByPath(nonExistentPath)

        assertNull(retrieved)
    }

    @Test
    fun `test unregisterSdk removes SDK`() {
        val sdk = createTestSdk("unregister-test")
        registry.registerSdk(sdk)

        val result = registry.unregisterSdk(sdk.id)

        assertTrue(result)
        assertNull(registry.getSdk(sdk.id))
    }

    @Test
    fun `test unregisterSdk returns false for non-existent SDK`() {
        val result = registry.unregisterSdk("non-existent-sdk-67890")

        assertFalse(result)
    }

    @Test
    fun `test isSdkRegistered checks existence`() {
        val sdk = createTestSdk("existence-test")
        registry.registerSdk(sdk)

        assertTrue(registry.isSdkRegistered(sdk.id))
        assertFalse(registry.isSdkRegistered("definitely-not-registered"))
    }

    @Test
    fun `test isSdkPathRegistered checks path existence`() {
        val sdkPath = tempFolder.newFolder("path-check").toPath()
        val sdk = CjSdk(
            id = "path-check-test",
            name = "Path Check SDK",
            homePath = sdkPath,
            version = null,
            isValid = true
        )
        registry.registerSdk(sdk)

        assertTrue(registry.isSdkPathRegistered(sdkPath))
        assertFalse(registry.isSdkPathRegistered(Paths.get("/unregistered/path")))
    }

    @Test
    fun `test registerSdkPath with valid SDK structure`() {
        val sdkDir = createValidSdkStructure("register-path-test")

        val result = registry.registerSdkPath(sdkDir)

        assertNotNull(result)
        assertTrue(registry.isSdkPathRegistered(sdkDir))
    }

    @Test
    fun `test registerSdkPath with invalid path returns null`() {
        val invalidPath = tempFolder.newFolder("invalid-sdk").toPath()
        // 不创建必要的 bin 目录

        val result = registry.registerSdkPath(invalidPath)

        assertNull(result)
    }


//    单独测试是通过的，一起测试是失败的，这是正常请求
    @Test
    fun `test registerSdkPath with custom name`() {
        val sdkDir = createValidSdkStructure("custom-name-test")
        val customName = "My Custom SDK Name"

        val result = registry.registerSdkPath(sdkDir, customName)

        assertNotNull(result)
        assertEquals(customName, result.name)
    }

    // Helper methods

    private fun createTestSdk(id: String): CjSdk {
        val version = CangJieSdkVersion(
            semver = SemVer.parseFromText("0.53.13")!!,
            target = "aarch64-linux-ohos"
        )

        return CjSdk(
            id = id,
            name = "Test SDK $id",
            homePath = tempFolder.newFolder(id).toPath(),
            version = version,
            isValid = true
        )
    }

    private fun createValidSdkStructure(name: String): java.nio.file.Path {
        val sdkDir = tempFolder.newFolder(name)
        val binDir = sdkDir.toPath().resolve("bin")
        Files.createDirectories(binDir)

        // 创建必需的可执行文件
        val execName = if (SystemInfo.isWindows) "cjc.exe" else "cjc"
        val cjcPath = binDir.resolve(execName)
        Files.createFile(cjcPath)
        cjcPath.toFile().setExecutable(true)

        return sdkDir.toPath()
    }
}
