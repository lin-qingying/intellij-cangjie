/*
 * Copyright 2024 LinQingYing. and contributors.
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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package com.linqingying.lsp.api.customization

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.util.PathUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresWriteLock
import com.linqingying.lsp.api.LspServer
import com.linqingying.lsp.util.applyTextEdits
import org.eclipse.lsp4j.*
import org.jetbrains.annotations.ApiStatus


/**
 * [IntentionAction]，用于应用从 LSP 服务器接收到的 [CodeAction]。
 *
 * #### 实现说明
 * IntelliJ Platform API 假设诊断的快速修复是在代码高亮的瞬间创建的。
 * 出于性能考虑，特殊的快速修复存根会在那个时刻创建。
 * 这些存根不包含 [CodeAction] 对象。只要 IntelliJ Platform 调用 [IntentionAction.getText] 或
 * [IntentionAction.isAvailable]，这些存根就会向 LSP 服务器请求计算真实的 [CodeAction] 对象，并在收到后，
 * 创建 [LspIntentionAction] 对象。之后，这些快速修复存根将 [getText]、[isAvailable] 和 [invoke] 调用委托给这些
 * [LspIntentionAction] 对象。
 *
 * [LspIntentionAction] 对象不仅用于快速修复，还用于其他类型的 [CodeActions][CodeAction]。
 * 在 IntelliJ Platform 术语中，其他类型的 `CodeActions` 作为 [Intention actions](https://www.jetbrains.com/help/idea/intention-actions.html) 处理。
 */

