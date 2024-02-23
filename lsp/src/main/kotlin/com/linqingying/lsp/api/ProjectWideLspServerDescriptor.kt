package com.linqingying.lsp.api

import com.intellij.openapi.project.BaseProjectDirectories
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe

/**
 * A helper class that assumes that a single LSP server is going to serve the whole project, regardless of the project structure.
 * So, it uses all [BaseProjectDirectories.getBaseDirectories] as LSP server roots.
 */
abstract class ProjectWideLspServerDescriptor(project: Project, @NlsSafe presentableName: String) :
    com.linqingying.lsp.api.LspServerDescriptor(project, presentableName, *project.getBaseDirectories().toTypedArray())
