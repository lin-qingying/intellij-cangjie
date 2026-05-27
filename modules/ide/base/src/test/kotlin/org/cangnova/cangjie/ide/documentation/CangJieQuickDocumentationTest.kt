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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.ide.documentation

import com.intellij.lang.documentation.ide.IdeDocumentationTargetProvider
import com.intellij.platform.backend.documentation.impl.computeDocumentationBlocking
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.cangnova.cangjie.test.CangJieLightPlatformCodeInsightFixtureTestCase
import kotlin.test.assertContains
import kotlin.test.assertEquals

class CangJieQuickDocumentationTest : CangJieLightPlatformCodeInsightFixtureTestCase() {

    fun testTopLevelFunctionDocumentation() {
        val html = configureAndRender(
            """
            package sample.docs

            /**
             * Returns greeting.
             * @return greeting text
             */
            func gre<caret>et(): String {
                return "hello"
            }
            """.trimIndent(),
        )

        assertNotNull(html)
        assertContains(html!!, "func greet(): String")
        assertContains(html, "Returns greeting.")
        assertContains(html, "@return greeting text")
    }

    fun testClassDocumentation() {
        val html = configureAndRender(
            """
            package sample.docs

            /**
             * Greeter class doc.
             */
            class Gre<caret>eter {
            }
            """.trimIndent(),
        )

        assertNotNull(html)
        assertContains(html!!, "class Greeter")
        assertContains(html, "Greeter class doc.")
    }

    fun testStructDocumentation() {
        val html = configureAndRender(
            """
            package sample.docs

            /**
             * Config struct doc.
             */
            struct Con<caret>fig {
            }
            """.trimIndent(),
        )

        assertNotNull(html)
        assertContains(html!!, "struct Config")
        assertContains(html, "Config struct doc.")
    }

    fun testMemberFunctionDocumentationFromReference() {
        val html = configureAndRender(
            """
            package sample.docs

            class Greeter {
                /**
                 * Returns greeting.
                 */
                func greet(): String {
                    return "hello"
                }
            }

            func useGreeter(greeter: Greeter): String {
                return greeter.gre<caret>et()
            }
            """.trimIndent(),
        )

        assertNotNull(html)
        assertContains(html!!, "func greet(): String")
        assertContains(html, "Returns greeting.")
    }

    fun testSignatureStillShownWithoutDocComment() {
        val html = configureAndRender(
            """
            package sample.docs

            func pl<caret>ain(): Int64 {
                return 1
            }
            """.trimIndent(),
        )

        assertNotNull(html)
        assertContains(html!!, "func plain(): Int64")
    }

    fun testTopLevelFunctionDocumentationWithoutDeclaredReturnTypeUsesPsiContainerInfo() {
        val html = configureAndRender(
            """
            package sample.docs

            func gre<caret>et() {
            }
            """.trimIndent(),
        )

        assertNotNull(html)
        assertContains(html!!, "func greet()")
        assertContains(html, "sample.docs")
    }

    fun testInlineDocumentationUsesSameBody() {
        val file = myFixture.configureByText(
            "inlineDocs.cj",
            """
            package sample.docs

            /**
             * Inline greeting docs.
             * @return inline greeting text
             */
            func greet(): String {
                return "hello"
            }
            """.trimIndent(),
        )

        val provider = CangJieInlineDocumentationProvider()
        val items = provider.inlineDocumentationItems(file)
        assertEquals(1, items.size)

        val item = items.single()
        val text = item.renderText()
        assertNotNull(text)
        assertContains(text!!, "Inline greeting docs.")
        assertContains(text, "@return inline greeting text")
    }

    private fun configureAndRender(text: String): String? {
        myFixture.configureByText("quickDoc.cj", text)
        return renderDocumentation(myFixture)
    }

    private fun renderDocumentation(fixture: CodeInsightTestFixture): String? {
        val target = IdeDocumentationTargetProvider.getInstance(project)
            .documentationTargets(fixture.editor, fixture.file, fixture.editor.caretModel.offset)
            .firstOrNull()
            ?: return null

        return computeDocumentationBlocking(target.createPointer())?.html
    }
}
