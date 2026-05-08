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

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TestFilesTest : CangJieNoPlatformTestBase() {
    fun testSingleFileParsingWithoutFileDirective() {
        val files = TestFiles.createTestFiles(
            "single.cj",
            """
            // WITH_STDLIB
            main()
            """.trimIndent(),
            object : TestFiles.TestFileFactoryNoModules<CangJieBaseTest.TestFile>() {
                override fun create(fileName: String, text: String, directives: Directives): CangJieBaseTest.TestFile {
                    return CangJieBaseTest.TestFile(fileName, text, directives)
                }
            },
        )

        assertEquals(1, files.size)
        val file = files.single()
        assertEquals("single.cj", file.name)
        assertTrue("WITH_STDLIB" in file.directives)
        assertEquals("// WITH_STDLIB\nmain()", file.content)
    }

    fun testMultiModuleParsingResolvesDependenciesAndFriends() {
        data class ParsedFile(
            val module: CangJieBaseTest.TestModule?,
            val fileName: String,
            val text: String,
            val directives: Directives,
        )

        val files = TestFiles.createTestFiles(
            "ignored.cj",
            """
            // WITH_STDLIB
            // MODULE: lib
            // FILE: lib.cj
            public class Lib {}

            // MODULE: main(lib)(lib)
            // FILE: main.cj
            public class Main {}
            """.trimIndent(),
            object : TestFiles.TestFileFactory<CangJieBaseTest.TestModule, ParsedFile> {
                override fun createFile(
                    module: CangJieBaseTest.TestModule?,
                    fileName: String,
                    text: String,
                    directives: Directives,
                ): ParsedFile {
                    return ParsedFile(module, fileName, text, directives)
                }

                override fun createModule(
                    name: String,
                    dependencies: List<String>,
                    friends: List<String>,
                ): CangJieBaseTest.TestModule {
                    return CangJieBaseTest.TestModule(name, dependencies, friends)
                }
            },
        )

        assertEquals(2, files.size)

        val libModule = files[0].module
        val mainModule = files[1].module
        assertEquals("lib", libModule?.name)
        assertEquals("main", mainModule?.name)
        assertSame(libModule, mainModule?.dependencies?.single())
        assertSame(libModule, mainModule?.friends?.single())
        assertTrue("WITH_STDLIB" in files[0].directives)
        assertTrue("WITH_STDLIB" in files[1].directives)
        assertFalse(files[0].text.isBlank())
        assertFalse(files[1].text.isBlank())
    }

    @Test
    fun annotatedJUnit4MethodIsAlsoDiscovered() {
        assertTrue(true)
    }
}
