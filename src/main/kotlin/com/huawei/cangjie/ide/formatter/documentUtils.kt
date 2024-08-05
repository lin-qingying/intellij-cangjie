package com.huawei.cangjie.ide.formatter

import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.util.DocumentUtil


fun Document.adjustLineIndent(project: Project, offset: Int) {
    CodeStyleManager.getInstance(project).adjustLineIndent(this, DocumentUtil.getLineStartOffset(offset, this))
}
