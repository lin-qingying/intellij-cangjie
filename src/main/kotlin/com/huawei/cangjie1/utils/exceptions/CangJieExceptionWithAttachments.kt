package com.huawei.cangjie1.utils.exceptions

import com.huawei.cangjie1.utils.exceptions.CangJieExceptionWithAttachments.Companion.withAttachmentsFrom
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

