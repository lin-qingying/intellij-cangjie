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
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.StubBuilder
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.analysis.decompiled.psi.CangJieDecompiledFileViewProvider
import org.cangnova.cangjie.analysis.decompiled.psi.file.CjDecompiledFile
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportList
import org.cangnova.cangjie.psi.stubs.CangJieImportDirectiveStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieImportDirectiveStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieNameReferenceExpressionStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJiePlaceHolderStubImpl
import org.cangnova.cangjie.psi.stubs.impl.deepCopy
import org.cangnova.cangjie.test.CangJieLightPlatformCodeInsightFixtureTestCase
import java.lang.reflect.Method
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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

        val descriptorDump = describeDescriptors(builder, document, descriptors)
        assertTrue("..." in placeholders, "missing imports placeholder, descriptors=$descriptorDump")
        assertTrue("/ block text .../" in placeholders, "missing block comment placeholder, descriptors=$descriptorDump")
        assertTrue("/** doc text ...*/" in placeholders, "missing cdoc placeholder, descriptors=$descriptorDump")
        assertEquals(3, placeholders.size, "unexpected descriptor count, descriptors=$descriptorDump")
        assertTrue(descriptors.all { "\n" in it.range.substring(document.text) }, "single-line descriptor found, descriptors=$descriptorDump")
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

        val descriptorDump = describeDescriptors(builder, document, descriptors)
        val callDescriptor = descriptors.singleOrNull { builder.placeholderTextOf(it) == "(...)" }
        assertNotNull(callDescriptor, "call folding descriptor missing, descriptors=$descriptorDump")
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

    fun testBuildsDecompiledImportDescriptorsInsideDocumentRange() {
        val fileStub = CangJieFileStubImpl.forFile(FqName("sample"))
        val importListStub = CangJiePlaceHolderStubImpl<CjImportList>(fileStub, CjStubElementTypes.IMPORT_LIST)
        createImportDirectiveStub(importListStub, "dep.alpha")
        createImportDirectiveStub(importListStub, "dep.beta")

        val virtualFile = LightVirtualFile("imports.cjo", CangJieBuiltInFileType, "")
        val provider = CangJieDecompiledFileViewProvider(PsiManager.getInstance(project), virtualFile, false) { null }
        val file = TestDecompiledImportFile(provider, fileStub)
        val document = DocumentImpl(file.text)
        val importList = assertNotNull(file.importList, "Decompiled PSI should expose import list from compiled stub.")

        assertTrue(
            importList.textRange.endOffset <= document.textLength,
            "Import list range must stay inside decompiled document. range=${importList.textRange}, documentLength=${document.textLength}, text=${document.text}",
        )

        val builder = CangJieFoldingBuilder()
        val descriptors = mutableListOf<FoldingDescriptor>()
        BUILD_LANGUAGE_FOLD_REGIONS.invoke(builder, descriptors, file, document, false)

        val importsDescriptor = descriptors.singleOrNull { builder.placeholderTextOf(it) == "..." }
        assertNotNull(importsDescriptor, "Expected imports folding descriptor for decompiled import list.")
        assertTrue(
            descriptors.all { descriptor ->
                descriptor.range.startOffset >= 0 &&
                    descriptor.range.endOffset <= document.textLength &&
                    descriptor.range.startOffset <= descriptor.range.endOffset
            },
            "All decompiled folding descriptors must stay inside document. actual=${describeDescriptors(builder, document, descriptors)}; documentLength=${document.textLength}; text=${document.text}",
        )
    }

    private fun buildDescriptors(text: String): Triple<CangJieFoldingBuilder, Document, List<FoldingDescriptor>> {
        configureCangJieByText(text)
        val builder = CangJieFoldingBuilder()
        val descriptors = mutableListOf<FoldingDescriptor>()
        val document = myFixture.editor.document
        BUILD_LANGUAGE_FOLD_REGIONS.invoke(builder, descriptors, myFixture.file, document, false)
        return Triple(builder, document, descriptors)
    }

    private fun createImportDirectiveStub(parent: CangJiePlaceHolderStubImpl<CjImportList>, fqName: String) {
        val importedFqName = FqName(fqName)
        val importDirectiveStub = CangJieImportDirectiveStubImpl(
            parent,
            FqName("sample"),
            listOf(
                CangJieImportDirectiveStub.ImportItemInfo(
                    importedFqName = importedFqName,
                    isAllUnder = false,
                    aliasName = null,
                ),
            ),
        )
        importedFqName.pathSegments().forEach { segment ->
            CangJieNameReferenceExpressionStubImpl(importDirectiveStub, StringRef.fromString(segment.asString()))
        }
    }

    /**
     * IDE 侧测试只验证桥接结果，placeholder 仍然必须来自共享 folding collector。
     */
    private fun CangJieFoldingBuilder.placeholderTextOf(descriptor: FoldingDescriptor): String {
        return GET_LANGUAGE_PLACEHOLDER_TEXT.invoke(this, descriptor.element, descriptor.range) as String
    }

    private fun describeDescriptors(
        builder: CangJieFoldingBuilder,
        document: Document,
        descriptors: List<FoldingDescriptor>,
    ): String {
        if (descriptors.isEmpty()) return "[]"
        return descriptors.joinToString(
            prefix = "[",
            postfix = "]",
        ) { descriptor ->
            val preview = if (descriptor.range.endOffset <= document.textLength) {
                descriptor.range.substring(document.text).replace("\n", "\\n")
            } else {
                "<outside-document>"
            }
            "{placeholder=${builder.placeholderTextOf(descriptor)}, range=${descriptor.range}, text=$preview}"
        }
    }

    private class TestDecompiledImportFile(
        provider: CangJieDecompiledFileViewProvider,
        fileStub: CangJieFileStubImpl,
    ) : CjDecompiledFile(provider) {
        private val renderedText: String by lazy(LazyThreadSafetyMode.NONE) {
            renderDecompiledText(fileStub)
        }

        override val customStubBuilder: StubBuilder = TestStubBuilder(fileStub)

        override fun getText(): String = renderedText
    }

    private class TestStubBuilder(
        private val fileStub: CangJieFileStubImpl,
    ) : StubBuilder {
        override fun buildStubTree(file: PsiFile): CangJieFileStubImpl {
            val cloned = fileStub.deepCopy()
            cloned.psi = file as CjFile
            return cloned
        }
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

        private fun renderDecompiledText(fileStub: CangJieFileStubImpl): String {
            val owner = Class.forName("org.cangnova.cangjie.analysis.decompiled.psi.text.DecompiledTextBuilderKt")
            val renderer = owner.declaredMethods.firstOrNull { method ->
                method.name.contains("buildDecompiledText") &&
                    method.parameterTypes.contentEquals(arrayOf(CangJieFileStubImpl::class.java))
            } ?: error("buildDecompiledText(CangJieFileStubImpl) not found")
            renderer.isAccessible = true
            return renderer.invoke(null, fileStub) as String
        }
    }
}
