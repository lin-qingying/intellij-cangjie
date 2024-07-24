package com.huawei.cangjie.dapDebugger.runconfig

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.util.io.BaseOutputReader
import com.pty4j.PtyProcess


class DapProcessHandler : OSProcessHandler {
    override fun readerOptions(): BaseOutputReader.Options {
        return BaseOutputReader.Options.forMostlySilentProcess()
    }

    constructor(generalCommandLine: GeneralCommandLine) : super(generalCommandLine)

    constructor(process: Process, commandLine: String) : super(process, commandLine, Charsets.UTF_8)


    val press :PtyProcess get() = process as PtyProcess
}
