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

package org.cangnova.cangjie.analysis

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.util.text.SemVer
import kotlinx.io.files.Path
import org.cangnova.cangjie.CangJieTestBase
import org.cangnova.cangjie.config.LanguageVersionSettingsImpl
import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.moduleinfo.IdeaModuleInfo
import org.cangnova.cangjie.moduleinfo.ModuleOrigin
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.resolve.AnalysisResult
import org.cangnova.cangjie.resolve.CangJieResolverForModuleFactory.Companion.analyzeFiles
import org.cangnova.cangjie.resolve.CompilerEnvironment
import org.cangnova.cangjie.resolve.PlatformDependentAnalyzerServices
import org.cangnova.cangjie.resolve.PlatformDependentAnalyzerServicesImpl
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.caches.CangJieCacheService
import org.cangnova.cangjie.resolve.caches.analyzeWithAllCompilerChecks
import org.cangnova.cangjie.toolchain.CangJieSdkVersion
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import org.cangnova.cangjie.toolchain.api.CjSdk
import org.cangnova.cangjie.toolchain.api.CjSdkRegistry

/**
 * 仓颉语言分析器测试基类
 *
 * 提供完整的语义分析测试环境
 *
 * ## 使用方法
 *
 * ### 方法一：直接测试 PSI 解析（推荐用于简单测试）
 *
 * ```kotlin
 * class MyParserTest : CangJieAnalysisTestBase() {
 *     fun testImportParsing() {
 *         val file = createFile("""
 *             package test
 *             import a.b.C
 *
 *             main() {}
 *         """)
 *
 *         val imports = file.importDirectives
 *         assertEquals(1, imports.size)
 *     }
 * }
 * ```
 *
 * ### 方法二：测试完整的语义分析
 *
 * ```kotlin
 * class MyResolverTest : CangJieAnalysisTestBase() {
 *     fun testResolve() {
 *         val file = createFile("""
 *             package test
 *
 *             func foo() {}
 *         """)
 *
 *         analyzeForTest(file) {
 *             // 在这里进行符号解析、类型推导等测试
 *             val bindingContext = bindingContext
 *             // 进行断言...
 *         }
 *     }
 * }
 * ```
 *
 * ## 设计说明
 *
 * - `analyzeWithAllCompilerChecks()` 会自动创建必需的描述符（ProjectDescriptor、ModuleDescriptor）
 * - 使用 `CjProjectSdkConfig` 进行 SDK 注册和管理
 * - 测试方法可以直接使用 PSI，也可以通过 `analyzeForTest` 获取完整的分析结果
 */
abstract class CangJieAnalysisTestBase : CangJieTestBase() {

    protected lateinit var factory: CjPsiFactory

    /**
     * 设置测试环境
     *
     * 初始化：
     * - PSI 工厂
     * - SDK 配置（如果需要）
     */
    override fun setUp() {
        super.setUp()

        // 创建 PSI 工厂
        factory = CjPsiFactory(project)

        // TODO: 配置 SDK（如果测试需要）
         setupSdk()
    }

    /**
     * 清理测试环境
     */
    override fun tearDown() {
        try {
            // 清理资源
        } finally {
            super.tearDown()
        }
    }



    // ==================== 工具方法 ====================

    /**
     * 创建仓颉文件
     *
     * @param content 文件内容
     * @param fileName 文件名（默认为 test.cj）
     * @return 创建的 CjFile
     */
    protected fun createFile(content: String, fileName: String = "test.cj"): CjFile {
        return factory.createFile(fileName,content, )
    }

    /**
     * 分析文件并在分析上下文中执行操作
     *
     * 该方法会自动创建所需的 ProjectDescriptor 和 ModuleDescriptor，
     * 执行完整的编译器检查。
     *
     * 示例：
     * ```kotlin
     * analyzeForTest(file) {
     *     val bindingContext = bindingContext
     *     // 进行符号解析、类型检查等测试
     * }
     * ```
     *
     * @param file 要分析的文件
     * @param action 在分析上下文中执行的操作
     */
    protected fun <R> analyzeForTest(file: CjFile, action: AnalysisContext.() -> R): R {
        val analysisResult = analyzeFiles(
            files = listOf(file),
            moduleName = org.cangnova.cangjie.name.Name.special("<test>"),
            dependOnBuiltIns = true,
            languageVersionSettings = org.cangnova.cangjie.config.LanguageVersionSettingsImpl.DEFAULT,
            targetEnvironment = org.cangnova.cangjie.resolve.CompilerEnvironment,
            capabilities = emptyMap(),
            explicitProjectContext = null
        )
        analysisResult.throwIfError()

        val context = AnalysisContext(
            bindingContext = analysisResult.bindingContext,
            file = file
        )

        return context.action()
    }





    /**
     * 获取虚拟文件对应的 PSI 文件
     */
    protected fun getPsiFile(virtualFile: VirtualFile): CjFile? {
        return PsiManager.getInstance(project).findFile(virtualFile) as? CjFile
    }


     private fun setupSdk() {
         val sdkConfig = CjProjectSdkConfig.getInstance(project)
         val sdkRegistry = CjSdkRegistry.getInstance()

         // 创建测试 SDK
         val testSdk = createTestSdk()
         sdkRegistry.registerSdk(testSdk)
         sdkConfig.setProjectSdkId(testSdk.id)
     }

    private  fun createTestSdk() : CjSdk{

        return CjSdk(
            id = "test-sdk",
            name = "Test CangJie SDK",
            version = CangJieSdkVersion(SemVer.parseFromText("1.0.0")!!, ""),
            homePath = java.nio.file.Paths.get("C:\\Users\\lin17\\.cangjie\\sdks\\cangjie-1.0.0"),
            isValid = true
        )
    }

    // ==================== 分析上下文 ====================

    /**
     * 分析上下文
     *
     *
     * @property bindingContext 绑定上下文，包含符号解析、类型推导等结果
     * @property file 正在分析的文件
     */
    data class AnalysisContext(
        val bindingContext: BindingContext,
        val file: CjFile
    )


}
