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

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CangJieLightCodeInsightTestCaseFrameworkTest : CangJieNoPlatformTestBase() {
    fun testDirectoryBasedLegacyCaseResolvesMethodMetadataUnderClassMetadataRoot() {
        val probe = LegacyDirectoryProbe()
        probe.setName("testMethodMetadata")

        val directory = probe.testDataDirectoryValue().path.replace('\\', '/')
        val path = probe.testDataPathValue().replace('\\', '/')

        assertTrue(directory.endsWith("intellij-ide/modules/test-support/src/testData/framework/legacyDirectory/method-data.cj"))
        assertTrue(path.endsWith("intellij-ide/modules/test-support/src/testData/framework/legacyDirectory/method-data.cj/"))
    }

    fun testFilesBasedLegacyCaseUsesClassMetadataDirectoryAsRoot() {
        val probe = LegacyFilesBasedProbe()
        probe.setName("testWithoutMethodMetadata")

        val directory = probe.testDataDirectoryValue().path.replace('\\', '/')
        val path = probe.testDataPathValue().replace('\\', '/')

        assertTrue(directory.endsWith("intellij-ide/modules/test-support/src/testData/framework/legacyFilesBased"))
        assertTrue(path.endsWith("intellij-ide/modules/test-support/src/testData/framework/legacyFilesBased/"))
    }

    fun testLegacyFileNameUsesMethodMetadataOrSnakeCaseFallback() {
        val probe = LegacyDirectoryProbe()

        probe.setName("testMethodMetadata")
        assertEquals("method-data.cj", probe.fileNameValue())

        probe.setName("testWithoutMethodMetadata")
        assertEquals("WithoutMethodMetadata.cj", probe.fileNameValue())
    }

    fun testLegacyCaseWithoutClassMetadataFailsExplicitly() {
        val probe = LegacyMissingClassMetadataProbe()
        probe.setName("testMethodMetadata")

        val error = assertFailsWith<IllegalArgumentException> {
            probe.testDataDirectoryValue()
        }
        assertTrue(error.message!!.contains("No metadata for class"))
    }
}

@TestRoot("intellij-ide/modules/test-support/src/testData")
@TestMetadata("framework/legacyDirectory")
private class LegacyDirectoryProbe : CangJieLightCodeInsightTestCase() {
    fun testDataDirectoryValue() = getTestDataDirectory()

    fun testDataPathValue() = getTestDataPath()

    fun fileNameValue() = fileName()

    @TestMetadata("method-data.cj")
    fun testMethodMetadata() {}

    fun testWithoutMethodMetadata() {}
}

@TestRoot("intellij-ide/modules/test-support/src/testData")
@TestMetadata("framework/legacyFilesBased")
private class LegacyFilesBasedProbe : CangJieLightCodeInsightTestCase() {
    override val filesBasedTest: Boolean
        get() = true

    fun testDataDirectoryValue() = getTestDataDirectory()

    fun testDataPathValue() = getTestDataPath()

    fun testWithoutMethodMetadata() {}
}

@TestRoot("intellij-ide/modules/test-support/src/testData")
private class LegacyMissingClassMetadataProbe : CangJieLightCodeInsightTestCase() {
    fun testDataDirectoryValue() = getTestDataDirectory()

    @TestMetadata("method-data.cj")
    fun testMethodMetadata() {}
}
