package com.huawei.cangjie.utils

import com.huawei.cangjie.utils.exceptions.CangJieExceptionWithAttachments.Companion.withAttachmentsFrom
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.psi.PsiElement
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import com.huawei.cangjie.utils.exceptions.CangJieExceptionWithAttachments as CangJieExceptionWithAttachmentsBase
@OptIn(ExperimentalContracts::class)
inline fun checkWithAttachment(value: Boolean, lazyMessage: () -> String, attachments: ( CangJieExceptionWithAttachments) -> Unit = {}) {
    contract { returns() implies (value) }

    if (!value) {
        val e = CangJieExceptionWithAttachments(lazyMessage())
        attachments(e)
        throw e
    }
}
open class CangJieExceptionWithAttachments : RuntimeException, CangJieExceptionWithAttachmentsBase {
    override val mutableAttachments = mutableListOf<Attachment>()

    override fun withAttachment(name: String, content: Any?): CangJieExceptionWithAttachments {
        return super.withAttachment(name, content) as CangJieExceptionWithAttachments
    }

    constructor(message: String) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause) {
        withAttachmentsFrom(cause)
    }

    fun withPsiAttachment(name: String, element: PsiElement?): CangJieExceptionWithAttachments {
        kotlin.runCatching { ApplicationManager.getApplication().runReadAction<String> { element?.let(::getElementTextWithContext) } }
            .getOrNull()?.let { withAttachment(name, it) }
        return this
    }
}
