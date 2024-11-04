package com.linqingying.cangjie.highlighter

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.SingleLazyInstanceSyntaxHighlighterFactory
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.cangjie.lang.declarations.CangJieBuiltInFileType


internal class CangJieSyntaxHighlighterFactory : SingleLazyInstanceSyntaxHighlighterFactory(),
    SyntaxHighlighterProvider {
    override fun createHighlighter(): SyntaxHighlighter = CangJieHighlighter()

    override fun create(fileType: FileType, project: Project?, file: VirtualFile?): SyntaxHighlighter? =
        when (fileType) {
            CangJieBuiltInFileType   -> getSyntaxHighlighter(project, file)
            else -> null
        }


}
