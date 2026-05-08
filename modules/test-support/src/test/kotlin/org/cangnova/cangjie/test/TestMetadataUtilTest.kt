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

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestMetadataUtilTest : CangJieNoPlatformTestBase() {
    @TestRoot("intellij-ide/modules/test-support")
    private open class RootAnnotatedBase

    @TestMetadata("src/test/kotlin/org/cangnova/cangjie/test")
    private class MetadataAnnotated : RootAnnotatedBase()

    @TestRoot("intellij-ide")
    private class Outer {
        @TestMetadata("modules/test-support")
        class Nested
    }

    private class RootOnlyAnnotated : RootAnnotatedBase()

    fun testGetTestRootFromSuperclassAnnotation() {
        val root = requireNotNull(TestMetadataUtil.getTestRoot(MetadataAnnotated::class.java))
        assertTrue(root.path.replace('\\', '/').endsWith("intellij-ide/modules/test-support"))
    }

    fun testGetTestDataCombinesRootAndMetadata() {
        val testData = requireNotNull(TestMetadataUtil.getTestData(MetadataAnnotated::class.java))
        assertTrue(testData.path.replace('\\', '/').endsWith("intellij-ide/modules/test-support/src/test/kotlin/org/cangnova/cangjie/test"))
    }

    fun testGetTestDataPathFallsBackToRepositoryRootWhenMetadataMissing() {
        val repositoryRoot = CangJiePluginTestCaseBase.locateRepositoryRoot().toFile()
        val expectedPath = repositoryRoot.absolutePath.let {
            if (it.endsWith(File.separator)) it else it + File.separator
        }

        assertEquals(expectedPath, TestMetadataUtil.getTestDataPath(RootOnlyAnnotated::class.java))
    }

    fun testGetAnnotationValueCanReadEnclosingClassAnnotation() {
        val testRoot = TestMetadataUtil.getAnnotationValue(Outer.Nested::class.java, TestRoot::class.java)
        val metadata = TestMetadataUtil.getTestMetadata(Outer.Nested::class.java)

        assertEquals("intellij-ide", testRoot?.value)
        assertEquals("modules/test-support", metadata)
    }
}
