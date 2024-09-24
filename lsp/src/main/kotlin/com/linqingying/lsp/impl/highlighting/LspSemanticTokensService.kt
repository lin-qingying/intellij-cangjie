package com.linqingying.lsp.impl.highlighting

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
class LspSemanticTokensService(val cs: CoroutineScope) {
    companion object {
        fun getInstance(project: Project):  LspSemanticTokensService {
            return project.service<LspSemanticTokensService>()
        }
    }


}

