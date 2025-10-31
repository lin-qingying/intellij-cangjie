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
 */

package org.cangnova.cangjie.lsp4ij

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.cangnova.cangjie.project.model.CjProject
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import org.cangnova.cangjie.toolchainapi.CjSdk
import org.cangnova.cangjie.toolchainapi.impl.CjSdkImpl
import org.eclipse.lsp4j.InitializeParams
import org.mockito.Mockito.*
import java.nio.file.Paths

/**
 * 测试更新后的initializationOptions功能
 *
 * 注意：每个LSP服务器实例对应一个CjProject
 */
class InitializationOptionsTest : BasePlatformTestCase() {

    private lateinit var lspClientFeatures: CangJieLSPClientFeatures
    private lateinit var mockSdk: CjSdk

    override fun setUp() {
        super.setUp()
        lspClientFeatures = CangJieLSPClientFeatures()

        // 模拟SDK
        mockSdk = mock(CjSdk::class.java)
        `when`(mockSdk.homePath).thenReturn(Paths.get("/test/sdk"))
    }

    fun testInitializationOptionsContainsRequiredFields() {
        val initializeParams = InitializeParams()

        // 模拟SDK配置
        val sdkConfig = mock(CjProjectSdkConfig::class.java)
        `when`(sdkConfig.getProjectSdk()).thenReturn(mockSdk)

        // 设置项目SDK
        CjProjectSdkConfig.getInstance(project).setProjectSdk(mockSdk)

        // 初始化参数
        lspClientFeatures.initializeParams(initializeParams)

        // 验证initializationOptions不为空
        assertNotNull(initializeParams.initializationOptions)

        val options = initializeParams.initializationOptions

        // 验证核心字段存在
        assertTrue("Should contain modulesHomeOption", options?.has("modulesHomeOption") ?: false)
        assertTrue("Should contain stdLibPathOption", options?.has("stdLibPathOption") ?: false)
        assertTrue("Should contain multiModuleOption", options?.has("multiModuleOption") ?: false)
        assertTrue("Should contain conditionCompileOption", options?.has("conditionCompileOption") ?: false)
        assertTrue("Should contain conditionCompilePaths", options?.has("conditionCompilePaths") ?: false)
    }

    fun testModulesHomeOptionUsesSdkPath() {
        val initializeParams = InitializeParams()

        // 设置项目SDK
        CjProjectSdkConfig.getInstance(project).setProjectSdk(mockSdk)

        lspClientFeatures.initializeParams(initializeParams)

        val modulesHomeOption = initializeParams.initializationOptions?.get("modulesHomeOption") as? String
        assertEquals("/test/sdk", modulesHomeOption)
    }

    fun testStdLibPathOptionPointsToStdlib() {
        val initializeParams = InitializeParams()

        // 设置项目SDK
        CjProjectSdkConfig.getInstance(project).setProjectSdk(mockSdk)

        lspClientFeatures.initializeParams(initializeParams)

        val stdLibPathOption = initializeParams.initializationOptions?.get("stdLibPathOption") as? String
        assertEquals("/test/sdk/stdlib", stdLibPathOption)
    }
}