package com.huawei.cangjie.utils

import com.huawei.cangjie.descriptors.PsiDiagnosticUtils
import com.intellij.psi.PsiElement

fun getExceptionMessage(
    subsystemName: String,
    message: String,
    cause: Throwable?,
    location: String?
): String =
    buildString {
        append(subsystemName).append(" Internal error: ").appendLine(message)

        if (location != null) {
            append("File being compiled: ").appendLine(location)
        } else {
            appendLine("File is unknown")
        }

        if (cause != null) {
            append("The root cause ${cause::class.java.name} was thrown at: ")
            append(cause.stackTrace?.firstOrNull()?.toString() ?: "unknown")
        }
    }


class CangJieFrontEndException(message: String, cause: Throwable) : CangJieExceptionWithAttachments(message, cause) {
    constructor(
        message: String,
        cause: Throwable,
        element: PsiElement
    ) : this(getExceptionMessage("Front-end", message, cause, PsiDiagnosticUtils.atLocation(element)), cause) {
        withPsiAttachment("element.kt", element)
    }
}
