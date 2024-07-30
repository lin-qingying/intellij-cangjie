package com.linqingying.lsp.api.customization

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.annotations.RequiresWriteLock
import com.linqingying.lsp.api.LspServer
import com.linqingying.lsp.api.customization.requests.util.applyTextEdits
import com.linqingying.lsp.api.customization.requests.util.getOffsetInDocument
import org.eclipse.lsp4j.*
import org.jetbrains.annotations.ApiStatus


/**
 * [IntentionAction] that knows how to apply [CodeAction] received from the LSP server.
 *
 * #### Implementation note
 * IntelliJ Platform API assumes that the quick fixes for diagnostics are created right at the moment of the code highlighting.
 * For performance reasons, special quick fix stubs are created at that
 * moment. Those stubs do not contain [CodeAction] objects. As soon as the IntelliJ Platform calls [IntentionAction.getText] or
 * [IntentionAction.isAvailable] on those stubs, the stubs ask the LSP server to calculate real [CodeAction] objects and, once received,
 * create [LspIntentionAction] objects. After that, those quick fix stubs delegate [getText], [isAvailable], and [invoke] calls to these
 * [LspIntentionAction] objects.
 */

open class LspIntentionAction(
    protected val lspServer: com.linqingying.lsp.api.LspServer, protected val codeAction: CodeAction
) : IntentionAction {
    private var uriToDocumentMapInitialized: Boolean = false
    private var uriToDocumentMap: Map<String, Document>? = null

    // `getFamilyName()` is not delegated to this class. The wrapper class returns empty string. This function is never called.
    final override fun getFamilyName(): String = ""

    // `startInWriteAction()` is not delegated to this class. The wrapper class returns `false`. This function is never called.
    final override fun startInWriteAction(): Boolean = false

    private fun executeLspCommandIfPossible(
        commandsSupport: LspCommandsSupport?, command: Command?, contextFile: VirtualFile?
    ) { /* compiled code */
        if (commandsSupport != null && command != null && contextFile != null) {
            commandsSupport.executeCommand(this.lspServer, contextFile, command)
        }
    }

    override fun getText(): String = codeAction.title

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        codeAction.disabled?.let { return false }

        if (!uriToDocumentMapInitialized) {
            uriToDocumentMap = if (codeAction.edit != null) getUriToDocumentMap(codeAction.edit) else emptyMap()
            uriToDocumentMapInitialized = true
        }

        return uriToDocumentMap != null
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val uriToDocumentMap = this.uriToDocumentMap ?: return
        val files = uriToDocumentMap.map { FileDocumentManager.getInstance().getFile(it.value) }
        if (!FileModificationService.getInstance().prepareVirtualFilesForWrite(lspServer.project, files)) return

        codeAction.edit?.let { edit -> WriteAction.run<Throwable> { applyWorkspaceEdit(edit, uriToDocumentMap) } }

        applyCommand(lspServer.descriptor.lspCommandsSupport, codeAction.command, file.virtualFile)
    }

    private fun applyCommand(commandsSupport: LspCommandsSupport?, command: Command?, contextFile: VirtualFile?) {
        if (commandsSupport != null && command != null && contextFile != null) {
            commandsSupport.executeCommand(lspServer, contextFile, command)
        }
    }

    /**
     * @param uriToDocumentMap it's guaranteed that it contains `Document` objects for URIs from this [workspaceEdit]
     */
    @RequiresWriteLock
    protected open fun applyWorkspaceEdit(workspaceEdit: WorkspaceEdit, uriToDocumentMap: Map<String, Document>) {
        workspaceEdit.changes?.let { changes ->
            changes.entries.forEach { entry ->
                val document = uriToDocumentMap[entry.key]!!
                for (textEdit in entry.value) {
                    if (!applyTextEdit(document, textEdit)) return@applyWorkspaceEdit
                }
            }
        }

        workspaceEdit.documentChanges?.let { documentChanges ->
            if (documentChanges.any { it.isRight }) {
                // TODO support ResourceOperations
                return@applyWorkspaceEdit
            }

            documentChanges.forEach { editOrResourceOperation ->
                editOrResourceOperation.left?.let { textDocumentEdit ->
                    val document = uriToDocumentMap[textDocumentEdit.textDocument.uri]!!
                    for (textEdit in textDocumentEdit.edits) {
                        if (!applyTextEdit(document, textEdit)) return@applyWorkspaceEdit
                    }
                }

                editOrResourceOperation.right?.let { resourceOperation ->
                    // TODO implement
                    when (resourceOperation) {
                        is CreateFile -> {}
                        is DeleteFile -> {}
                        is RenameFile -> {}
                    }
                }
            }
        }
    }

    private fun getUriToDocumentMap(edit: WorkspaceEdit): Map<String, Document>? {
        val result = mutableMapOf<String, Document>()

        edit.changes?.let { changes ->
            changes.keys.forEach { documentUri ->
                val document = getDocument(documentUri) ?: return null
                result[documentUri] = document
            }
        }

        edit.documentChanges?.let { documentChanges ->
            documentChanges.forEach { editOrResourceOperation ->
                editOrResourceOperation.left?.let { textDocumentEdit ->
                    val documentUri = textDocumentEdit.textDocument.uri
                    val version: Int? = textDocumentEdit.textDocument.version
                    val document = getDocument(documentUri, version ?: -1) ?: return null
                    result[documentUri] = document
                }
            }
        }

        return result
    }

    private fun getDocument(documentUri: String, version: Int = -1): Document? {
        val file = lspServer.descriptor.findFileByUri(documentUri)
        if (file == null) {
            logger<LspIntentionAction>().warn("File not found: $documentUri")
            return null
        }

        if (!ProjectFileIndex.getInstance(lspServer.project).isInContent(file)) {
            logger<LspIntentionAction>().warn("File is not within the project content: $documentUri")
            return null
        }

        val document = FileDocumentManager.getInstance().getDocument(file)
        if (document == null) {
            logger<LspIntentionAction>().warn("Document not found for file: $file")
            return null
        }

        val documentVersion = lspServer.requestExecutor.getDocumentVersion(document)
        if (version != -1 && documentVersion != version) {
            logger<LspIntentionAction>().info(
                "Ignoring CodeAction for document version $version (${file.name}); " + "current document version: $documentVersion"
            )
            return null
        }

        return document
    }

    /**
     * @return `false` if the `textEdit` can't be applied to the `document` because `textEdit.textRange` is outside the `document` text textRange.
     */
    protected fun applyTextEdit(document: Document, textEdit: TextEdit): Boolean {
        val startOffset = getOffsetInDocument(document, textEdit.range.start)
        val endOffset = getOffsetInDocument(document, textEdit.range.end)
        if (startOffset == null || endOffset == null) {
            logger<LspIntentionAction>().warn(
                "Ignoring TextEdit, its text textRange is outside the document text textRange.\n" + "document.lineCount = ${document.lineCount}, document.textLength = ${document.textLength}, textRange: ${textEdit.range}"
            )
            return false
        }

        document.replaceString(startOffset, endOffset, textEdit.newText)
        return true
    }

    private fun getDocumentByUri(
        documentUri: String, version: Int = -1
    ): Document? {

        val file = lspServer.descriptor.findFileByUri(documentUri)
        if (file == null) {
            Logger.getInstance(LspIntentionAction::class.java).warn("File not found: $documentUri")
            return null
        } else if (!ProjectFileIndex.getInstance(this.lspServer.project).isInContent(file)) {
            Logger.getInstance(LspIntentionAction::class.java)
                .warn("File is not within the project content: $documentUri")
            return null
        } else {
            val document = FileDocumentManager.getInstance().getDocument(file)
            if (document == null) {
                Logger.getInstance(LspIntentionAction::class.java).warn("Document not found for file: $file")
                return null
            } else {
                val dv = this.lspServer.requestExecutor.getDocumentVersion(document)
                if (version != -1 && dv != version) {
                    Logger.getInstance(LspIntentionAction::class.java)
                        .info("Ignoring CodeAction for document version $version (${file.name}); current document version: $dv")
                    return null
                } else {
                    return document
                }
            }
        }
    }

    private fun getDocumentsFromEdit(edit: WorkspaceEdit): Map<String, Document>? {

        val map = LinkedHashMap<String, Document>()
        edit.changes?.let { textMap ->
            textMap.keys.forEach { key ->
                val document = getDocument(key, 0) ?: return null
                map[key] = document
            }
        }

        val map2 = LinkedHashSet<String>()
        edit.documentChanges?.forEach { either ->
            val textDocumentEdit = either.left
            val uri = textDocumentEdit.textDocument.uri
            if (!map2.contains(uri)) {
                val version = textDocumentEdit.textDocument.version ?: -1
                val document = getDocumentByUri(uri, version) ?: return null
                map[uri] = document
            }

            val createFile = either.right as? CreateFile
            if (createFile != null) {

                if (createFile.uri != null) {
                    map2.add(createFile.uri)
                }
            }
        }

        return map

    }

    private fun applyEditsFromPhysicalToNonPhysicalDocument(
        nonPhysicalDocument: Document, physicalDocument: Document
    ) {

        this.uriToDocumentMap?.let {

            this.codeAction.edit?.let { workspaceEdit ->
                workspaceEdit.changes?.let { map ->
                    map.entries.forEach { entry ->
                        if (physicalDocument == it[entry.key]) {
                            val edits = entry.value as List<TextEdit>
                            if (!applyTextEdits(nonPhysicalDocument, edits)) {
                                return
                            }
                        }
                    }
                }

                workspaceEdit.documentChanges?.forEach { either ->
                    val textDocumentEdit = either.left
                    if (textDocumentEdit != null && physicalDocument == it[textDocumentEdit.textDocument.uri]) {
                        val edits = textDocumentEdit.edits
                        if (!applyTextEdits(nonPhysicalDocument, edits)) {
                            return
                        }
                    }
                }
            }
        }
    }

    override fun generatePreview(project: Project, editor: Editor, nonPhysicalFile: PsiFile): IntentionPreviewInfo {
        val uriToDocumentMap = this.uriToDocumentMap ?: return IntentionPreviewInfo.EMPTY
        val physicalFile = nonPhysicalFile.originalFile
        val physicalDocument =
            PsiDocumentManager.getInstance(project).getDocument(physicalFile) ?: return IntentionPreviewInfo.EMPTY
        if (!uriToDocumentMap.containsValue(physicalDocument)) return IntentionPreviewInfo.EMPTY

        invokeForPreview(nonPhysicalFile.viewProvider.document, physicalDocument)
        return IntentionPreviewInfo.DIFF
    }

    private fun invokeForPreview(nonPhysicalDocument: Document, physicalDocument: Document) {
        val uriToDocumentMap = this.uriToDocumentMap ?: return
        val workspaceEdit = codeAction.edit ?: return

        workspaceEdit.changes?.let { changes ->
            changes.entries.forEach { entry ->
                if (physicalDocument != uriToDocumentMap[entry.key]) return@forEach
                for (textEdit in entry.value) {
                    if (!applyTextEdit(nonPhysicalDocument, textEdit)) return@invokeForPreview
                }
            }
        }

        workspaceEdit.documentChanges?.let { documentChanges ->
            documentChanges.forEach { editOrResourceOperation ->
                editOrResourceOperation.left?.let { textDocumentEdit ->
                    if (physicalDocument != uriToDocumentMap[textDocumentEdit.textDocument.uri]) return@forEach
                    for (textEdit in textDocumentEdit.edits) {
                        if (!applyTextEdit(nonPhysicalDocument, textEdit)) return@invokeForPreview
                    }
                }
            }
        }
    }
}
