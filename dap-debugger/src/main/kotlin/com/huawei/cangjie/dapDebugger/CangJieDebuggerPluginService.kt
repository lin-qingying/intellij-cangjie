package com.linqingying.cangjie.dapDebugger

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope

@Service
class CangJieDebuggerPluginService(val project:Project,val coroutineScope: CoroutineScope): Disposable {
    override fun dispose() {
        TODO("Not yet implemented")
    }
}
