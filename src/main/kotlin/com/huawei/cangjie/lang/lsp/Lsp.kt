package com.huawei.cangjie.lang.lsp

//import com.huawei.cangjie.lang.CangJieFileType
//import com.huawei.cangjie.psi.CjFile
//import com.intellij.execution.configurations.GeneralCommandLine
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.vfs.VirtualFile
//import com.intellij.platform.lsp.api.LspServerSupportProvider
//import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
//
//
//class FooLspServerSupportProvider : LspServerSupportProvider {
//    override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerSupportProvider.LspServerStarter) {
//       if(file.fileType != CangJieFileType) return
//        serverStarter.ensureServerStarted(FooLspServerDescriptor(project));
//    }
//}
//private class FooLspServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "Foo") {
//    override fun isSupportedFile(file: VirtualFile) = file.fileType == CangJieFileType
//    override fun createCommandLine() = GeneralCommandLine("foo", "--stdio")
//}
 
