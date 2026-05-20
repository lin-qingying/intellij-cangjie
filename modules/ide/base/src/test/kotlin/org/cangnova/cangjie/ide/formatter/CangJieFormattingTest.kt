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

package org.cangnova.cangjie.ide.formatter

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import org.cangnova.cangjie.test.CangJieLightPlatformCodeInsightFixtureTestCase
import kotlin.test.assertEquals

class CangJieFormattingTest : CangJieLightPlatformCodeInsightFixtureTestCase() {

    fun testCodeStyleManagerReformatsFunctionBodyAndBinarySpacing() {
        myFixture.configureByText(
            "formatting.cj",
            """
            func main(){
            let value=1+2
            }
            """.trimIndent(),
        )

        reformatFixtureFile()

        assertEquals(
            """
            func main() {
                let value = 1 + 2
            }
            """.trimIndent(),
            myFixture.file.text.trimEnd(),
        )
    }

    fun testCodeStyleManagerReformatsClassAndFunctionBlocks() {
        myFixture.configureByText(
            "formatting.cj",
            """
            class Box{
            func value(): Int64{
            return 1+2
            }
            }
            """.trimIndent(),
        )

        reformatFixtureFile()

        assertEquals(
            """
            class Box {
                func value(): Int64 {
                    return 1 + 2
                }
            }
            """.trimIndent(),
            myFixture.file.text.trimEnd(),
        )
    }

    /**
     * IDE 侧格式化测试直接走平台 `CodeStyleManager`，验证插件扩展能复用共享 formatter。
     */
    private fun reformatFixtureFile() {
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()
    }
}
