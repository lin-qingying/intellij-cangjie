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

package com.linqingying.lsp.impl.completion

import com.intellij.model.Pointer
import com.intellij.model.Symbol
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.linqingying.lsp.impl.LspServerImpl
import com.linqingying.lsp.impl.documentation.createLspDocumentationData


import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageServer


class LspCompletionObject(
    val lspServer: LspServerImpl,
    val initialCompletionItem: CompletionItem
) : Pointer<LspCompletionObject>,
    Symbol, DocumentationTarget {
    val completionItem: CompletionItem
        get() =
            this.resolvedCompletionItem ?: this.initialCompletionItem


    private var resolvedCompletionItem: CompletionItem? = null

    override fun computeDocumentation(): DocumentationResult? {
        val completionItem = resolvedCompletionItem
        completionItem?.let {
            it.documentation?.let { documentation ->
                return convertToDocumentationResult(documentation)
            }
        }

        initialCompletionItem.documentation?.let { documentation ->
            return convertToDocumentationResult(documentation) as DocumentationResult
        } ?: run {
            if (resolvedCompletionItem != null) {
                return null
            }

            val serverCapabilities = lspServer.serverCapabilities
            val isResolveProviderEnabled = serverCapabilities?.completionProvider?.resolveProvider == true

            return if (!isResolveProviderEnabled) {
                null
            } else {
                DocumentationResult.asyncDocumentation {
                    val future = lspServer.sendRequest {
                        it.textDocumentService.resolveCompletionItem(initialCompletionItem)

                    }

                    val resolvedItem = future ?: initialCompletionItem

                    resolvedCompletionItem = resolvedItem
                    resolvedCompletionItem?.documentation?.let { doc ->
                        return@asyncDocumentation convertToDocumentationResult(doc)
                    }
                    null
                }
            }
        }
    }

    @RequiresReadLock
    private fun convertToDocumentationResult(lsp4jDocs: Either<String, MarkupContent>): DocumentationResult.Documentation? {
        val markupContent: MarkupContent? = when {
            lsp4jDocs.isLeft -> {
                val leftValue = lsp4jDocs.left

                if ((leftValue as CharSequence).isNotEmpty()) {
                    MarkupContent("plaintext", leftValue.toString())
                } else {
                    null
                }
            }

            lsp4jDocs.isRight -> {
                val rightValue = lsp4jDocs.right as MarkupContent
                val value = rightValue.value

                if (value.isNotEmpty()) {
                    rightValue
                } else {
                    null
                }
            }

            else -> null
        }

        return markupContent?.let {
            createLspDocumentationData(it).toQuickDocHtml(lspServer.project)
        }
    }

    override fun computePresentation(): TargetPresentation {
        val label = initialCompletionItem.label
        return TargetPresentation.builder(label).presentation()
    }

    override fun createPointer(): Pointer<LspCompletionObject> = Pointer.hardPointer(this)

    override fun dereference(): LspCompletionObject = this

    @RequiresBackgroundThread
    internal fun resolveCompletionItem() {
        if (resolvedCompletionItem == null) {
            val isResolveProviderEnabled: Boolean = run {
                val serverCapabilities = lspServer.serverCapabilities
                serverCapabilities?.completionProvider?.resolveProvider == true

            }

            resolvedCompletionItem = if (!isResolveProviderEnabled) {
                initialCompletionItem
            } else {
                val completionItem = lspServer.sendRequestSync { server: LanguageServer ->

                    val future = server.textDocumentService.resolveCompletionItem(initialCompletionItem)

                    future
                }
                completionItem ?: initialCompletionItem
            }
        }
    }


}

