package org.cangnova.cangjie.ide.quickfix

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.codeinsight.api.applicators.fixes.CangJieQuickFixService
import org.cangnova.cangjie.codeinsight.api.applicators.fixes.CangJieQuickFixRegistrar
import org.cangnova.cangjie.ide.core.overrideImplement.CangJieOverrideImplementBundle
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.CangJieLightCodeInsightFixtureTestCase
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 锁定产品插件中的 quick-fix 装配主链。
 *
 * 这里必须跑在 `product/idea-plugin`：
 * - `CangJieK2QuickFixRegistrar` 通过 plugin xml 扩展点装配；
 * - `CangJieDiagnosticHighlightVisitor` 通过 shipped descriptor 消费 `CangJieQuickFixService`；
 * - 只有产品插件级 light fixture 才能同时覆盖这两条链路。
 */
class CangJieQuickFixRegistrationTest : CangJieLightCodeInsightFixtureTestCase() {
    fun testAbstractMemberNotImplementedRegistersImplementMembersQuickFix() {
        myFixture.configureByText(
            "implementMembersQuickFix.cj",
            """
            package sample.quickfix

            interface Worker {
                func work(): Int64
            }

            class <caret>WorkerImpl <: Worker {
            }
            """.trimIndent(),
        )

        assertTrue(
            CangJieQuickFixRegistrar.allQuickFixesList().isNotEmpty(),
            "产品插件环境必须加载 codeinsight.quickfix.registrar 扩展。",
        )

        val errors = myFixture.doHighlighting(HighlightSeverity.ERROR)
        assertTrue(
            errors.any { highlight -> highlight.description?.contains("ABSTRACT_MEMBER_NOT_IMPLEMENTED") == true },
            "抽象成员未实现时必须先产出 ABSTRACT_MEMBER_NOT_IMPLEMENTED 诊断，quick-fix 才有注册入口。",
        )

        val quickFixText = CangJieOverrideImplementBundle.message("implement.members.handler.family")
        val cjFile = myFixture.file as CjFile
        val directQuickFixTexts = ApplicationManager.getApplication().executeOnPooledThread<List<String>> {
            runReadAction {
                analyze(cjFile) {
                    val abstractMemberNotImplemented = cjFile
                        .collectDiagnostics(CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
                        .single { it.factoryName == "ABSTRACT_MEMBER_NOT_IMPLEMENTED" }
                    with(CangJieQuickFixService.getInstance()) {
                        getQuickFixesFor(abstractMemberNotImplemented).map { it.text }
                    }
                }
            }
        }.get()
        assertTrue(
            quickFixText in directQuickFixTexts,
            "CangJieQuickFixService 必须能为 ABSTRACT_MEMBER_NOT_IMPLEMENTED 产出 Implement Members，actual=$directQuickFixTexts",
        )

        val allAvailableQuickFixes = myFixture.availableIntentions
        val availableQuickFixes = allAvailableQuickFixes.filter { it.text == quickFixText }
        assertEquals(
            1,
            availableQuickFixes.size,
            "产品插件环境必须只暴露一个 Implement Members quick-fix，actual=${
                allAvailableQuickFixes.map { "${it.text} (${it.javaClass.name})" }
            }",
        )
        assertEquals(quickFixText, availableQuickFixes.single().text)
    }
}
