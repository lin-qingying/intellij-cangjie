package org.cangnova.cangjie.ide.decompiled

import com.intellij.testFramework.LightVirtualFile
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.decompiled.psi.CangJieDecompiledFileViewProvider
import org.cangnova.cangjie.analysis.decompiled.psi.file.CjDecompiledFile
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.test.CangJieLightPlatformCodeInsightFixtureTestCase
import kotlin.test.assertEquals

class CangJieDecompiledFileTextContractTest : CangJieLightPlatformCodeInsightFixtureTestCase() {
    fun testTextLengthTracksDecompiledTextContract() {
        val failureText = """
            // Could not decompile the file: CangJie file stub is not found
            // Please report an issue: https://kotl.in/issue
        """.trimIndent()

        val virtualFile = LightVirtualFile("broken.cjo", CangJieBuiltInFileType, "")
        val provider = CangJieDecompiledFileViewProvider(PsiManager.getInstance(project), virtualFile, false) { null }
        val file = object : CjDecompiledFile(provider) {
            override fun getText(): String {
                return failureText
            }
        }

        assertEquals(failureText, file.text)
        assertEquals(failureText.length, file.textLength)
        assertEquals(failureText.toCharArray().concatToString(), file.textToCharArray().concatToString())
    }
}
