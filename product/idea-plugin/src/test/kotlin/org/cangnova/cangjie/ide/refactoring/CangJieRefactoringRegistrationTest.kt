package org.cangnova.cangjie.ide.refactoring

import com.intellij.lang.LanguageRefactoringSupport
import com.intellij.refactoring.rename.RenameInputValidator
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.cangnova.cangjie.codeinsight.refactoring.CangJieRefactoringSupportProvider
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.test.CangJieLightCodeInsightFixtureTestCase
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 锁定 shipped IDEA 插件里的语言级重构装配。
 *
 * 这里不验证完整重构矩阵，而是先保证：
 * 1. `LanguageRefactoringSupport` 已经对仓颉语言注册；
 * 2. 基础 rename 链路能在产品插件环境下真正跑通；
 * 3. provider 暴露的 safe delete 判断命中仓颉命名声明。
 */
class CangJieRefactoringRegistrationTest : CangJieLightCodeInsightFixtureTestCase() {
    fun testLanguageRefactoringSupportIsRegisteredInProductPlugin() {
        val provider = LanguageRefactoringSupport.INSTANCE.forLanguage(CangJieLanguage)
        assertEquals(
            "org.cangnova.cangjie.codeinsight.refactoring.CangJieRefactoringSupportProvider",
            provider::class.java.name,
        )
    }

    fun testRenameInputValidatorIsRegisteredInProductPlugin() {
        val file = myFixture.configureByText(
            "renameInputValidator.cj",
            """
            package sample.refactor

            func candidate(): Int64 {
                return 1
            }
            """.trimIndent(),
        )
        val declaration = PsiTreeUtil.findChildOfType(file, CjNamedFunction::class.java)
            ?: error("测试文件里必须存在命名函数声明。")
        val validator = RenameInputValidator.EP_NAME.extensionList.singleOrNull {
            it::class.java.name ==
                "org.cangnova.cangjie.codeinsight.refactoring.rename.CangJieDeclarationRenameInputValidator"
        }

        assertNotNull(validator, "产品插件必须注册仓颉 rename 输入校验器。")
        assertTrue(validator.isInputValid("renamedCandidate", declaration, ProcessingContext()))
        assertTrue(validator.isInputValid("class", declaration, ProcessingContext()))
        assertFalse(validator.isInputValid("", declaration, ProcessingContext()))
    }

    fun testRenameAtCaretWorksForNamedFunction() {
        myFixture.configureByText(
            "renameFunction.cj",
            """
            package sample.refactor

            func gre<caret>et(): Int64 {
                return 1
            }

            func useGreeting(): Int64 {
                return greet()
            }
            """.trimIndent(),
        )

        myFixture.renameElementAtCaret("renamedGreet")

        myFixture.checkResult(
            """
            package sample.refactor

            func renamedGreet(): Int64 {
                return 1
            }

            func useGreeting(): Int64 {
                return renamedGreet()
            }
            """.trimIndent(),
        )
    }

    fun testProviderMarksNamedFunctionAsSafeDeleteCandidate() {
        val file = myFixture.configureByText(
            "safeDeleteCandidate.cj",
            """
            package sample.refactor

            func candidate(): Int64 {
                return 1
            }
            """.trimIndent(),
        )

        val declaration = PsiTreeUtil.findChildOfType(file, CjNamedFunction::class.java)
            ?: error("测试文件里必须存在命名函数声明。")

        val provider = LanguageRefactoringSupport.INSTANCE.forLanguage(CangJieLanguage) as CangJieRefactoringSupportProvider
        assertTrue(provider.isSafeDeleteAvailable(declaration))
    }
}
