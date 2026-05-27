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
 */

package org.cangnova.cangjie.test

import com.intellij.lang.LanguageParserDefinitions
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiFile
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.RunAll
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.junit.runner.RunWith
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.test.asserter

/**
 * 对位 Kotlin `KotlinLightPlatformCodeInsightFixtureTestCase` 的仓颉平台级 light fixture 基类。
 *
 * 该层只负责：
 * 1. 提供统一的轻量级项目描述符；
 * 2. 统一 test data 路径与文件名推导；
 * 3. 保留 IntelliJ/JUnit3 风格测试命名到文件名的映射。
 *
 * toolchain、stdlib、库装配等更高层测试输入不在这里处理。
 */
@Suppress("LeakingThis")
@RunWith(CangJieJUnit4TestRunner::class)
abstract class CangJieLightPlatformCodeInsightFixtureTestCase : BasePlatformTestCase(), CangJieTestCase {
    private var vfsDisposable = Ref<Disposable>()

    override fun setUp() {
        super.setUp()
        ensureCangJieParserDefinitionRegistered()
        vfsDisposable = CangJieTestUtils.allowProjectRootAccess(this)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = CangJieLightProjectDescriptor.INSTANCE

    override fun getTestDataPath(): String = TestMetadataUtil.getTestDataPath(javaClass)

    protected open fun fileName(): String =
        CangJieTestUtils.getTestDataFileName(javaClass, name) ?: "$testName.$testFileExtension"

    protected val fileNameWithExtension: String
        get() = fileName()

    protected val testName: String
        get() = getTestName(true)

    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        val camelCase = super.getTestName(lowercaseFirstLetter)
        return TestCase.camelOrWordsToSnake(camelCase)
    }

    override fun tearDown() {
        RunAll.runAll(
            { CangJieTestUtils.disposeVfsRootAccess(vfsDisposable) },
            { super.tearDown() },
        )
    }

    /**
     * 模块级 light fixture 测试不依赖 plugin.xml 的文件类型装配，
     * 直接用仓颉 file type 构造 PSI，避免 `*.cj` 在当前宿主下退化成 plain text。
     */
    protected fun configureCangJieByText(text: String): PsiFile {
        ensureCangJieParserDefinitionRegistered()
        return myFixture.configureByText(CangJieFileType.INSTANCE, text)
    }

    private fun ensureCangJieParserDefinitionRegistered() {
        if (LanguageParserDefinitions.INSTANCE.forLanguage(CangJieLanguage) == null) {
            LanguageParserDefinitions.INSTANCE.addExplicitExtension(CangJieLanguage, CangJieParserDefinition())
        }
    }

    /** Asserts that the [actual] value is not `null`, with an optional [message]. */
    @OptIn(ExperimentalContracts::class)
    fun <T : Any> assertNotNull(actual: T?, message: String? = null): T {
        contract { returns() implies (actual != null) }
        asserter.assertNotNull(message, actual)
        return actual!!
    }
}
