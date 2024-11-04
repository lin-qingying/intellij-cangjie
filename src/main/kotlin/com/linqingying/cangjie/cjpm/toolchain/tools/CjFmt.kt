package com.linqingying.cangjie.cjpm.toolchain.tools

import com.linqingying.cangjie.cjpm.toolchain.CjToolchainBase
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.vfs.VirtualFile


class CjFmt(
    toolchain: CjToolchainBase
) : CangJieComponent(NAME, toolchain) {

    init {
        toolchain.cjfmt = this
    }

    /**
     * 格式化整个文件
     */
    fun formatFile(file: VirtualFile): ProcessOutput? {
      return  GeneralCommandLine(executable, false, "-f", file.path).execute(1000)
    }


    companion object {
        const val NAME: String = "tools/bin/cjfmt"

    }
}
