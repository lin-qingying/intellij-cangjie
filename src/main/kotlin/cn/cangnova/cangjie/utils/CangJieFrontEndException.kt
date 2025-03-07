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

package cn.cangnova.cangjie.utils

import cn.cangnova.cangjie.descriptors.PsiDiagnosticUtils
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
        withPsiAttachment("element.cj", element)
    }
}
