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

package com.linqingying.cangjie.utils.exceptions

import com.linqingying.cangjie.utils.exceptions.CangJieExceptionWithAttachments.Companion.withAttachmentsFrom
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.ExceptionWithAttachments
import java.nio.charset.StandardCharsets



interface CangJieExceptionWithAttachments : ExceptionWithAttachments {
    val mutableAttachments: MutableList<Attachment>

    override fun getAttachments(): Array<Attachment> = mutableAttachments.toTypedArray()

    fun withAttachment(name: String, content: Any?): CangJieExceptionWithAttachments {
        mutableAttachments.add(Attachment(name, content?.toString() ?: "<null>"))
        return this
    }

    companion object {
        internal fun CangJieExceptionWithAttachments.withAttachmentsFrom(from: Throwable?) {
            if (from is CangJieExceptionWithAttachments) {
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

open class CangJieIllegalStateExceptionWithAttachments : IllegalStateException, CangJieExceptionWithAttachments {
    final override val mutableAttachments = mutableListOf<Attachment>()

    constructor(message: String) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause) {
        withAttachmentsFrom(cause)
    }
}

open class CangJieRuntimeExceptionWithAttachments : RuntimeException, CangJieExceptionWithAttachments {
    final override val mutableAttachments = mutableListOf<Attachment>()

    constructor(message: String) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause) {
        withAttachmentsFrom(cause)
    }
}

open class CangJieIllegalArgumentExceptionWithAttachments : IllegalArgumentException, CangJieExceptionWithAttachments {
    final override val mutableAttachments = mutableListOf<Attachment>()

    constructor(message: String) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause) {
        withAttachmentsFrom(cause)
    }
}

