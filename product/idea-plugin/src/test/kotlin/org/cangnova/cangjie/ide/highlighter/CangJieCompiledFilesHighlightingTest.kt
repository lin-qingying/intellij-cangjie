package org.cangnova.cangjie.ide.highlighter

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.analysis.decompiled.psi.file.CjDecompiledFile
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.CangJiePropertyStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.test.CANGJIE_WITH_STDLIB
import org.cangnova.cangjie.test.CangJieLightCodeInsightFixtureTestCase
import org.cangnova.cangjie.test.ProjectDescriptorKind
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 对位 Kotlin `CompiledFilesHighlightingTest` 的产品插件级验证。
 *
 * 这里必须跑在 `product/idea-plugin`，保证测试沙箱加载真实 `plugin.xml`，
 * 从而覆盖 `.cjo` 文件类型、binary decompiler、builtins provider 与编辑器打开链路。
 */
class CangJieCompiledFilesHighlightingTest : CangJieLightCodeInsightFixtureTestCase() {
    @ProjectDescriptorKind(CANGJIE_WITH_STDLIB)
    fun testStdlibStringResolvesFromConfiguredToolchain() {
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

    @ProjectDescriptorKind(CANGJIE_WITH_STDLIB)
    fun testStdlibCjoIsRegisteredAsBuiltInFileType() {
        val virtualFile = stdlibCjoVirtualFile("std.core.cjo")
        assertTrue(
            virtualFile.extension == "cjo",
            "Builtins test input must stay on a real `.cjo` file. actualPath=${virtualFile.path}, actualExtension=${virtualFile.extension}",
        )
        assertSame(
            "`.cjo` virtual file must be recognized as builtins file type. actual=${virtualFile.fileType::class.qualifiedName}/${virtualFile.fileType.name}",
            CangJieBuiltInFileType,
            virtualFile.fileType,
        )
        assertSame(
            "File type registry must map `.cjo` file name to builtins file type.",
            CangJieBuiltInFileType,
            FileTypeRegistry.getInstance().getFileTypeByFileName(virtualFile.name),
        )
    }

    @ProjectDescriptorKind(CANGJIE_WITH_STDLIB)
    fun testStdlibCjoProvidesDecompiledPsiAndRenderedText() {
        val virtualFile = stdlibCjoVirtualFile("std.core.cjo")
        val psiFile = requireNotNull(PsiManager.getInstance(project).findFile(virtualFile)) {
            "PsiManager should restore `.cjo` PSI for ${virtualFile.path}"
        }
        assertTrue(
            psiFile is CjDecompiledFile,
            "Expected decompiled `.cjo` PSI to be CjDecompiledFile, actual=${psiFile::class.qualifiedName}",
        )
        assertSame(virtualFile, psiFile.virtualFile)
        val decompiledText = requireNotNull(psiFile.text) {
            "Decompiled `.cjo` PSI should expose text for ${virtualFile.path}"
        }
        assertTrue(decompiledText.isNotBlank(), "Decompiled `.cjo` PSI should expose non-empty text")
        assertTrue(
            decompiledText.contains("String") || decompiledText.contains("std.core"),
            "Decompiled `.cjo` PSI should expose stdlib declarations. actual preview=${decompiledText.take(200)}",
        )

        val renderedFromBinary = decompiledText
        val renderedFromPsi = decompiledText
        assertTrue(renderedFromBinary.isNotBlank(), "Binary-file render result should not be blank.")
        assertTrue(renderedFromPsi.isNotBlank(), "Decompiled PSI render result should not be blank.")
        assertTrue(
            renderedFromBinary.contains("String") || renderedFromBinary.contains("std.core"),
            "Binary-file render result should contain stdlib declarations. actual preview=${renderedFromBinary.take(200)}",
        )
        assertTrue(
            renderedFromPsi.contains("String") || renderedFromPsi.contains("std.core"),
            "Decompiled PSI render result should contain stdlib declarations. actual preview=${renderedFromPsi.take(200)}",
        )
    }

    @ProjectDescriptorKind(CANGJIE_WITH_STDLIB)
    fun testSmallStdlibCjoOpensInEditorWithDecompiledDocument() {
        val virtualFile = stdlibCjoVirtualFile("std.objectpool.cjo")
        val psiFile = requireNotNull(PsiManager.getInstance(project).findFile(virtualFile)) {
            "PsiManager should restore `.cjo` PSI for ${virtualFile.path}"
        }

        myFixture.openFileInEditor(virtualFile)
        assertSame(virtualFile, myFixture.file.virtualFile)
        assertTrue(
            myFixture.file is CjFile,
            "Editor should open `.cjo` as CangJie PSI file. actual=${myFixture.file::class.qualifiedName}",
        )

        val document = requireNotNull(FileDocumentManager.getInstance().getDocument(virtualFile)) {
            "Opening `.cjo` file should create editor document for ${virtualFile.path}"
        }
        val editorPsi = requireNotNull(PsiDocumentManager.getInstance(project).getPsiFile(document)) {
            "Editor document for `.cjo` should map back to PSI."
        }
        assertSame(virtualFile, editorPsi.virtualFile)
        assertTrue(
            document.text.isNotBlank(),
            "Opened `.cjo` document should expose non-empty decompiled text.",
        )
        assertTrue(
            document.text.contains("ObjectPool") || document.text.contains("std.objectpool"),
            "Opened `.cjo` document should render package declarations. actual preview=${document.text.take(200)}",
        )
        assertTrue(
            psiFile.text.contains("ObjectPool") || psiFile.text.contains("std.objectpool"),
            "Decompiler PSI for opened `.cjo` should expose package declarations. actual preview=${psiFile.text.take(200)}",
        )
    }

    @ProjectDescriptorKind(CANGJIE_WITH_STDLIB)
    fun testLargeStdlibCjoCanOpenInEditor() {
        val virtualFile = stdlibCjoVirtualFile("std.core.cjo")

        myFixture.openFileInEditor(virtualFile)

        assertSame(virtualFile, myFixture.file.virtualFile)
        val document = requireNotNull(FileDocumentManager.getInstance().getDocument(virtualFile)) {
            "Opening large `.cjo` file should create editor document for ${virtualFile.path}"
        }
        assertTrue(document.text.isNotBlank(), "Large `.cjo` document should expose non-empty text.")
        assertTrue(
            document.text.contains("String") || document.text.contains("std.core"),
            "Large `.cjo` document should render stdlib declarations. actual preview=${document.text.take(200)}",
        )
    }

    @ProjectDescriptorKind(CANGJIE_WITH_STDLIB)
    fun testStdlibCjoPropertyCarriesCompiledTypeReferenceStub() {
        val virtualFile = stdlibCjoVirtualFile("std.core.cjo")
        val psiFile = requireNotNull(PsiManager.getInstance(project).findFile(virtualFile)) {
            "PsiManager should restore `.cjo` PSI for ${virtualFile.path}"
        } as CjDecompiledFile

        val rootStub = psiFile.calcStubTree().root
        val propertyStubs = collectPropertyStubs(rootStub)
        val propertyWithMissingTypeRef = propertyStubs.firstOrNull { propertyStub ->
            propertyStub.childrenStubs.none { child -> child.stubType == CjStubElementTypes.TYPE_REFERENCE }
        }
        assertTrue(
            propertyStubs.isNotEmpty(),
            "Expected at least one property stub inside std.core.cjo compiled stub tree.",
        )
        assertTrue(
            propertyWithMissingTypeRef == null,
            buildString {
                appendLine("Compiled property stub should carry TYPE_REFERENCE child.")
                appendLine("propertyCount=${propertyStubs.size}")
                appendLine("missingProperty=${propertyWithMissingTypeRef?.name}")
                appendLine("missingChildren=${propertyWithMissingTypeRef?.childrenStubs?.map(::renderStubDebugName)}")
                appendLine("rootChildren=${rootStub.childrenStubs.map(::renderStubDebugName)}")
            },
        )
    }

    private fun renderStubDebugName(stub: StubElement<*>): String = "${stub::class.simpleName}:${stub.stubType}"

    private fun collectPropertyStubs(root: StubElement<*>): List<CangJiePropertyStub> {
        val result = mutableListOf<CangJiePropertyStub>()

        fun visit(stub: StubElement<*>) {
            if (stub is CangJiePropertyStub) {
                result += stub
            }
            stub.childrenStubs.forEach(::visit)
        }

        visit(root)
        return result
    }

    private fun stdlibCjoVirtualFile(fileName: String) = run {
        val sdkHome = requireNotNull(CjProjectSdkConfig.getInstance(project).getProjectSdk()?.homePath) {
            "Project descriptor should configure a CangJie test SDK"
        }
        val cjoPath = sdkHome.resolve("modules").resolve("windows_x86_64_llvm").resolve("std").resolve(fileName)
        requireNotNull(VirtualFileManager.getInstance().findFileByNioPath(cjoPath)) {
            "Cannot locate builtins file at $cjoPath"
        }
    }
}
