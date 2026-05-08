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

import junit.framework.Test
import org.junit.internal.MethodSorter
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.TestClass
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Collections

/**
 * 对齐 Kotlin 插件测试框架的 JUnit 运行入口。
 *
 * 仓颉现有插件测试同时存在：
 * 1. JUnit4 注解风格；
 * 2. IntelliJ/JUnit3 传统 `testXxx` 命名风格。
 *
 * 因此测试框架层统一提供兼容这两种测试发现方式的运行器。
 */
open class CangJieJUnit4TestRunner(
    private val declaringTestClass: Class<*>,
) : BlockJUnit4ClassRunner(declaringTestClass) {
    override fun computeTestMethods(): List<FrameworkMethod> =
        addJUnit3Methods(super.computeTestMethods(), declaringTestClass)

    companion object {
        fun addJUnit3Methods(junit4Methods: List<FrameworkMethod>, testClass: Class<*>): List<FrameworkMethod> {
            val junit3Methods = computeJUnit3TestMethods(testClass)
            if (junit3Methods.isEmpty()) {
                return junit4Methods
            }

            val all = junit4Methods.toMutableList()
            junit3Methods.mapTo(all) { FrameworkMethod(it) }
            return Collections.unmodifiableList(all)
        }

        private fun computeJUnit3TestMethods(testClass: Class<*>): List<Method> {
            var currentClass: Class<*>? = testClass
            val visitedNames = mutableSetOf<String>()
            val testMethods = mutableListOf<Method>()

            while (currentClass != null && Test::class.java.isAssignableFrom(currentClass)) {
                for (method in MethodSorter.getDeclaredMethods(currentClass)) {
                    val methodName = method.name
                    if (visitedNames.add(methodName) && isJUnit3TestMethod(method)) {
                        testMethods += method
                    }
                }
                currentClass = currentClass.superclass
            }

            return testMethods
        }

        private fun isJUnit3TestMethod(method: Method): Boolean {
            return method.parameterTypes.isEmpty() &&
                method.name.startsWith("test") &&
                method.returnType == Void.TYPE &&
                Modifier.isPublic(method.modifiers) &&
                method.getAnnotation(org.junit.Test::class.java) == null
        }
    }
}

/**
 * 不依赖 IntelliJ 平台容器的纯单元测试基类。
 */
@RunWith(CangJieJUnit4TestRunner::class)
abstract class CangJieNoPlatformTestBase : junit.framework.TestCase()

interface TestCase {
    val testFileExtension: String

    fun getTestName(lowercaseFirstLetter: Boolean): String

    companion object {
        const val testResourcesPath: String = "src/test/resources"

        @JvmStatic
        fun camelOrWordsToSnake(name: String): String {
            if (' ' in name) {
                return name.trim().replace(" ", "_")
            }

            return name.split("(?=[A-Z])".toRegex()).joinToString("_", transform = String::lowercase)
        }
    }
}

interface CangJieTestCase : TestCase {
    override val testFileExtension: String
        get() = "cj"
}

/**
 * 旧测试入口保留为框架级别别名，新的 light fixture 测试应直接继承
 * [CangJieLightPlatformCodeInsightFixtureTestCase] 或 [CangJieLightCodeInsightFixtureTestCase]。
 */
@RunWith(CangJieJUnit4TestRunner::class)
abstract class CangJieTestBase : CangJieLightPlatformCodeInsightFixtureTestCase()
