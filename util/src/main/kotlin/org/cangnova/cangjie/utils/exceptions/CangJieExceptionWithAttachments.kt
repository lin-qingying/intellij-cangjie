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
import org.cangnova.cangjie.utils.getElementTextWithContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.psi.PsiElement
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun checkWithAttachment(
    value: Boolean,
    lazyMessage: () -> String,
    attachments: (CangJieExceptionWithAttachments) -> Unit = {}
) {
    contract { returns() implies (value) }

    if (!value) {
        val e = CangJieExceptionWithAttachments(lazyMessage())
        attachments(e)
        throw e
    }
}

open class CangJieExceptionWithAttachments : RuntimeException, ICangJieExceptionWithAttachments {
    override val mutableAttachments = mutableListOf<Attachment>()

    override fun withAttachment(name: String, content: Any?): CangJieExceptionWithAttachments {
        return super.withAttachment(name, content) as CangJieExceptionWithAttachments
    }

    constructor(message: String) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause) {
        withAttachmentsFrom(cause)
    }

    fun withPsiAttachment(name: String, element: PsiElement?): CangJieExceptionWithAttachments {
        runCatching {
            ApplicationManager.getApplication().runReadAction<String> { element?.let(::getElementTextWithContext) }
        }
            .getOrNull()?.let { withAttachment(name, it) }
        return this
    }
}
