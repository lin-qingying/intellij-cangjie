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

package org.cangnova.cangjie.lsp4ij

import com.google.gson.JsonObject
import org.cangnova.cangjie.CangJieTestBase
import org.eclipse.lsp4j.InitializeParams

/**
 * 测试更新后的initializationOptions功能
 *
 * 注意：每个LSP服务器实例对应一个CjProject
 */
class InitializationOptionsTest : CangJieTestBase() {

    private lateinit var lspClientFeatures: CangJieLSPClientFeatures

    override fun setUp() {
        super.setUp()
        lspClientFeatures = CangJieLSPClientFeatures()
    }

    fun testInitializationOptionsContainsRequiredFields() {
        val initializeParams = InitializeParams()

        // 初始化参数
        lspClientFeatures.initializeParams(initializeParams)

        // 验证initializationOptions不为空
        assertNotNull(initializeParams.initializationOptions)

        val options = initializeParams.initializationOptions as? JsonObject
        assertNotNull("initializationOptions should be JsonObject", options)

        // 验证核心字段存在
        assertTrue("Should contain modulesHomeOption", options?.has("modulesHomeOption") ?: false)
        assertTrue("Should contain stdLibPathOption", options?.has("stdLibPathOption") ?: false)
        assertTrue("Should contain multiModuleOption", options?.has("multiModuleOption") ?: false)
        assertTrue("Should contain conditionCompileOption", options?.has("conditionCompileOption") ?: false)
        assertTrue("Should contain conditionCompilePaths", options?.has("conditionCompilePaths") ?: false)
    }

    fun testModulesHomeOptionUsesSdkPath() {
        val initializeParams = InitializeParams()

        lspClientFeatures.initializeParams(initializeParams)

        val options = initializeParams.initializationOptions as? JsonObject
        val modulesHomeOption = options?.get("modulesHomeOption")?.asString

        // 验证SDK路径已设置（实际路径取决于测试环境的SDK配置）
        assertNotNull("modulesHomeOption should be set", modulesHomeOption)
    }

    fun testStdLibPathOptionPointsToStdlib() {
        val initializeParams = InitializeParams()

        lspClientFeatures.initializeParams(initializeParams)

        val options = initializeParams.initializationOptions as? JsonObject
        val stdLibPathOption = options?.get("stdLibPathOption")?.asString

        // 验证stdlib路径已设置
        assertNotNull("stdLibPathOption should be set", stdLibPathOption)
        // 验证路径包含stdlib
        if (stdLibPathOption != null) {
            assertTrue("stdLibPathOption should contain 'stdlib'",
                stdLibPathOption.contains("stdlib"))
        }
    }
}
