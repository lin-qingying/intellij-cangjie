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

import com.intellij.util.text.SemVer
import org.cangnova.cangjie.CangJieTestBase
import org.cangnova.cangjie.toolchain.CangJieSdkVersion
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import org.cangnova.cangjie.toolchain.api.CjSdk
import org.cangnova.cangjie.toolchain.api.CjSdkRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CjProjectSdkConfigImplTest : CangJieTestBase() {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var projectConfig: CjProjectSdkConfig
    private lateinit var sdkRegistry: CjSdkRegistry

    @Before
   public override fun setUp() {
        super.setUp()
        projectConfig = CjProjectSdkConfig.getInstance(project)
        sdkRegistry = CjSdkRegistry.getInstance()
    }

    @Test
    fun `test getProjectSdkId initially returns null`() {
        val sdkId = projectConfig.getProjectSdkId()

        assertNull(sdkId)
    }

    @Test
    fun `test hasProjectSdk initially returns false`() {
        val hasSdk = projectConfig.hasProjectSdk()

        assertFalse(hasSdk)
    }

    @Test
    fun `test setProjectSdkId updates SDK ID`() {
        val testSdkId = "test-sdk-id"

        projectConfig.setProjectSdkId(testSdkId)

        assertEquals(testSdkId, projectConfig.getProjectSdkId())
        assertTrue(projectConfig.hasProjectSdk())
    }

    @Test
    fun `test setProjectSdkId to null clears configuration`() {
        projectConfig.setProjectSdkId("test-sdk-id")

        projectConfig.setProjectSdkId(null)

        assertNull(projectConfig.getProjectSdkId())
        assertFalse(projectConfig.hasProjectSdk())
    }

    @Test
    fun `test getProjectSdk returns null when SDK not registered`() {
        projectConfig.setProjectSdkId("non-existent-sdk")

        val sdk = projectConfig.getProjectSdk()

        assertNull(sdk)
    }

    @Test
    fun `test getProjectSdk returns SDK when registered`() {
        // 创建并注册SDK
        val testSdk = createTestSdk("registered-sdk")
        sdkRegistry.registerSdk(testSdk)

        // 设置项目SDK
        projectConfig.setProjectSdkId(testSdk.id)

        // 验证能获取到SDK
        val sdk = projectConfig.getProjectSdk()
        assertNotNull(sdk)
        assertEquals(testSdk.id, sdk!!.id)
        assertEquals(testSdk.name, sdk.name)
    }

    @Test
    fun `test getProjectSdk returns null when no SDK configured`() {
        val sdk = projectConfig.getProjectSdk()

        assertNull(sdk)
    }

    @Test
    fun `test multiple setProjectSdkId updates correctly`() {
        val sdk1 = createTestSdk("sdk-1")
        val sdk2 = createTestSdk("sdk-2")

        sdkRegistry.registerSdk(sdk1)
        sdkRegistry.registerSdk(sdk2)

        // 先设置sdk1
        projectConfig.setProjectSdkId(sdk1.id)
        assertEquals(sdk1.id, projectConfig.getProjectSdk()?.id)

        // 再设置sdk2
        projectConfig.setProjectSdkId(sdk2.id)
        assertEquals(sdk2.id, projectConfig.getProjectSdk()?.id)
    }

    // Helper method
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
}
