/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.utils.exceptions

import cn.cangnova.cangjie.psi.psiUtil.getElementTextWithContext
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
inline fun errorWithAttachment(
    message: String,
    cause: Throwable? = null,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
): Nothing {
    throw buildErrorWithAttachment(message, cause, attachmentName, buildAttachment)
}
inline fun buildErrorWithAttachment(
    message: String,
    cause: Throwable? = null,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
): Throwable {
    val exception = CangJieIllegalArgumentExceptionWithAttachments(message, cause)
    exception.buildAttachment(attachmentName) { buildAttachment() }
    return exception
}
