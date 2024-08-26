package com.huawei.cangjie.utils.exceptions

import com.huawei.cangjie.psi.psiUtil.getElementTextWithContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement


inline fun CangJieExceptionWithAttachments.buildAttachment(
    name: String = "info.txt",
    buildContent: ExceptionAttachmentBuilder.() -> Unit,
): CangJieExceptionWithAttachments {
    return withAttachment(name, ExceptionAttachmentBuilder().apply(buildContent).buildString())
}

inline fun buildErrorWithAttachment(
    message: String,
    cause: Exception? = null,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
): Throwable {
    val exception = CangJieIllegalArgumentExceptionWithAttachments(message, cause)
    exception.buildAttachment(attachmentName) { buildAttachment() }
    return exception
}
inline fun Logger.logErrorWithAttachment(
    message: String,
    cause: Exception? = null,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    this.error(buildErrorWithAttachment(message, cause, attachmentName, buildAttachment))
}
class ExceptionAttachmentBuilder {
    private val printer = SmartPrinter(StringBuilder())

    fun <T> withEntry(name: String, value: T, render: (T & Any) -> String) {
        withEntry(name) {
            println("Class: ${value?.let { it::class.java.name } ?: "<null>"}")
            println("Value:")
            withIndent {
                println(value?.let(render) ?: "<null>")
            }
        }
    }

    fun withEntry(name: String, value: String?) {
        with(printer) {
            println("- $name:")
            withIndent {
                println(value ?: "<null>")
            }
            println(separator)
        }
    }

    fun withEntry(name: String, buildValue: SmartPrinter.() -> Unit) {
        withEntry(name, SmartPrinter(StringBuilder()).apply(buildValue).toString())
    }

    fun withEntryGroup(groupName: String, build: ExceptionAttachmentBuilder.() -> Unit) {
        val builder = ExceptionAttachmentBuilder().apply(build)
        withEntry(groupName, builder) { it.buildString() }
    }

    fun buildString(): String = printer.toString()

    private companion object {
        private const val separator = "========"
    }
}
fun ExceptionAttachmentBuilder.withPsiEntry(name: String, psi: PsiElement?) {
    withEntry(name, psi) { psiElement ->
        getElementTextWithContext(psiElement)
    }
}
