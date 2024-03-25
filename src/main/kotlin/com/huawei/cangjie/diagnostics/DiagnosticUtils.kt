package com.huawei.cangjie.diagnostics

import com.intellij.openapi.application.ApplicationManager

object DiagnosticUtils {

    fun throwIfRunningOnServer(e: Throwable?) {
        // This is needed for the Web Demo server to log the exceptions coming from the analyzer instead of showing them in the editor.
        if (System.getProperty(
                "cangjie.running.in.server.mode",
                "false"
            ) == "true" || ApplicationManager.getApplication().isUnitTestMode
        ) {
            if (e is RuntimeException) {
                throw (e as RuntimeException?)!!
            }
            if (e is Error) {
                throw (e as Error?)!!
            }
            throw RuntimeException(e)
        }
    }

}