open class LspIntentionAction(protected val lspServer: LspServer, private val initialCodeAction: CodeAction) :
    IntentionAction {
    // 1. 如果 `initialCodeAction` 包含 `edit` 属性，意味着它已经被解析。
    // 2. 如果服务器不支持代码操作解析，意味着 `initialCodeAction` 已经被解析。
    private var resolvedCodeAction: CodeAction? =
        if (initialCodeAction.edit != null ||
            lspServer.initializeResult?.capabilities?.codeActionProvider?.right?.resolveProvider != true
        ) {
            initialCodeAction
        } else null

    private val codeAction: CodeAction
        get() = resolvedCodeAction ?: initialCodeAction

    private var uriToDocumentMapInitialized: Boolean = false
    private var uriToDocumentMap: Map<String, Document>? = null

    // `getFamilyName()` 不委托给这个类。包装类返回空字符串。此函数从未被调用。
    final override fun getFamilyName(): String = ""

    // `startInWriteAction()` 不委托给这个类。包装类返回 `false`。此函数从未被调用。
    final override fun startInWriteAction(): Boolean = false

    override fun getText(): String = codeAction.title

    override fun isAvailable(project: Project, editor: Editor, psiFile: PsiFile): Boolean = isAvailable()

    private fun isAvailable(): Boolean {
        codeAction.disabled?.let { return false }

        resolveCodeAction()

        if (!uriToDocumentMapInitialized) {
            uriToDocumentMap = if (codeAction.edit != null) getUriToDocumentMap(codeAction.edit) else emptyMap()
            uriToDocumentMapInitialized = true
        }

        return uriToDocumentMap != null && (codeAction.edit != null || codeAction.command != null)
    }

    @RequiresBackgroundThread
    private fun resolveCodeAction() {
        if (resolvedCodeAction != null) return

        val resolved = lspServer.sendRequestSync { it.textDocumentService.resolveCodeAction(codeAction) }
        resolvedCodeAction = resolved ?: initialCodeAction
    }

    override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) = invoke(psiFile.virtualFile)

    fun invoke(contextFile: VirtualFile?) {
        val uriToDocumentMap = this.uriToDocumentMap ?: return
        val files = uriToDocumentMap.map { FileDocumentManager.getInstance().getFile(it.value) }
        if (!FileModificationService.getInstance().prepareVirtualFilesForWrite(lspServer.project, files)) return

        if (codeAction.edit?.changes?.isNotEmpty() == true || codeAction.edit?.documentChanges?.isNotEmpty() == true) {
            WriteAction.run<Throwable> { applyWorkspaceEdit(codeAction.edit, uriToDocumentMap) }
        }

        applyCommand(lspServer.descriptor.lspCommandsSupport, codeAction.command, contextFile)
    }

    private fun applyCommand(commandsSupport: LspCommandsSupport?, command: Command?, contextFile: VirtualFile?) {
        if (commandsSupport != null && command != null && contextFile != null) {
            commandsSupport.executeCommand(lspServer, contextFile, command)
        }
    }

    /**
     * @param uriToDocumentMap 保证包含该 [workspaceEdit] 中 URI 的 `Document` 对象
     */
    @RequiresWriteLock
    protected open fun applyWorkspaceEdit(workspaceEdit: WorkspaceEdit, uriToDocumentMap: Map<String, Document>) {
        workspaceEdit.changes?.let { changes ->
            changes.entries.forEach { entry ->
                val document = uriToDocumentMap[entry.key]!!
                if (!applyTextEdits(document, entry.value)) return@applyWorkspaceEdit
            }
        }

        val uriToCreatedDocumentMap = mutableMapOf<String, Document>()

        workspaceEdit.documentChanges?.let { documentChanges ->
            if (documentChanges.any { it.isRight && it.right !is CreateFile }) {
                // TODO 支持 DeleteFile 和 RenameFile
                return@applyWorkspaceEdit
            }

            documentChanges.forEach { editOrResourceOperation ->
                editOrResourceOperation.left?.let { textDocumentEdit ->
                    val document = uriToDocumentMap[textDocumentEdit.textDocument.uri]
                        ?: uriToCreatedDocumentMap[textDocumentEdit.textDocument.uri]
                    if (document == null) {
                        thisLogger().error("No Document for ${textDocumentEdit.textDocument.uri}")
                        return@applyWorkspaceEdit
                    }
                    if (!applyTextEdits(document, textDocumentEdit.edits)) return@applyWorkspaceEdit
                }

                editOrResourceOperation.right?.let { resourceOperation ->
                    when (resourceOperation) {
                        is CreateFile -> createFile(resourceOperation)?.let {
                            uriToCreatedDocumentMap[resourceOperation.uri] = it
                        }

                        is DeleteFile -> {} // TODO 实现
                        is RenameFile -> {} // TODO 实现
                    }
                }
            }
        }
    }

    private fun createFile(createFile: CreateFile): Document? {
        val fileUri = createFile.uri
        var existingDirUri = PathUtil.getParentPath(fileUri)
        var existingDir = lspServer.descriptor.findFileByUri(existingDirUri)
        while (existingDir == null && existingDirUri.isNotEmpty()) {
            existingDirUri = PathUtil.getParentPath(existingDirUri)
            existingDir = lspServer.descriptor.findFileByUri(existingDirUri)
        }

        if (existingDir == null) {
            thisLogger().warn("Ignoring CreateFile(${fileUri}): base directory not found")
            return null
        }

        val relativePath = fileUri.substring(existingDirUri.length + 1)
        val fileName = PathUtil.getFileName(relativePath)
        val relativeParentPath = PathUtil.getParentPath(relativePath)

        val parentDir = when {
            relativeParentPath.isEmpty() -> existingDir
            else -> VfsUtil.createDirectoryIfMissing(existingDir.path + "/" + relativeParentPath)
        }

        if (parentDir == null) {
            thisLogger().warn("Failed to create parent directory for CreateFile(${fileUri})")
            return null
        }

        val createdFile = parentDir.createChildData(this, fileName)
        return FileDocumentManager.getInstance().getDocument(createdFile)
            .also { if (it == null) thisLogger().warn("No Document for created file ${createdFile.path}") }
    }

    private fun getUriToDocumentMap(edit: WorkspaceEdit): Map<String, Document>? {
        val result = mutableMapOf<String, Document>()

        edit.changes?.let { changes ->
            changes.keys.forEach { documentUri ->
                val document = getDocument(documentUri) ?: return null
                result[documentUri] = document
            }
        }

        val urisToCreate = mutableSetOf<String>()

        edit.documentChanges?.let { documentChanges ->
            documentChanges.forEach { editOrResourceOperation ->
                editOrResourceOperation.left?.let { textDocumentEdit ->
                    val documentUri = textDocumentEdit.textDocument.uri
                    if (!urisToCreate.contains(documentUri)) {
                        val version: Int? = textDocumentEdit.textDocument.version
                        val document = getDocument(documentUri, version ?: -1) ?: return null
                        result[documentUri] = document
                    }
                }

                (editOrResourceOperation.right as? CreateFile)?.uri?.let { urisToCreate.add(it) }
            }
        }

        return result
    }

    private fun getDocument(documentUri: String, version: Int = -1): Document? {
        val file = lspServer.descriptor.findFileByUri(documentUri)
        if (file == null) {
            thisLogger().warn("File not found: $documentUri")
            return null
        }

        if (!ProjectFileIndex.getInstance(lspServer.project).isInContent(file)) {
            thisLogger().warn("File is not within the project content: $documentUri")
            return null
        }

        val document = FileDocumentManager.getInstance().getDocument(file)
        if (document == null) {
            thisLogger().warn("Document not found for file: $file")
            return null
        }

        val documentVersion = lspServer.getDocumentVersion(document)
        if (version != -1 && documentVersion != version) {
            thisLogger().info(
                "Ignoring CodeAction for document version $version (${file.name}); " +
                        "current document version: $documentVersion"
            )
            return null
        }

        return document
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
                if (!applyTextEdits(nonPhysicalDocument, entry.value)) return@invokeForPreview
            }
        }

        workspaceEdit.documentChanges?.let { documentChanges ->
            documentChanges.forEach { editOrResourceOperation ->
                editOrResourceOperation.left?.let { textDocumentEdit ->
                    if (physicalDocument != uriToDocumentMap[textDocumentEdit.textDocument.uri]) return@forEach
                    if (!applyTextEdits(nonPhysicalDocument, textDocumentEdit.edits)) return@invokeForPreview
                }
            }
        }
    }
}
