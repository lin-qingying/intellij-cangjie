package org.cangnova.cangjie.ide.references

import com.intellij.lang.documentation.ide.IdeDocumentationTargetProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.model.psi.PsiSymbolService
import com.intellij.model.psi.impl.targetDeclarationAndReferenceSymbols
import com.intellij.platform.backend.documentation.impl.computeDocumentationBlocking
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReference
import org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.api.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getResolutionFacade
import org.cangnova.cangjie.psi.CangJieReferenceProvidersService
import org.cangnova.cangjie.psi.CjBasicType
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNameReferenceExpression
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.test.CANGJIE_WITH_STDLIB
import org.cangnova.cangjie.test.CangJieLightCodeInsightFixtureTestCase
import org.cangnova.cangjie.test.ProjectDescriptorKind
import org.cangnova.cangjie.utils.runReadActionInSmartMode
import java.util.concurrent.Callable
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * 锁定产品插件里的 reference binding 主链。
 *
 * 这里必须跑在 `product/idea-plugin`：
 * 1. 验证 shipped plugin descriptor 会把 `cj-references` 一起聚合进来；
 * 2. 同时覆盖 source 方法体引用、source -> `.cjo` 引用、`.cjo` 声明名自身 target/doc 三条链路。
 */
class CangJieReferenceBindingTest : CangJieLightCodeInsightFixtureTestCase() {
    fun testReferenceProvidersServiceIsRegisteredInProductPlugin() {
        val service = CangJieReferenceProvidersService.getInstance(project)
        assertEquals(
            "产品插件环境必须装配 cj-references 的 provider service，不能回退到 no-op 实现。",
            "org.cangnova.cangjie.analysis.references.CangJieReferenceProvidersServiceImpl",
            service::class.java.name,
        )

        val platformSettings = CaPlatformSettings.getInstance(project)
        assertEquals(
            "产品插件环境必须装配 IDE 专用 platform settings，不能回退到 shared base 设置。",
            "org.cangnova.cangjie.ide.base.analysisApiPlatform.CaIdePlatformSettings",
            platformSettings::class.java.name,
        )
        assertEquals(
            "产品插件中的库声明必须按 stub origin 进入 Analysis API / low-level API。",
            CaDeserializedDeclarationsOrigin.STUBS,
            platformSettings.deserializedDeclarationsOrigin,
        )
    }

    fun testSourceStatementReferenceProvidesTargetAndDocumentation() {
        myFixture.configureByText(
            "sourceBinding.cj",
            """
            package sample.binding

            class Greeter {
                /**
                 * Returns greeting.
                 */
                func greet(): String {
                    return "hello"
                }
            }

            func useGreeting(greeter: Greeter): String {
                return greeter.gre<caret>et()
            }
            """.trimIndent(),
        )

        assertEquals(listOf("greet"), referenceNamesAtCaret())

        val html = renderDocumentationAtCaret()
        assertNotNull(html, "source 方法体里的普通调用位必须能恢复 Quick Documentation target。")
        val plainText = documentationPlainText(html)
        assertContains(plainText, "func greet(): String")
        assertContains(plainText, "Returns greeting.")
    }

    @ProjectDescriptorKind(CANGJIE_WITH_STDLIB)
    fun testSourceReferenceToCjoProvidesTargetAndDocumentation() {
        myFixture.configureByText(
            "stdlibBinding.cj",
            """
            package sample.binding

            func useText(value: Str<caret>ing): String {
                return value
            }
            """.trimIndent(),
        )

        if (referenceSymbolsAtCaret().isEmpty()) {
            fail("source -> `.cjo` 引用位必须恢复 reference symbol target。 ${referenceDebugSnapshotAtCaret()}")
        }
        if (!referenceTargetElementDebugAtCaret().any { debugName -> debugName.contains("String") }) {
            fail("source -> `.cjo` 引用位提取出的 PSI target 至少应暴露内建声明名。 ${referenceDebugSnapshotAtCaret()}")
        }

        val html = renderDocumentationAtCaret()
        assertNotNull(html, "source -> `.cjo` 引用位必须能恢复 Quick Documentation target。")
        assertContains(html, "String")
    }

    @ProjectDescriptorKind(CANGJIE_WITH_STDLIB)
    fun testFieldBasicTypeReferenceDoesNotProvideTargetOrDocumentation() {
        myFixture.configureByText(
            "fieldBasicTypeBinding.cj",
            """
            package sample.binding

            class Box {
                let count: Int<caret>64 = 0
            }
            """.trimIndent(),
        )

        assertTrue(referenceSymbolsAtCaret().isEmpty(), "基本类型不是可导航声明，不能暴露 reference symbol target。 ${referenceDebugSnapshotAtCaret()}")

        val html = renderDocumentationAtCaret()
        assertTrue(html == null, "基本类型没有可导航声明，不应生成 Quick Documentation target。 ${referenceDebugSnapshotAtCaret()}")
    }

