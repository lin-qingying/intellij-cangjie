package cn.cangnova.cangjie.lsp4ij

import cn.cangnova.cangjie.configurable.CangJieLspConfigurable
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider
import java.io.InputStream
import java.io.OutputStream

internal class CangJieLanguageServerFactory : LanguageServerFactory {
    override fun createConnectionProvider(p0: Project): StreamConnectionProvider {
        return CangJieStreamConnectionProvider(p0)
    }
}

private class CangJieStreamConnectionProvider(val project: Project) : StreamConnectionProvider {



    override fun start() {

    }

    override fun getInputStream(): InputStream? {
        TODO("Not yet implemented")
    }

    override fun getOutputStream(): OutputStream? {
        TODO("Not yet implemented")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }
}