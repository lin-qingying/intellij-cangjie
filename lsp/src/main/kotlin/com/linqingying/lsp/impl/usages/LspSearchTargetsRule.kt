package com.linqingying.lsp.impl.usages

import com.intellij.find.usages.api.SearchTarget
import com.intellij.ide.impl.dataRules.GetDataRule
import com.intellij.injected.editor.EditorWindow
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.linqingying.lsp.impl.LspServerManagerImpl
import com.linqingying.lsp.util.getLsp4jPosition

private class LspSearchTargetsRule : GetDataRule {
    override fun getData(dataProvider: DataProvider): Collection<SearchTarget>? {

        val editor = CommonDataKeys.EDITOR.getData(dataProvider) ?: return null
        if (editor is EditorWindow) return null

        val project = editor.project?.takeIf { !it.isDefault } ?: return null
        val virtualFile = editor.virtualFile ?: return null
        val document = editor.document
        val offset = editor.caretModel.offset
        val position = getLsp4jPosition(document, offset)

        val lspServers = LspServerManagerImpl.getInstanceImpl(project)
            .getServersWithThisFileOpen(virtualFile).toList()
            .filter { it.descriptor.lspFindReferencesSupport != null && it.supportsFindReferences(virtualFile) }

        return if (lspServers.isNotEmpty()) {
            listOf(LspSearchTarget(lspServers, virtualFile, position))
        } else {
            null
        }
    }
}

