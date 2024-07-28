package com.huawei.cangjie.cjpm.runconfig

import com.huawei.cangjie.ide.run.cjpm.runconfig.CjResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.util.io.BaseOutputReader


//class CjCapturingProcessHandler private constructor(commandLine: GeneralCommandLine) : CapturingProcessHandler(commandLine) {
//    override fun readerOptions(): BaseOutputReader.Options = BaseOutputReader.Options.BLOCKING
//
//    companion object {
//        fun startProcess(commandLine: GeneralCommandLine): CjResult<CjCapturingProcessHandler, ExecutionException> {
//            return try {
//                CjResult.Ok(CjCapturingProcessHandler(commandLine))
//            } catch (e: ExecutionException) {
//                CjResult.Err(e)
//            }
//        }
//    }
//}
