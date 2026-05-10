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

import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import org.junit.runner.RunWith
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CangJieBaseClassFrameworkTest : CangJieNoPlatformTestBase() {
    fun testNoPlatformBaseUsesJUnit4RunnerAndJUnit3TestCaseContract() {
        val runWith = requireNotNull(CangJieNoPlatformTestBase::class.java.getAnnotation(RunWith::class.java))

        assertSame(CangJieJUnit4TestRunner::class.java, runWith.value.java)
        assertTrue(junit.framework.TestCase::class.java.isAssignableFrom(CangJieNoPlatformTestBase::class.java))
    }

    fun testLegacyTestBaseUsesJUnit4RunnerAndLightPlatformBaseAlias() {
        val runWith = requireNotNull(CangJieTestBase::class.java.getAnnotation(RunWith::class.java))

        assertSame(CangJieJUnit4TestRunner::class.java, runWith.value.java)
        assertSame(CangJieLightPlatformCodeInsightFixtureTestCase::class.java, CangJieTestBase::class.java.superclass)
    }

    fun testMultiModuleBaseUsesJUnit4RunnerAndDoesNotExposeKotlinPluginMode() {
        val runWith = requireNotNull(AbstractCangJieMultiModuleTest::class.java.getAnnotation(RunWith::class.java))

        assertSame(CangJieJUnit4TestRunner::class.java, runWith.value.java)
        assertTrue(AbstractCangJieMultiModuleTest::class.java.methods.none { method -> method.name == "getPluginMode" })
    }

    fun testLightPlatformBaseUsesSnakeCaseFileNameAndDefaultDescriptor() {
        val probe = LightPlatformFrameworkProbe()

        assertEquals("sample_name.cj", probe.fileNameFor("testSampleName"))
        assertEquals("sample_name", probe.testNameValue(false))
        assertSame(CangJieLightProjectDescriptor.INSTANCE, probe.projectDescriptorValue())
    }

    fun testLightPlatformBaseUsesClassLevelTestRootAndMetadataPath() {
        val probe = LightPlatformFrameworkProbe()
        val path = probe.testDataPathValue().replace('\\', '/')

        assertTrue(path.endsWith("intellij-ide/modules/test-support/src/testData/framework/platformProbe/"))
    }
}

@TestRoot("intellij-ide/modules/test-support/src/testData")
@TestMetadata("framework/platformProbe")
private class LightPlatformFrameworkProbe : CangJieLightPlatformCodeInsightFixtureTestCase() {
    fun fileNameFor(methodName: String): String {
        setName(methodName)
        return fileName()
    }

    fun testNameValue(lowercaseFirstLetter: Boolean): String = getTestName(lowercaseFirstLetter)

    fun projectDescriptorValue() = getProjectDescriptor()

    fun testDataPathValue(): String = getTestDataPath()

    fun testSampleName() {}
}

@TestRoot("intellij-ide/modules/test-support/src/testData")
@TestMetadata("framework/platformProbe")
private class MultiModuleFrameworkProbe : AbstractCangJieMultiModuleTest() {
    fun fileNameFor(methodName: String): String {
        setName(methodName)
        return fileName()
    }

    fun testNameValue(lowercaseFirstLetter: Boolean): String = getTestName(lowercaseFirstLetter)

    fun testDataPathValue(): String = testDataPath

    fun testSampleName() {}
}

class CangJieMultiModuleFrameworkTest : AbstractCangJieMultiModuleTest() {
    fun testMultiModuleBaseUsesSnakeCaseFileNameAndMetadataPath() {
        val probe = MultiModuleFrameworkProbe()
        val path = probe.testDataPathValue().replace('\\', '/')

        assertEquals("sample_name.cj", probe.fileNameFor("testSampleName"))
        assertEquals("sample_name", probe.testNameValue(false))
        assertTrue(path.endsWith("intellij-ide/modules/test-support/src/testData/framework/platformProbe/"))
    }

    fun testMultiModuleBaseCreatesRealModulesDependenciesAndProjectLibraries() {
        val dependencyModule = createModuleInTmpDir(
            name = "dependency",
            createFiles = { listOf(FileWithText("dependency.cj", "main() {}")) },
        )
        val consumerModule = createModuleInTmpDir("consumer")

        consumerModule.addDependency(dependencyModule, DependencyScope.TEST, exported = true)

        val libraryRoot = createLocalDirectory(Files.createTempDirectory("cangjie-framework-library"))
        consumerModule.addProjectLibrary(
            libraryName = "framework-library",
            classesRoot = libraryRoot,
            dependencyScope = DependencyScope.PROVIDED,
            exported = true,
        )

        val orderEntries = ModuleRootManager.getInstance(consumerModule).orderEntries.toList()
        val moduleDependency = orderEntries.filterIsInstance<ModuleOrderEntry>().single {
            it.moduleName == dependencyModule.name
        }
        val libraryDependency = orderEntries.filterIsInstance<LibraryOrderEntry>().single {
            it.libraryName == "framework-library"
        }

        assertEquals(DependencyScope.TEST, moduleDependency.scope)
        assertTrue(moduleDependency.isExported)
        assertEquals(DependencyScope.PROVIDED, libraryDependency.scope)
        assertTrue(libraryDependency.isExported)
    }
}
