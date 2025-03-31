package cn.cangnova.cangjie.lsp.listener

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

internal class LspFileListener : AsyncFileListener {
    override fun prepareChange(events: List<  VFileEvent>): AsyncFileListener.ChangeApplier? {

 return null
    }

}