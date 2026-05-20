package org.cangnova.cangjie.ide.highlighter

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import org.cangnova.cangjie.highlighter.BeforeResolveHighlightingVisitor
import org.cangnova.cangjie.highlighter.CangJieHighlightInfoTypeSemanticNames
import org.cangnova.cangjie.test.CangJieLightPlatformCodeInsightFixtureTestCase
import kotlin.test.assertTrue

class CangJieSourceHighlightingTest : CangJieLightPlatformCodeInsightFixtureTestCase() {
    fun testSourceDeclarationsUseSharedStructuralHighlightingRules() {
        myFixture.configureByText(
            "sourceHighlighting.cj",
            """
            class SourceClass {}
            struct SourceStruct {}
            interface SourceInterface {}
            type SourceAlias = SourceClass

            class Holder {
                prop member: Int64 {
                    get() {
                        return 0
                    }
                }

                func memberFunction(parameter: Int64): Int64 {
                    let localValue = parameter
                    return localValue
                }
            }
            """.trimIndent(),
        )

        val highlights = collectBeforeResolveHighlights()

        assertHighlight(highlights, "SourceClass", CangJieHighlightInfoTypeSemanticNames.CLASS)
        assertHighlight(highlights, "SourceStruct", CangJieHighlightInfoTypeSemanticNames.STRUCT)
        assertHighlight(highlights, "SourceInterface", CangJieHighlightInfoTypeSemanticNames.INTERFACE)
        assertHighlight(highlights, "SourceAlias", CangJieHighlightInfoTypeSemanticNames.TYPE_ALIAS)
        assertHighlight(highlights, "memberFunction", CangJieHighlightInfoTypeSemanticNames.FUNCTION_DECLARATION)
        assertHighlight(highlights, "member", CangJieHighlightInfoTypeSemanticNames.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION)
        assertHighlight(highlights, "parameter", CangJieHighlightInfoTypeSemanticNames.PARAMETER)
        assertHighlight(highlights, "localValue", CangJieHighlightInfoTypeSemanticNames.LOCAL_VARIABLE)
    }

    private fun collectBeforeResolveHighlights(): List<HighlightInfo> {
        val holder = HighlightInfoHolder(myFixture.file)
        val visitor = BeforeResolveHighlightingVisitor(holder)
        myFixture.file.accept(
            object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    element.accept(visitor)
                    super.visitElement(element)
                }
            },
        )
        return (0 until holder.size()).map(holder::get)
    }

    private fun assertHighlight(
        highlights: List<HighlightInfo>,
        text: String,
        expectedType: HighlightInfoType,
    ) {
        val matchingHighlights = highlights.filter { highlight ->
            myFixture.file.text.substring(highlight.actualStartOffset, highlight.actualEndOffset) == text
        }
        assertTrue(
            matchingHighlights.any { highlight -> highlight.type == expectedType },
            "Expected `$text` to be highlighted as $expectedType, actual=${matchingHighlights.map { it.type }}",
        )
    }
}
