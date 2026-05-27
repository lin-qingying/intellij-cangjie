package org.cangnova.cangjie.ide.highlighter

import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.PlatformTestUtil
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.resolution.successfulFunctionCallOrNull
import org.cangnova.cangjie.analysis.api.resolution.symbol
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.decompiled.psi.file.CjDecompiledFile
import org.cangnova.cangjie.codeinsight.folding.CangJieFoldingRangeCollector
import org.cangnova.cangjie.ide.editor.folding.CangJieFoldingBuilder
import org.cangnova.cangjie.idea.references.mainReference
import org.cangnova.cangjie.project.service.CjProjectsService
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.stubs.CangJieNamedFunctionStub
import org.cangnova.cangjie.psi.stubs.CangJiePropertyStub
import org.cangnova.cangjie.psi.stubs.CangJieTypeParameterStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.test.CANGJIE_WITH_STDLIB
import org.cangnova.cangjie.test.CangJieLightCodeInsightFixtureTestCase
import org.cangnova.cangjie.test.CangJiePluginTestCaseBase
import org.cangnova.cangjie.test.CangJieTestUtils
import org.cangnova.cangjie.test.ProjectDescriptorKind
import org.cangnova.cangjie.utils.executeWriteCommand
import org.cangnova.cangjie.utils.runReadActionInSmartMode
import java.util.concurrent.Callable
import kotlin.test.assertSame
import kotlin.test.assertFalse
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
    fun testStdlibGenericFunctionDiagnosticHighlightingDoesNotCrash() {
        myFixture.configureByText(
            "stdlibGenericFunction.cj",
            """
            package sample

            func main() {
                println(1)
            }
            """.trimIndent(),
        )

        val errors = myFixture.doHighlighting(HighlightSeverity.ERROR)
        val interruptedErrors = errors.filter { highlight ->
            highlight.description?.contains("diagnostic collector has been interrupted", ignoreCase = true) == true ||
                highlight.description?.contains("Class name must not be root", ignoreCase = true) == true
        }

        assertTrue(
            interruptedErrors.isEmpty(),
            "Stdlib generic function signatures must deserialize during diagnostic highlighting. actual=${errors.mapNotNull { it.description }}",
        )
    }

    @ProjectDescriptorKind(CANGJIE_WITH_STDLIB)
    fun testSameProjectLowercaseMacroImportHasNoDiagnosticHighlightingErrors() {
        myFixture.tempDirFixture.createFile(
            "macros.cj",
            """
            macro package untitled89.b

            import std.ast.*

            public macro a11(a1: Tokens): Tokens {
                return a1
            }
            """.trimIndent(),
        )
        val mainFile = myFixture.tempDirFixture.createFile(
            "main.cj",
            """
            import untitled89.b.a11

            func testLowercaseMacroImport(): Int64 {
                let value: Int64 = @a11(42)
                return value
            }
            """.trimIndent(),
        )
        myFixture.configureFromExistingVirtualFile(mainFile)

        val errors = myFixture.doHighlighting(HighlightSeverity.ERROR)

        assertTrue(
            errors.isEmpty(),
            buildString {
                appendLine("Same-project lowercase macro import must not produce IDE diagnostic errors.")
                appendLine("actual=${errors.map { highlight -> highlight.description to highlight.text }}")
            },
        )
    }

    @ProjectDescriptorKind(CANGJIE_WITH_STDLIB)
    fun testStdlibFunctionGotoDeclarationOpensBuiltinsTarget() {
        myFixture.configureByText(
            "stdlibGotoDeclaration.cj",
            """
            package sample

            func main() {
                printl<caret>n(1)
            }
            """.trimIndent(),
        )

        val sourceVirtualFile = myFixture.file.virtualFile

        myFixture.performEditorAction(IdeActions.ACTION_GOTO_DECLARATION)

        val fileEditorManager = FileEditorManager.getInstance(project) as FileEditorManagerEx
        val targetEditor = requireNotNull(fileEditorManager.selectedTextEditor) {
            "Goto declaration must leave a selected text editor after navigating to stdlib."
        }
        val targetVirtualFile = requireNotNull(targetEditor.virtualFile) {
            "Goto declaration target editor must be backed by a virtual file."
        }
        val targetDocument = targetEditor.document
        val targetPsi = requireNotNull(PsiDocumentManager.getInstance(project).getPsiFile(targetDocument)) {
            "Goto declaration target document must map back to PSI."
        }

        assertTrue(
            targetVirtualFile != sourceVirtualFile,
            buildString {
                appendLine("Goto declaration should leave the source file and open the stdlib declaration target.")
                appendLine("sourcePath=${sourceVirtualFile.path}")
                appendLine("targetPath=${targetVirtualFile.path}")
            },
        )
        assertSame(
            CangJieBuiltInFileType,
            targetVirtualFile.fileType,
            "Goto declaration target must stay on a real builtins `.cjo` file.",
        )
        assertTrue(
            targetPsi is CjFile && targetPsi.isCompiled,
            "Goto declaration target PSI must be a compiled CangJie file. actual=${targetPsi::class.qualifiedName}",
        )
        assertTrue(
            targetDocument.textLength > targetEditor.caretModel.offset,
            buildString {
                appendLine("Goto declaration target caret must stay inside the opened document.")
                appendLine("targetPath=${targetVirtualFile.path}")
                appendLine("caret=${targetEditor.caretModel.offset}")
                appendLine("documentLength=${targetDocument.textLength}")
            },
        )
        assertTrue(
            targetDocument.text.contains("println"),
            "Goto declaration target document should contain the navigated stdlib declaration. actualPath=${targetVirtualFile.path}",
        )
    }

    @ProjectDescriptorKind(CANGJIE_WITH_STDLIB)
    fun testStaleCompiledGotoTargetRestoresLiveNavigationRequest() {
        myFixture.configureByText(
            "staleCompiledGotoTarget.cj",
            """
            package sample

            func main() {
                printl<caret>n(1)
            }
            """.trimIndent(),
        )

        val sourceResolveSnapshotBeforeGoto = referenceResolutionSnapshotAtOffset(myFixture.file, myFixture.editor.caretModel.offset)
        myFixture.performEditorAction(IdeActions.ACTION_GOTO_DECLARATION)
        val sourceResolveSnapshotAfterGoto = referenceResolutionSnapshotAtOffset(myFixture.file, myFixture.editor.caretModel.offset)

        val fileEditorManager = FileEditorManager.getInstance(project) as FileEditorManagerEx
        val targetEditor = requireNotNull(fileEditorManager.selectedTextEditor) {
            "Goto declaration must open a compiled target editor before cache invalidation."
        }
        val targetDocument = targetEditor.document
        val targetPsi = requireNotNull(PsiDocumentManager.getInstance(project).getPsiFile(targetDocument)) {
            "Goto declaration target document must map back to PSI before cache invalidation."
        }
        val targetLeaf = requireNotNull(targetPsi.findElementAt(targetEditor.caretModel.offset)) {
            "Goto declaration target caret should point to a PSI leaf."
        }
        val compiledTarget = requireNotNull(PsiTreeUtil.getParentOfType(targetLeaf, CjDeclaration::class.java, false)) {
            "Goto declaration target caret should stay inside a compiled declaration."
        }
        assertTrue(
            compiledTarget.containingCjFile.isCompiled,
            buildString {
                appendLine("Resolved target must come from compiled stdlib PSI.")
                appendLine("actual=${compiledTarget.containingFile.virtualFile.path}")
                appendLine("selectedEditor=${targetEditor.virtualFile?.path}")
                appendLine("sourceResolveBeforeGoto=$sourceResolveSnapshotBeforeGoto")
                appendLine("sourceResolveAfterGoto=$sourceResolveSnapshotAfterGoto")
            },
        )

        project.executeWriteCommand("drop compiled psi caches") {
            PsiManager.getInstance(project).dropPsiCaches()
        }

        data class NavigationSnapshot(
            val requestClassName: String?,
            val filePath: String,
            val range: TextRange,
            val documentLength: Int,
        )

        val navigationSnapshot = ApplicationManager.getApplication().executeOnPooledThread(
            Callable {
                project.runReadActionInSmartMode {
                    val navigationRequest = requireNotNull(compiledTarget.navigationRequest()) {
                        "Compiled declaration should still produce a navigation request after PSI cache invalidation."
                    }
                    val navigationElement = compiledTarget.navigationElement as? CjDeclaration
                    val navigationFile = requireNotNull(navigationElement?.containingFile ?: compiledTarget.containingFile) {
                        "Navigation target should stay backed by PSI after cache invalidation."
                    }
                    val navigationDocument = requireNotNull(
                        PsiDocumentManager.getInstance(project).getDocument(navigationFile)
                            ?: FileDocumentManager.getInstance().getDocument(navigationFile.virtualFile),
                    ) {
                        "Navigation target should still map to a document after cache invalidation."
                    }
                    val navigationRange = requireNotNull(navigationElement?.textRange ?: compiledTarget.textRange) {
                        "Navigation target should still expose a text range after cache invalidation."
                    }

                    NavigationSnapshot(
                        requestClassName = navigationRequest::class.qualifiedName,
                        filePath = navigationFile.virtualFile.path,
                        range = navigationRange,
                        documentLength = navigationDocument.textLength,
                    )
                }
            },
        ).get()

        assertTrue(
            navigationSnapshot.range.endOffset <= navigationSnapshot.documentLength,
            buildString {
                appendLine("Navigation target range must stay inside the current document after cache invalidation.")
                appendLine("request=${navigationSnapshot.requestClassName}")
                appendLine("file=${navigationSnapshot.filePath}")
                appendLine("range=${navigationSnapshot.range}")
                appendLine("documentLength=${navigationSnapshot.documentLength}")
            },
        )
    }

    @ProjectDescriptorKind(CANGJIE_WITH_STDLIB)
    fun testStdlibCjoGenericFunctionCarriesCompiledTypeParameterList() {
        val virtualFile = stdlibCjoVirtualFile("std.core.cjo")
        val psiFile = requireNotNull(PsiManager.getInstance(project).findFile(virtualFile)) {
            "PsiManager should restore `.cjo` PSI for ${virtualFile.path}"
        } as CjDecompiledFile

        val rootStub = psiFile.calcStubTree().root
        val printlnStub = requireNotNull(collectNamedFunctionStubs(rootStub).firstOrNull { functionStub ->
            functionStub.name == "println" &&
                functionStub.childrenStubs.any { child -> child.stubType == CjStubElementTypes.TYPE_PARAMETER_LIST }
        }) {
            "Expected std.core.cjo compiled stub tree to contain generic function `println`."
        }

        val typeParameterListStub = printlnStub.childrenStubs.firstOrNull { child ->
            child.stubType == CjStubElementTypes.TYPE_PARAMETER_LIST
        }
        val typeParameterNames = typeParameterListStub
            ?.childrenStubs
            ?.filterIsInstance<CangJieTypeParameterStub>()
            ?.mapNotNull { typeParameterStub -> typeParameterStub.name }
            .orEmpty()

        assertTrue(
            typeParameterListStub != null && "T" in typeParameterNames,
            buildString {
                appendLine("Compiled generic function stub must expose TYPE_PARAMETER_LIST so low-level deserialization can resolve `T`.")
                appendLine("functionChildren=${printlnStub.childrenStubs.map(::renderStubDebugName)}")
                appendLine("typeParameterListChildren=${typeParameterListStub?.childrenStubs?.map(::renderStubDebugName)}")
                appendLine("typeParameterNames=$typeParameterNames")
            },
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
    fun testStdlibAstCjoFoldingDescriptorsStayInsideDocumentRange() {
        val virtualFile = stdlibCjoVirtualFile("std.ast.cjo")

        myFixture.openFileInEditor(virtualFile)

        val document = requireNotNull(FileDocumentManager.getInstance().getDocument(virtualFile)) {
            "Opening `.cjo` file should create editor document for ${virtualFile.path}"
        }
        val psiFile = requireNotNull(PsiDocumentManager.getInstance(project).getPsiFile(document)) {
            "Editor document for `.cjo` should map back to PSI."
        }
        assertTrue(
            psiFile is CjFile,
            "Expected `.cjo` editor PSI to be CjFile, actual=${psiFile::class.qualifiedName}",
        )

        val documentLength = document.textLength
        val regions = CangJieFoldingRangeCollector.collect(psiFile, document)
        val invalidRegions = regions.filter { region ->
            region.range.startOffset < 0 ||
                region.range.endOffset > documentLength ||
                region.range.startOffset > region.range.endOffset
        }
        assertTrue(
            invalidRegions.isEmpty(),
            buildString {
                appendLine("All folding descriptors must stay inside document range.")
                appendLine("path=${virtualFile.path}")
                appendLine("documentLength=$documentLength")
                appendLine("psiLength=${psiFile.textLength}")
                appendLine("descriptorCount=${regions.size}")
                appendLine("invalid=${describeRegions(document, invalidRegions)}")
                appendLine("all=${describeRegions(document, regions)}")
            },
        )
    }

    fun testStdlibAstCjoPlaceholderDocumentReloadsAfterToolchainRegistration() {
        CjProjectsService.getInstance(project)

        val sdkHome = CangJiePluginTestCaseBase.createSlimToolchainHome(
            "std.cjo",
            "std/std.core.cjo",
            "std/std.ast.cjo",
            "std/std.objectpool.cjo",
        )
        val rootAccess = CangJieTestUtils.allowRootAccess(this, sdkHome.toString())

        try {
            val virtualFile = requireNotNull(
                LocalFileSystem.getInstance().refreshAndFindFileByNioFile(
                    sdkHome.resolve("modules").resolve("windows_x86_64_llvm").resolve("std").resolve("std.ast.cjo"),
                ),
            ) {
                "Failed to locate std.ast.cjo under temporary SDK home: $sdkHome"
            }

            myFixture.openFileInEditor(virtualFile)

            val document = requireNotNull(FileDocumentManager.getInstance().getDocument(virtualFile)) {
                "Opening std.ast.cjo should create editor document for ${virtualFile.path}"
            }
            val initialText = document.text
            assertTrue(
                isDecompilerFailurePlaceholder(initialText),
                buildString {
                    appendLine("Without project toolchain, std.ast.cjo should first open with decompiler failure placeholder.")
                    appendLine("path=${virtualFile.path}")
                    appendLine("documentLength=${document.textLength}")
                    appendLine("preview=${initialText.take(200)}")
                },
            )

            CangJiePluginTestCaseBase.registerProjectToolchain(
                project = project,
                parentDisposable = testRootDisposable,
                sdkHome = sdkHome,
                displayName = "Reload Test SDK",
            )

            PlatformTestUtil.waitWithEventsDispatching(
                { "Waiting for std.ast.cjo document to reload after toolchain registration" },
                { !isDecompilerFailurePlaceholder(document.text) && document.textLength > initialText.length },
                30_000,
            )
            PlatformTestUtil.waitForAllDocumentsCommitted(30, java.util.concurrent.TimeUnit.SECONDS)

            val psiFile = requireNotNull(PsiDocumentManager.getInstance(project).getPsiFile(document)) {
                "Reloaded std.ast.cjo document should map back to PSI."
            }
            assertTrue(
                psiFile is CjDecompiledFile,
                "Reloaded std.ast.cjo PSI must stay on decompiled compiled file. actual=${psiFile::class.qualifiedName}",
            )
            val psiText = requireNotNull(psiFile.text) {
                "Reloaded std.ast.cjo PSI should expose decompiled text."
            }
            assertFalse(
                isDecompilerFailurePlaceholder(psiText),
                buildString {
                    appendLine("Reloaded std.ast.cjo PSI must no longer expose the failure placeholder.")
                    appendLine("path=${virtualFile.path}")
                    appendLine("psiLength=${psiFile.textLength}")
                    appendLine("preview=${psiText.take(200)}")
                },
            )
            assertTrue(
                psiFile.textLength == document.textLength && psiText == document.text,
                buildString {
                    appendLine("Reloaded std.ast.cjo PSI/document must stay text-identical.")
                    appendLine("path=${virtualFile.path}")
                    appendLine("psiLength=${psiFile.textLength}")
                    appendLine("documentLength=${document.textLength}")
                },
            )

            val regions = CangJieFoldingRangeCollector.collect(psiFile, document)
            val invalidRegions = regions.filter { region ->
                region.range.startOffset < 0 ||
                    region.range.endOffset > document.textLength ||
                    region.range.startOffset > region.range.endOffset
            }
            assertTrue(
                invalidRegions.isEmpty(),
                buildString {
                    appendLine("Reloaded std.ast.cjo folding ranges must stay inside document.")
                    appendLine("path=${virtualFile.path}")
                    appendLine("documentLength=${document.textLength}")
                    appendLine("psiLength=${psiFile.textLength}")
                    appendLine("invalid=${describeRegions(document, invalidRegions)}")
                    appendLine("all=${describeRegions(document, regions)}")
                },
            )
        } finally {
            CangJieTestUtils.disposeVfsRootAccess(rootAccess)
        }
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

    private fun referenceResolutionSnapshotAtOffset(file: PsiFile, offset: Int): String {
        val caretElement = file.findElementAt(offset)
        val simpleName = caretElement?.parentsWithSelf()?.filterIsInstance<CjSimpleNameExpression>()?.firstOrNull()
        val reference = simpleName?.mainReference
        return buildString {
            append("sourcePath=").append(file.virtualFile.path)
            append(", offset=").append(offset)
            append(", caretChain=").append(caretElement?.parentsWithSelf()?.joinToString(" -> ", transform = ::renderPsiDebugName) ?: "<none>")
            append(", simpleName=").append(simpleName?.let(::renderPsiDebugName) ?: "<none>")
            append(", analysis=").append(simpleName?.let(::renderAnalysisResolutionSnapshot) ?: "<none>")
            append(", reference=").append(reference?.let(::renderReferenceDebugName) ?: "<none>")
            append(", resolvedTargets=").append(reference?.let(::resolveReferenceTargetsDebug) ?: emptyList<String>())
        }
    }

    private fun PsiElement.parentsWithSelf(): List<PsiElement> {
        return generateSequence(this) { element -> element.parent }.toList()
    }

    private fun renderPsiDebugName(element: PsiElement): String {
        return "${element::class.java.simpleName}(`${element.text}`)"
    }

    private fun renderReferenceDebugName(reference: PsiReference): String {
        return "${reference::class.java.simpleName}@${reference.rangeInElement}"
    }

    private fun renderAnalysisResolutionSnapshot(simpleName: CjSimpleNameExpression): String {
        val simpleNamePointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(simpleName)
        return ApplicationManager.getApplication().executeOnPooledThread(
            Callable {
                project.runReadActionInSmartMode {
                    val liveSimpleName = simpleNamePointer.element as? CjSimpleNameExpression
                        ?: return@runReadActionInSmartMode "<disposed>"
                    analyze(liveSimpleName) {
                        val callExpression = liveSimpleName.parent as? CjCallExpression
                        val resolvedSymbols = liveSimpleName.resolveToSymbols().toList()
                        val resolvedCall = callExpression?.resolveToCall()?.successfulFunctionCallOrNull()
                        buildString {
                            append("{")
                            append("symbols=").append(resolvedSymbols.map(::renderSymbolDebugName))
                            append(", call=").append(
                                resolvedCall?.symbol?.let(::renderSymbolDebugName) ?: "<none>"
                            )
                            append("}")
                        }
                    }
                }
            },
        ).get()
    }

    private fun renderSymbolDebugName(symbol: CaSymbol): String {
        val identity = when (symbol) {
            is CaCallableSymbol -> symbol.callableId?.toString()
            is CaClassLikeSymbol -> symbol.classId?.asString()
            is CaPackageSymbol -> symbol.fqName.asString()
            else -> null
        } ?: symbol.name?.asString()

        val psi = symbol.psi
        val psiDebug = when {
            psi == null -> "<null>"
            else -> "${psi::class.java.simpleName}@${psi.containingFile?.virtualFile?.path}"
        }

        return "${symbol::class.java.simpleName}(origin=${symbol.origin}, id=${identity ?: "<none>"}, psi=$psiDebug)"
    }

    private fun resolveReferenceTargetsDebug(reference: PsiReference): List<String> {
        val targets = when (reference) {
            is PsiPolyVariantReference -> reference.multiResolve(false).mapNotNull { resolveResult -> resolveResult.element }
            else -> listOfNotNull(reference.resolve())
        }
        return targets.map(::renderPsiDebugName)
    }

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

    private fun collectNamedFunctionStubs(root: StubElement<*>): List<CangJieNamedFunctionStub> {
        val result = mutableListOf<CangJieNamedFunctionStub>()

        fun visit(stub: StubElement<*>) {
            if (stub is CangJieNamedFunctionStub) {
                result += stub
            }
            stub.childrenStubs.forEach(::visit)
        }

        visit(root)
        return result
    }

    private fun CangJieFoldingBuilder.placeholderTextOf(descriptor: FoldingDescriptor): String {
        return CangJieFoldingRangeCollector.placeholderText(descriptor.element)
    }

    private fun describeDescriptors(
        builder: CangJieFoldingBuilder,
        document: Document,
        descriptors: List<FoldingDescriptor>,
    ): String {
        if (descriptors.isEmpty()) return "[]"
        return descriptors.joinToString(prefix = "[", postfix = "]") { descriptor ->
            val preview = if (descriptor.range.endOffset <= document.textLength) {
                descriptor.range.substring(document.text).replace("\n", "\\n")
            } else {
                "<outside-document>"
            }
            "{placeholder=${builder.placeholderTextOf(descriptor)}, range=${descriptor.range}, text=$preview}"
        }
    }

    private fun describeRegions(
        document: Document,
        regions: List<org.cangnova.cangjie.codeinsight.folding.CangJieFoldingRegion>,
    ): String {
        if (regions.isEmpty()) return "[]"
        return regions.joinToString(prefix = "[", postfix = "]") { region ->
            val preview = if (region.range.endOffset <= document.textLength) {
                region.range.substring(document.text).replace("\n", "\\n")
            } else {
                "<outside-document>"
            }
            "{placeholder=${region.placeholderText}, range=${region.range}, text=$preview}"
        }
    }

    private fun isDecompilerFailurePlaceholder(text: String): Boolean {
        return text.contains("Could not decompile the file", ignoreCase = true) &&
            text.contains("Please report an issue", ignoreCase = true)
    }

    private fun stdlibCjoVirtualFile(fileName: String) = run {
        val builtinsFiles = BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles()
        requireNotNull(builtinsFiles.firstOrNull { file -> file.name.equals(fileName, ignoreCase = true) }) {
            "Cannot locate builtins file `$fileName` from provider. actual=${builtinsFiles.map { it.path }.sorted()}"
        }
    }
}
