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

package org.cangnova.cangjie.ide.editor.folding

import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.test.CangJieLightPlatformCodeInsightFixtureTestCase
import java.lang.reflect.Method
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CangJieFoldingBuilderTest : CangJieLightPlatformCodeInsightFixtureTestCase() {

    fun testBuildsImportsAndCommentDescriptorsFromSharedCollector() {
        val (builder, document, descriptors) = buildDescriptors(
            """
            import std.collection.ArrayList
            import std.collection.HashMap

            /*
             * block text
             */
            /**
             * doc text
             */
            func main() {}
            """.trimIndent(),
        )

        val placeholders = descriptors.map { builder.placeholderTextOf(it) }

        assertTrue("..." in placeholders)
        assertTrue("/ block text .../" in placeholders)
        assertTrue("/** doc text ...*/" in placeholders)
        assertEquals(3, placeholders.size)
        assertTrue(descriptors.all { "\n" in it.range.substring(document.text) })
    }

    fun testBuildsMultilineCallDescriptorWithSharedPlaceholder() {
        val (builder, document, descriptors) = buildDescriptors(
            """
            func main() {
                consume(
                    1,
                    2
                )
            }
            """.trimIndent(),
        )

        val callDescriptor = descriptors.singleOrNull { builder.placeholderTextOf(it) == "(...)" }
        assertNotNull(callDescriptor)
        val foldedText = callDescriptor.range.substring(document.text)
        assertTrue(foldedText.startsWith("("))
        assertTrue(foldedText.endsWith(")"))
        assertTrue(foldedText.contains("\n"))
    }

    fun testSkipsSingleLineBlockAndCallDescriptors() {
        val (builder, document, descriptors) = buildDescriptors(
            """
            func single() { return consume(1, 2) }
            """.trimIndent(),
        )

        assertFalse(descriptors.any { builder.placeholderTextOf(it) == "(...)" })
        assertTrue(descriptors.isEmpty() || descriptors.all { "\n" in it.range.substring(document.text) })
    }

    private fun buildDescriptors(text: String): Triple<CangJieFoldingBuilder, Document, List<FoldingDescriptor>> {
        myFixture.configureByText("folding.cj", text)
        val builder = CangJieFoldingBuilder()
        val descriptors = mutableListOf<FoldingDescriptor>()
        val document = myFixture.editor.document
        BUILD_LANGUAGE_FOLD_REGIONS.invoke(builder, descriptors, myFixture.file, document, false)
        return Triple(builder, document, descriptors)
    }

    /**
     * IDE 侧测试只验证桥接结果，placeholder 仍然必须来自共享 folding collector。
     */
    private fun CangJieFoldingBuilder.placeholderTextOf(descriptor: FoldingDescriptor): String {
        return GET_LANGUAGE_PLACEHOLDER_TEXT.invoke(this, descriptor.element, descriptor.range) as String
    }

    companion object {
        private val BUILD_LANGUAGE_FOLD_REGIONS: Method = CangJieFoldingBuilder::class.java.getDeclaredMethod(
            "buildLanguageFoldRegions",
            MutableList::class.java,
            PsiElement::class.java,
            Document::class.java,
            Boolean::class.javaPrimitiveType,
        ).apply {
            isAccessible = true
        }

        private val GET_LANGUAGE_PLACEHOLDER_TEXT: Method = CangJieFoldingBuilder::class.java.getDeclaredMethod(
            "getLanguagePlaceholderText",
            descriptorElementType(),
            TextRange::class.java,
        ).apply {
            isAccessible = true
        }

        private fun descriptorElementType(): Class<*> {
            val getter = FoldingDescriptor::class.java.methods.firstOrNull { it.name == "getElement" && it.parameterCount == 0 }
                ?: error("FoldingDescriptor#getElement not found")
            return getter.returnType
        }
    }
}
