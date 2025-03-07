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

package cn.cangnova.cangjie.highlighter

import com.intellij.codeHighlighting.*
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import cn.cangnova.cangjie.checkers.utils.DebugInfoUtil
import cn.cangnova.cangjie.config.CangJieIdePlugin
import cn.cangnova.cangjie.ide.base.projectStructure.RootKindFilter
import cn.cangnova.cangjie.ide.base.projectStructure.matches
import cn.cangnova.cangjie.ide.stubindex.resolve.isApplicationInternalMode
import cn.cangnova.cangjie.ide.stubindex.resolve.isUnitTestMode
import cn.cangnova.cangjie.psi.CjCodeFragment
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.psi.CjReferenceExpression


/**
 * Quick showing possible problems with CangJie internals in IDEA with tooltips
 */
class DebugInfoHighlightingPass(file: CjFile, document: Document) : AbstractBindingContextAwareHighlightingPassBase(file, document) {

    override fun annotate(element: PsiElement, holder: HighlightInfoHolder) {
        if (element is CjFile && element !is CjCodeFragment) {

            @Suppress("HardCodedStringLiteral")
            fun errorAnnotation(
                expression: PsiElement,
                message: String,
                textAttributes: TextAttributesKey? = CangJieHighlightingColors.DEBUG_INFO
            ) {
                val info = HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR)
                    .descriptionAndTooltip("[DEBUG] $message")
                    .range(expression.textRange)
                    .also {
                        textAttributes?.let { ta -> it.textAttributes(ta) }
                    }.create()
                holder.add(info)
            }
//            try {
//                DebugInfoUtil.markDebugAnnotations(element, bindingContext(), object : DebugInfoUtil.DebugInfoReporter() {
//                    override fun reportElementWithErrorType(expression: CjReferenceExpression) =
//                        errorAnnotation(expression, "Resolved to error element", CangJieHighlightingColors.RESOLVED_TO_ERROR)
//
//                    override fun reportMissingUnresolved(expression: CjReferenceExpression) =
//                        errorAnnotation(
//                            expression,
//                            "Reference is not resolved to anything, but is not marked unresolved"
//                        )
//
//                    override fun reportUnresolvedWithTarget(expression: CjReferenceExpression, target: String) =
//                        errorAnnotation(
//                            expression,
//                            "Reference marked as unresolved is actually resolved to $target"
//                        )
//                })
//            } catch (e: ProcessCanceledException) {
//                throw e
//            } catch (e: Throwable) {
//                // TODO
//                errorAnnotation(element, "${e.javaClass.canonicalName}: ${e.message}", null)
//                e.printStackTrace()
//            }

        }
    }

    class Factory : TextEditorHighlightingPassFactory {
        override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
            val useDebugInfoPass =
              file is CjFile &&
              !file.isCompiled &&
              // Temporary workaround to ignore red code in library sources
              file.shouldHighlightErrors() &&
              (isUnitTestMode() || isApplicationInternalMode() /*&& (CangJieIdePlugin.isSnapshot || CangJieIdePlugin.isDev)*/) &&
              RootKindFilter.projectAndLibrarySources.matches(file)

            return if (useDebugInfoPass) DebugInfoHighlightingPass(file as CjFile, editor.document) else null
        }
    }

    class Registrar : TextEditorHighlightingPassFactoryRegistrar {
        override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
            registrar.registerTextEditorHighlightingPass(
                Factory(),
                /* runAfterCompletionOf = */ intArrayOf(Pass.UPDATE_ALL),
                /* runAfterStartingOf = */ null,
                /* runIntentionsPassAfter = */ false,
                /* forcedPassId = */ -1
            )
        }
    }

}
