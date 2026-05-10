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

import com.intellij.testFramework.LightProjectDescriptor
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CangJieLightCodeInsightFixtureTestCaseFrameworkTest : CangJieNoPlatformTestBase() {
    fun testMethodLevelTestMetadataOverridesDefaultFileName() {
        val probe = FixtureFrameworkProbe()

        assertEquals("method-level.cj", probe.fileNameFor("testMethodLevelMetadata"))
    }

    fun testDefaultFileNameFallsBackToSnakeCaseAndCjExtension() {
        val probe = FixtureFrameworkProbe()

        assertEquals("without_metadata.cj", probe.fileNameFor("testWithoutMetadata"))
    }

    fun testProjectDescriptorSelectionUsesAnnotation() {
        val probe = FixtureFrameworkProbe()

        assertSame(CangJieStdlibLightProjectDescriptor.INSTANCE, probe.projectDescriptorFor("testWithStdlibDescriptor"))
        assertSame(CangJieLightProjectDescriptor.INSTANCE, probe.projectDescriptorFor("testWithoutMetadata"))
    }

    fun testUnknownProjectDescriptorKindFailsExplicitly() {
        val probe = FixtureFrameworkProbe()

        val error = assertFailsWith<IllegalStateException> {
            probe.projectDescriptorFor("testWithUnknownDescriptor")
        }
        assertTrue(error.message!!.contains("Unknown value for project descriptor kind"))
    }

    fun testTestDataPathUsesClassLevelTestRootAndMetadata() {
        val probe = FixtureFrameworkProbe()
        val path = probe.testDataPathValue().replace('\\', '/')

        assertTrue(path.endsWith("intellij-ide/modules/test-support/src/testData/framework/probe/"))
    }
}

@TestRoot("intellij-ide/modules/test-support/src/testData")
@TestMetadata("framework/probe")
private class FixtureFrameworkProbe : CangJieLightCodeInsightFixtureTestCase() {
    fun fileNameFor(methodName: String): String {
        setName(methodName)
        return fileName()
    }

    fun projectDescriptorFor(methodName: String): LightProjectDescriptor {
        setName(methodName)
        return getProjectDescriptorFromAnnotation()
    }

    fun testDataPathValue(): String = getTestDataPath()

    @TestMetadata("method-level.cj")
    fun testMethodLevelMetadata() {}

    @ProjectDescriptorKind(CANGJIE_WITH_STDLIB)
    fun testWithStdlibDescriptor() {}

    @ProjectDescriptorKind("UNKNOWN")
    fun testWithUnknownDescriptor() {}

    fun testWithoutMetadata() {}
}