    fun testInterfaceMemberTypeParameterReferenceRemainsResolvable() {
        myFixture.configureByText(
            "interfaceTypeParameterBinding.cj",
            """
            package sample.binding

            public interface Comparable<T> {
                func compareTo(other: <caret>T): Int64 {
                    return 0
                }
            }
            """.trimIndent(),
        )

        assertEquals(listOf("T"), referenceNamesAtCaret())

        val unresolvedErrors = myFixture.doHighlighting(HighlightSeverity.ERROR).filter { highlight ->
            highlight.description?.contains("Unresolved reference", ignoreCase = true) == true &&
                highlight.text == "T"
        }
        assertTrue(
            unresolvedErrors.isEmpty(),
            "产品插件里的 source type-parameter 引用位不应退化成 UNRESOLVED_REFERENCE. actual=${unresolvedErrors.mapNotNull { it.description }}; ${referenceDebugSnapshotAtCaret()}",
        )
    }

    @ProjectDescriptorKind(CANGJIE_WITH_STDLIB)
    fun testCjoDeclarationNameProvidesDeclarationTargetAndDocumentation() {
        val virtualFile = stdlibCjoVirtualFile("std.objectpool.cjo")
        val decompiledFile = requireNotNull(PsiManager.getInstance(project).findFile(virtualFile)) {
            "PsiManager should restore `.cjo` PSI for ${virtualFile.getPath()}"
        }
        myFixture.openFileInEditor(virtualFile)

        val declaration = decompiledFile
            .let { it as CjFile }
            .declarations
            .filterIsInstance<CjTypeStatement>()
            .single { typeStatement -> typeStatement.name == "ObjectPool" }
        val nameIdentifier = requireNotNull(declaration.nameIdentifier) {
            "Decompiled ObjectPool declaration should expose nameIdentifier."
        }
        myFixture.editor.caretModel.moveToOffset(nameIdentifier.getTextOffset() + 1)

        assertEquals(listOf("ObjectPool"), declarationNamesAtCaret())

        val html = renderDocumentationAtCaret()
        assertNotNull(html, "`.cjo` 声明名必须能恢复 Quick Documentation target。")
        assertContains(html, "ObjectPool")
    }

    private fun declarationNamesAtCaret(): List<String> {
        val offset = myFixture.editor.caretModel.offset
        val symbolService = PsiSymbolService.getInstance()
        val (declared, _) = targetDeclarationAndReferenceSymbols(myFixture.file, offset)
        return declared
            .mapNotNull(symbolService::extractElementFromSymbol)
            .filterIsInstance<CjNamedDeclaration>()
            .mapNotNull { declaration -> declaration.name }
    }

    private fun referenceNamesAtCaret(): List<String> {
        return referenceTargetsAtCaret().mapNotNull { declaration -> declaration.name }
    }

    private fun referenceSymbolsAtCaret() = targetDeclarationAndReferenceSymbols(
        myFixture.file,
        myFixture.editor.caretModel.offset,
    ).second

    private fun referenceTargetsAtCaret(): List<CjNamedDeclaration> {
        val symbolService = PsiSymbolService.getInstance()
        return referenceSymbolsAtCaret()
            .mapNotNull(symbolService::extractElementFromSymbol)
            .filterIsInstance<CjNamedDeclaration>()
    }

    private fun referenceTargetElementDebugAtCaret(): List<String> {
        val symbolService = PsiSymbolService.getInstance()
        return referenceSymbolsAtCaret().map { symbol ->
            val element = symbolService.extractElementFromSymbol(symbol)
            when (element) {
                null -> "<null-element:${symbol::class.java.name}>"
                else -> "${element::class.java.name}:${element.text}"
            }
        }
    }

