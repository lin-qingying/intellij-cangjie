package com.linqingying.cangjie.ide

import com.intellij.openapi.application.ApplicationManager
import javax.swing.Icon


abstract class CangJieIconProviderService {
    abstract val fileIcon: Icon?
    abstract val builtInFileIcon: Icon?

    class CompilerCangJieFileIconProviderService : CangJieIconProviderService() {
        override val builtInFileIcon: Icon? = null
        override val fileIcon: Icon? = null

    }

    companion object {
        val instance: CangJieIconProviderService
            get() {
                val service = ApplicationManager.getApplication().getService(
                    CangJieIconProviderService::class.java
                )
                return service ?: CompilerCangJieFileIconProviderService()
            }
    }
}
