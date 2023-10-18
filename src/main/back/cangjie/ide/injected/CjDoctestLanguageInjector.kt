package com.huawei.cangjie.ide.injected

import com.huawei.cangjie.lang.core.psi.CjFile
import com.huawei.cangjie.lang.doc.psi.CjDocCodeFence
import com.huawei.cangjie.openapiext.toPsiFile
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

fun VirtualFile.isDoctestInjection(project: Project): Boolean {
    val virtualFileWindow = this as? VirtualFileWindow ?: return false
    val hostFile = virtualFileWindow.delegate.toPsiFile(project) as? CjFile ?: return false
    val hostElement = hostFile.findElementAt(virtualFileWindow.documentWindow.injectedToHost(0)) ?: return false
    return hostElement.parent is CjDocCodeFence
}
