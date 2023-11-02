package com.huawei.cangjie.lang.lsp


import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.progress.ProgressIndicator
import org.wso2.lsp4intellij.IntellijLanguageClient
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.RawCommandServerDefinition



class BallerinaPreloadingActivity : PreloadingActivity() {
    override fun preload(indicator: ProgressIndicator?) {
        IntellijLanguageClient.addServerDefinition(
            RawCommandServerDefinition(
                "cangjie",
                arrayOf("D:\\Code\\idea\\intellij-cangjie\\lsp\\LSPServer.exe")
            )
        )
    }
}
