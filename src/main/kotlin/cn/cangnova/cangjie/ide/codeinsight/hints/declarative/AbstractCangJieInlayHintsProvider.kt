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

package cn.cangnova.cangjie.ide.codeinsight.hints.declarative

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.declarative.InlayActionData
import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition
import com.intellij.codeInsight.hints.declarative.PresentationTreeBuilder
import com.intellij.codeInsight.hints.declarative.PsiPointerInlayActionNavigationHandler
import com.intellij.codeInsight.hints.declarative.PsiPointerInlayActionPayload
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.codeInsight.hints.declarative.StringInlayActionPayload
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.createSmartPointer
import cn.cangnova.cangjie.ide.codeinsight.hints.*

abstract class AbstractCangJieInlayHintsProvider(private vararg val hintTypes: HintType): InlayHintsProvider {

    override fun createCollector(
        file: PsiFile,
        editor: Editor
    ): InlayHintsCollector? {
        val project = editor.project ?: file.project
        if (project.isDefault) return null

        return object : SharedBypassCollector {
            override fun collectFromElement(
                element: PsiElement,
                sink: InlayTreeSink
            ) {
                val resolved = hintTypes.filter { it.isApplicable(element) }.ifEmpty { return }

                resolved.forEach { hintType ->
                    hintType.provideHintDetails(element).forEach { details: InlayInfoDetails ->
                        val inlayInfo: InlayInfo = details.inlayInfo
                        details.option?.let {
                            when (it) {
                                is NamedInlayInfoOption -> sink.whenOptionEnabled(it.name) {
                                    addInlayInfo(sink, inlayInfo, details)
                                }

                                NoInlayInfoOption -> addInlayInfo(sink, inlayInfo, details)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        internal fun PresentationTreeBuilder.addInlayInfoDetail(detail: InlayInfoDetail) {
                when (detail) {
                    is TextInlayInfoDetail -> text(detail.text)
                    is TypeInlayInfoDetail ->
                        text(detail.text,
                             detail.fqName?.let {
                                 InlayActionData(
                                     StringInlayActionPayload(it),
                                     CangJieFqnDeclarativeInlayActionHandler.HANDLER_NAME
                                 )
                             })

                    is PsiInlayInfoDetail ->
                        text(detail.text,
                             detail.element.createSmartPointer<PsiElement>().let {
                                 InlayActionData(
                                     PsiPointerInlayActionPayload(it),
                                     PsiPointerInlayActionNavigationHandler.HANDLER_ID
                                 )
                             })

                    else -> {}
                }
        }

        internal fun addInlayInfo(sink: InlayTreeSink, inlayInfo: InlayInfo, details: InlayInfoDetails) {
            sink.addPresentation(InlineInlayPosition(inlayInfo.offset, true), hasBackground = true) {
                details.details.forEach { detail ->
                    addInlayInfoDetail(detail)
                }
            }
        }
    }
}