    private fun referenceDebugSnapshotAtCaret(): String {
        val offset = myFixture.editor.caretModel.offset
        val caretElement = requireNotNull(myFixture.file.findElementAt(offset)) {
            "Expected PSI element at caret offset=$offset."
        }
        val simpleName = caretElement.parentsWithSelf().filterIsInstance<CjNameReferenceExpression>().firstOrNull()
        val basicType = caretElement.parentsWithSelf().filterIsInstance<CjBasicType>().firstOrNull()
        val typeReference = caretElement.parentsWithSelf().filterIsInstance<CjTypeReference>().firstOrNull()
        return buildString {
            append("offset=").append(offset)
            append(", caretChain=").append(caretElement.parentsWithSelf().joinToString(" -> ", transform = ::renderPsiDebugName))
            append(", simpleName=").append(simpleName?.let(::renderPsiDebugName) ?: "<none>")
            append(", simpleName.references=").append(simpleName?.references?.map(::renderReferenceDebugName) ?: emptyList<String>())
            append(", simpleName.resolvedTargets=").append(simpleName?.references?.flatMap(::resolveReferenceTargetsDebug) ?: emptyList<String>())
            append(", simpleName.ownReferences=").append(simpleName?.ownReferences?.map(::renderSymbolReferenceDebugName) ?: emptyList<String>())
            append(", simpleName.cfir=").append(simpleName?.let(::renderCfirDebugName) ?: "<none>")
            append(", basicType=").append(basicType?.let(::renderPsiDebugName) ?: "<none>")
            append(", basicType.references=").append(basicType?.references?.map(::renderReferenceDebugName) ?: emptyList<String>())
            append(", basicType.ownReferences=").append(basicType?.ownReferences?.map(::renderSymbolReferenceDebugName) ?: emptyList<String>())
            append(", typeReference=").append(typeReference?.let(::renderPsiDebugName) ?: "<none>")
            append(", typeReference.references=").append(typeReference?.references?.map(::renderReferenceDebugName) ?: emptyList<String>())
            append(", typeReference.ownReferences=").append(typeReference?.ownReferences?.map(::renderSymbolReferenceDebugName) ?: emptyList<String>())
            append(", typeReference.cfir=").append(typeReference?.let(::renderCfirDebugName) ?: "<none>")
            append(", extractedTargets=").append(referenceTargetElementDebugAtCaret())
        }
    }

    private fun PsiElement.parentsWithSelf(): List<PsiElement> {
        return generateSequence(this) { element -> element.parent }.toList()
    }

    private fun renderPsiDebugName(element: PsiElement): String {
        return "${element::class.java.simpleName}(`${element.text}`)"
    }

    private fun renderReferenceDebugName(reference: com.intellij.psi.PsiReference): String {
        return "${reference::class.java.simpleName}@${reference.rangeInElement}"
    }

    private fun renderSymbolReferenceDebugName(reference: com.intellij.model.psi.PsiSymbolReference): String {
        return "${reference::class.java.simpleName}@${reference.rangeInElement}"
    }

    private fun renderCfirDebugName(element: PsiElement): String {
        if (element !is org.cangnova.cangjie.psi.CjElement) {
            return "<non-cj-element:${element::class.java.simpleName}>"
        }
        return ApplicationManager.getApplication().executeOnPooledThread(
            Callable {
                project.runReadActionInSmartMode {
                    val module = CaModuleProvider.getModule(project, element, useSiteModule = null)
                    val cfir = element.getOrBuildCfir(module.getResolutionFacade(project))
                    when (cfir) {
                        null -> "<null>"
                        else -> cfir::class.java.name
                    }
                }
            },
        ).get()
    }

    private fun resolveReferenceTargetsDebug(reference: com.intellij.psi.PsiReference): List<String> {
        val targets = when (reference) {
            is PsiPolyVariantReference -> reference.multiResolve(false).mapNotNull { resolveResult -> resolveResult.element }
            else -> listOfNotNull(reference.resolve())
        }
        return targets.map(::renderPsiDebugName)
    }

    private fun renderDocumentationAtCaret(): String? {
        val editor = myFixture.editor
        val file = myFixture.file
        val offset = editor.caretModel.offset
        return ApplicationManager.getApplication().executeOnPooledThread(
            Callable {
                project.runReadActionInSmartMode {
                    val target = IdeDocumentationTargetProvider.getInstance(project)
                        .documentationTargets(editor, file, offset)
                        .firstOrNull()
                        ?: return@runReadActionInSmartMode null

                    computeDocumentationBlocking(target.createPointer())?.html
                }
            },
        ).get()
    }

    private fun stdlibCjoVirtualFile(fileName: String) = run {
        val builtinsFiles = BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles()
        requireNotNull(builtinsFiles.firstOrNull { file -> file.name.equals(fileName, ignoreCase = true) }) {
            "Cannot locate builtins file `$fileName` from provider. actual=${builtinsFiles.map { it.getPath() }.sorted()}"
        }
    }

    private fun documentationPlainText(html: String): String {
        return html
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+([(),:<>])"), "$1")
            .replace(Regex("([(<])\\s+"), "$1")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

}
