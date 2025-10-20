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

package org.cangnova.cangjie

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.Test
import org.junit.internal.MethodSorter
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.TestClass
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.test.asserter

/**
 * 这是一个自定义的 JUnit4 测试运行器，它支持两种风格的测试方法：
 * 1. JUnit4 风格 - 使用 @Test 注解
 * 2. JUnit3 风格 - 方法名以 "test" 开头
 */
class CangJieJUnit4TestRunner(testClass: Class<*>) : BlockJUnit4ClassRunner(testClass) {

    override fun computeTestMethods(): List<FrameworkMethod> =
        addJUnit3Methods(super.computeTestMethods(), testClass)

    companion object {
        fun addJUnit3Methods(junit4Methods: List<FrameworkMethod>, testClass: TestClass): List<FrameworkMethod> {
            val junit3Methods = computeJUnit3TestMethods(testClass)

            return if (junit3Methods.isEmpty()) {
                junit4Methods
            } else {
                val all = junit4Methods.toMutableList()
                junit3Methods.mapTo(all) { FrameworkMethod(it) }
                Collections.unmodifiableList(all)
            }
        }

        private fun computeJUnit3TestMethods(testClass: TestClass): List<Method> {
            val theClass = testClass.javaClass
            var superClass = theClass
            val names = mutableSetOf<String>()
            val testMethods = mutableListOf<Method>()
            while (Test::class.java.isAssignableFrom(superClass)) {
                for (method in MethodSorter.getDeclaredMethods(superClass)) {
                    val name = method.name
                    if (!names.contains(name) && isJUnit3TestMethod(method)) {
                        names.add(name)
                        testMethods += method
                    }
                }
                superClass = superClass.superclass
            }

            return testMethods
        }

        private fun isJUnit3TestMethod(m: Method): Boolean {
            return m.parameterTypes.isEmpty()
                    && m.name.startsWith("test")
                    && m.returnType == Void.TYPE
                    && Modifier.isPublic(m.modifiers)
                    && m.getAnnotation(org.junit.Test::class.java) == null
        }
    }
}

/**
 * 不需要Intellij平台的测试类,可以提升运行速度
 */
@RunWith(CangJieJUnit4TestRunner::class)
abstract class CangJieNoPlatformTestBase : junit.framework.TestCase() {

}

/**
 * Intellij平台测试类
 */
@RunWith(CangJieJUnit4TestRunner::class)
abstract class CangJieTestBase : BasePlatformTestCase(), CangJieTestCase {
    open val dataPath: String = ""

    override fun getProjectDescriptor(): LightProjectDescriptor = CangJieLightProjectDescriptor

    private object CangJieLightProjectDescriptor : LightProjectDescriptor()

    private fun setupCangJieProject() {
        if (projectDescriptor != CangJieLightProjectDescriptor) {
         CangJieProjectDescriptorHolder.disposePreviousDescriptor()
            return
        }
    }

    /**
     * Holds a static instance of [RustProjectDescriptorBase] between tests in order to speed up tests.
     *
     * (This is similar to [com.intellij.testFramework.LightPlatformTestCase.ourProject], see
     * [com.intellij.testFramework.LightPlatformTestCase.doSetup])
     */
    private object CangJieProjectDescriptorHolder {
//        var previousDescriptorKey: CangJieProjectDescriptorKey? = null
        var disposable: Disposable? = null

        fun disposePreviousDescriptor() {
//            previousDescriptorKey = null
            val disposable = disposable
            if (disposable != null) {
                Disposer.dispose(disposable)
                this.disposable = null

            }
        }
    }
    private data class CangJieProjectDescriptorKey(
        val project: Project,
        val projectDir: VirtualFile,

    )

    override fun getTestDataPath(): String = "${TestCase.testResourcesPath}/$dataPath"


    protected val fileName: String
        get() = "$testName.cj"



    override fun setUp() {
        super.setUp()
//   setupCangJieProject()


    }

    /** Asserts that the [actual] value is not `null`, with an optional [message]. */
    @OptIn(ExperimentalContracts::class)
    public fun <T : Any> assertNotNull(actual: T?, message: String? = null): T {
        contract { returns() implies (actual != null) }
        asserter.assertNotNull(message, actual)
        return actual!!
    }

    private val testName: String
        get() = getTestName(true)

    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        val camelCase = super.getTestName(lowercaseFirstLetter)
        return TestCase.camelOrWordsToSnake(camelCase)
    }
}

interface TestCase {
    val testFileExtension: String

    //    fun getTestDataPath(): String
    fun getTestName(lowercaseFirstLetter: Boolean): String

    companion object {
//        const val testResourcesPath = "testFixtures"
        const val testResourcesPath = "src/test/resources"
        @JvmStatic
        fun camelOrWordsToSnake(name: String): String {
            if (' ' in name) return name.trim().replace(" ", "_")

            return name.split("(?=[A-Z])".toRegex()).joinToString("_", transform = String::lowercase)
        }
    }
}

interface CangJieTestCase : TestCase {
    override val testFileExtension: String get() = "cj"
}
