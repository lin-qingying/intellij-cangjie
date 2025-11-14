package org.cangnova.cangjie.protodebugger.symbol

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import java.nio.file.Path

@Tag
class NtSymbolSettings {
    companion object {
        const val microsoftSymbolServerURL: String = "https://msdl.microsoft.com/download/symbols"

        @JvmStatic
        fun getDefaultSymbolCachePath(): String {


            return Path.of(FileUtil.getTempDirectory(), * arrayOf("SymbolCache")).toString()
        }
    }

    var ntSymbolCache: String = ""
        @OptionTag get

    var ntSymbolPaths: MutableList<NtSymbolPathEntry> = mutableListOf()
        @OptionTag @XCollection get

    var ntSymbolServers: MutableList<NtSymbolPathEntry> = mutableListOf()
        @OptionTag @XCollection get

    var useNtSymbolServers: Boolean = false
        @OptionTag get
}

