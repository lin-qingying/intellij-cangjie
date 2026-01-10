/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.utils.exceptions

import org.cangnova.cangjie.utils.exceptions.ICangJieExceptionWithAttachments.Companion.withAttachmentsFrom
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.ExceptionWithAttachments
import java.nio.charset.StandardCharsets
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

interface ICangJieExceptionWithAttachments : ExceptionWithAttachments {
    val mutableAttachments: MutableList<Attachment>

    override fun getAttachments(): Array<Attachment> = mutableAttachments.toTypedArray()

    fun withAttachment(name: String, content: Any?): ICangJieExceptionWithAttachments {
        mutableAttachments.add(Attachment(name, content?.toString() ?: "<null>"))
        return this
    }

    companion object {
        internal fun ICangJieExceptionWithAttachments.withAttachmentsFrom(from: Throwable?) {
            if (from is ICangJieExceptionWithAttachments) {
                from.mutableAttachments.mapTo(mutableAttachments) { attachment ->
                    attachment.copyWithNewName("case_${attachment.path}")
                }
            }
            if (from != null) {
                withAttachment("causeThrowable", from.stackTraceToString())
            }
        }

        private fun Attachment.copyWithNewName(newName: String): Attachment {
            val content = String(bytes, StandardCharsets.UTF_8)
            return Attachment(newName, content)
        }
    }
}

open class CangJieIllegalStateExceptionWithAttachments : IllegalStateException, ICangJieExceptionWithAttachments {
    final override val mutableAttachments = mutableListOf<Attachment>()

    constructor(message: String) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause) {
        withAttachmentsFrom(cause)
    }
}

open class CangJieRuntimeExceptionWithAttachments : RuntimeException, ICangJieExceptionWithAttachments {
    final override val mutableAttachments = mutableListOf<Attachment>()

    constructor(message: String) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause) {
        withAttachmentsFrom(cause)
    }
}

open class CangJieIllegalArgumentExceptionWithAttachments : IllegalArgumentException, ICangJieExceptionWithAttachments {
    final override val mutableAttachments = mutableListOf<Attachment>()

    constructor(message: String) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause) {
        withAttachmentsFrom(cause)
    }
}

@OptIn(ExperimentalContracts::class)
inline fun requireWithAttachment(
    condition: Boolean,
    message: () -> String,
    attachmentName: String = "info.txt",
    buildAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    contract { returns() implies (condition) }

    if (!condition) {
        val exception = CangJieIllegalArgumentExceptionWithAttachments(message())
        exception.buildAttachment(attachmentName) { buildAttachment() }
        throw exception
    }
}
