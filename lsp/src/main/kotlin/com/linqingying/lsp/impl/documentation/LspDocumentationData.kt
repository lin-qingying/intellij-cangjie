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

package com.linqingying.lsp.impl.documentation

import com.intellij.lang.documentation.QuickDocHighlightingHelper
import com.intellij.markdown.utils.doc.DocMarkdownToHtmlConverter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName.Companion.create
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.xml.util.XmlStringUtil
import com.linqingying.lsp.api.LspServerSupportProvider
import org.eclipse.lsp4j.MarkupContent
import org.jetbrains.annotations.NonNls
import java.lang.invoke.MethodHandles

data class LspDocumentationData(
    val definitionCodeBlock: @NlsSafe String? = null,
    val definitionLanguage: @NonNls String? = null,
    val description: @NlsSafe String? = null,
    val descriptionMarkup: DescriptionMarkup = DescriptionMarkup.PLAINTEXT
) {


    @RequiresReadLock
    fun toQuickDocHtml(project: Project): DocumentationResult.Documentation {


        val styledSignature = definitionCodeBlock?.let { codeBlock ->
            QuickDocHighlightingHelper.getStyledSignatureFragment(
                project,
                QuickDocHighlightingHelper.guessLanguage(definitionLanguage),
                codeBlock
            )
        }

        val definitionHtml = styledSignature?.let {
            "<div class='definition'><pre>$it</pre></div>"
        }

        val descriptionHtml = description?.let { desc ->
            when (descriptionMarkup) {
                DescriptionMarkup.MARKDOWN -> {
                    val converted = DocMarkdownToHtmlConverter.convert(
                        project,
                        desc,
                        QuickDocHighlightingHelper.guessLanguage(definitionLanguage)
                    )
                    converted.trim().removePrefix("<hr />")
                }

                DescriptionMarkup.PLAINTEXT -> {
                    XmlStringUtil.escapeString(desc)
                }


            }?.takeIf { it.isNotBlank() }?.let { content ->
                "<div class='content'>$content</div>"
            }
        }

        return DocumentationResult.Companion.documentation(
            (descriptionHtml ?: "") + (definitionHtml ?: "")
        )
    }

    override fun toString(): String {
        return "LspDocumentationData(definitionCodeBlock=" + this.definitionCodeBlock + ", definitionLanguage=" + this.definitionLanguage + ", description=" + this.description + ", descriptionMarkup=" + this.descriptionMarkup + ")"

    }


}

enum class DescriptionMarkup {
    PLAINTEXT,

    MARKDOWN;
}

interface LspMarkDownFormat {

    fun formatMarkdown(markupContent: MarkupContent): LspDocumentationData
    companion object {
        val EP_NAME = create<LspMarkDownFormat>("com.linqingying.lsp.markdownFormat")


    }
}

fun createLspDocumentationData(markupContent: MarkupContent): LspDocumentationData {


    when (markupContent.kind) {
        "markdown" -> {
            LspMarkDownFormat.EP_NAME.extensionList.firstOrNull()?.let {
                return it.formatMarkdown(markupContent)
            }

            val value = StringUtilRt.convertLineSeparators(markupContent.value)
            val indexOfCodeBlock = value.indexOf("```", 3)

            if (value.startsWith("```") && value.indexOf("\n") > 0 && indexOfCodeBlock >= 0) {
                val trimmedHeader = value.takeWhile { !it.isWhitespace() }
                val content = value.substring(3).trimStart()
                val description = content.ifEmpty { null }
                val codeBlock = value.substringAfter("\n", "").substringBefore("```").trimEnd()
                val remaining = value.substring(indexOfCodeBlock + 3).trimStart()

                return LspDocumentationData(codeBlock, description, remaining, DescriptionMarkup.MARKDOWN)
            } else {
                return LspDocumentationData(null, null, value, DescriptionMarkup.MARKDOWN)
            }
        }

        "plaintext" -> {
            return LspDocumentationData(null, null, markupContent.value, DescriptionMarkup.PLAINTEXT)
        }

        else -> {
            val logger = Logger.getInstance(MethodHandles.lookup().lookupClass())
            logger.warn("Unexpected MarkupKind: ${markupContent.kind}, treating as plain text")
            return LspDocumentationData(null, null, markupContent.value, DescriptionMarkup.PLAINTEXT)
        }
    }
}
