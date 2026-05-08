package org.cangnova.cangjie.ide.highlighter

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.CangJieLightCodeInsightFixtureTestCase
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 对位 Kotlin `CompiledFilesHighlightingTest` 的产品插件级验证。
 *
 * 这里必须跑在 `product/idea-plugin`，保证测试沙箱加载真实 `plugin.xml`，
 * 从而覆盖 `.cjo` 文件类型、binary decompiler、builtins provider 与编辑器打开链路。
 */
class CangJieCompiledFilesHighlightingTest : CangJieLightCodeInsightFixtureTestCase() {
    fun testStdlibStringResolvesFromConfiguredToolchain() {
        withToolchainStdlibFixture("std.cjo", "std/std.core.cjo", "std/std.objectpool.cjo") {
            myFixture.configureByText(
                "stdlibResolve.cj",
                """
                package sample

                func greet(name: String): String {
                    return name
                }
                """.trimIndent(),
            )

            val errors = myFixture.doHighlighting(HighlightSeverity.ERROR)
            val unresolvedErrors = errors.filter { highlight ->
                highlight.description?.contains("Unresolved reference", ignoreCase = true) == true
            }

            assertTrue(
                unresolvedErrors.isEmpty(),
                "Configured toolchain stdlib should make `String` resolvable. actual=${unresolvedErrors.mapNotNull { it.description }}",
            )
        }
    }

    fun testStdlibCjoOpensAsDecompiledPsi() {
        withToolchainStdlibFixture("std.cjo", "std/std.core.cjo", "std/std.objectpool.cjo") { sdkHome ->
            val cjoPath = sdkHome.resolve("modules").resolve("windows_x86_64_llvm").resolve("std").resolve("std.core.cjo")
            val virtualFile = requireNotNull(VirtualFileManager.getInstance().findFileByNioPath(cjoPath)) {
                "Cannot locate builtins file at $cjoPath"
            }
            assertTrue(
                virtualFile.extension == "cjo",
                "Builtins test input must stay on a real `.cjo` file. actualPath=${virtualFile.path}, actualExtension=${virtualFile.extension}",
            )

            val psiFile = requireNotNull(PsiManager.getInstance(project).findFile(virtualFile)) {
                "Decompiler should provide PSI for $cjoPath"
            }
            assertTrue(
                psiFile is CjFile,
                "Expected decompiled `.cjo` PSI to be CjFile, actual=${psiFile::class.qualifiedName}, fileType=${virtualFile.fileType::class.qualifiedName}/${virtualFile.fileType.name}",
            )
            assertEquals(
                CangJieBuiltInFileType.defaultExtension,
                psiFile.fileType.defaultExtension,
                "Decompiled PSI must expose the builtins file type contract. actual=${psiFile.fileType::class.qualifiedName}/${psiFile.fileType.name}",
            )
            assertTrue(psiFile.text.isNotBlank(), "Decompiled `.cjo` PSI should expose non-empty text")
            assertTrue(
                psiFile.text.contains("String") || psiFile.text.contains("std.core"),
                "Decompiled `.cjo` PSI should expose stdlib declarations. actual preview=${psiFile.text.take(200)}",
            )

            myFixture.openFileInEditor(virtualFile)
            assertSame(virtualFile, myFixture.file.virtualFile)
        }
    }
}
