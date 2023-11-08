package com.huawei.cangjie.lang.lsp


import com.intellij.openapi.application.PreloadingActivity
import com.intellij.openapi.progress.ProgressIndicator
import org.eclipse.lsp4j.ClientInfo
import org.eclipse.lsp4j.InitializeParams

import java.io.File

//
//class BallerinaPreloadingActivity : PreloadingActivity() {
//    @Deprecated("Use {@link #execute()}", replaceWith = ReplaceWith("execute()"))
//    override fun preload(indicator: ProgressIndicator?) {
//
//
////        仓颉lsp服务
////        D:\Code\idea\intellij-cangjie\lsp\LSPServer.exe
//        val process = ProcessBuilder("D:\\Code\\idea\\intellij-cangjie\\lsp\\LSPServer.exe","src")
//process.directory(File("D:\\Code\\idea\\intellij-cangjie\\lsp\\"))
//
//        IntellijLanguageClient.addServerDefinition(
//            CangJieServerDefinition("Cangjie", process)
//        )
//    }
//}
//
//class CangJieServerDefinition(ext: String?, process: ProcessBuilder?) :
//    ProcessBuilderServerDefinition(ext, process) {
//
//}
