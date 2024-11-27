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


package com.linqingying.lsp.api.customization

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.TextRange
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.DiagnosticTag
import org.jetbrains.annotations.ApiStatus

/**
 * 处理从 LSP 服务器接收到的 [Diagnostic](https://microsoft.github.io/language-server-protocol/specification#diagnostic) 对象。
 */

open class LspDiagnosticsSupport {
  @RequiresReadLock
  @RequiresBackgroundThread
  open fun createAnnotation(holder: AnnotationHolder, diagnostic: Diagnostic, textRange: TextRange, quickFixes: List<IntentionAction>) {
    val severity = getHighlightSeverity(diagnostic) ?: return
    holder.newAnnotation(severity, getMessage(diagnostic))
      .tooltip(getTooltip(diagnostic))
      .range(textRange)
      .let {
        val highlightType = getSpecialHighlightType(diagnostic)
        if (highlightType != null) it.highlightType(highlightType) else it
      }
      .let {
        var builder = it
        quickFixes.forEach { fix -> builder = builder.withFix(fix) }
        builder
      }
      .create()
  }

  /**
   * 如果此 [diagnostic] 应被忽略，实现可以返回 `null`。
   */
  open fun getHighlightSeverity(diagnostic: Diagnostic): HighlightSeverity? =
    when (diagnostic.severity) {
      DiagnosticSeverity.Error -> HighlightSeverity.ERROR
      DiagnosticSeverity.Warning -> HighlightSeverity.WARNING
      else -> HighlightSeverity.WEAK_WARNING
    }

  @InspectionMessage
  open fun getMessage(diagnostic: Diagnostic): String = diagnostic.message

  @NlsContexts.Tooltip
  open fun getTooltip(diagnostic: Diagnostic): String = diagnostic.message

  open fun getSpecialHighlightType(diagnostic: Diagnostic): ProblemHighlightType? = when {
    diagnostic.tags?.contains(DiagnosticTag.Unnecessary) == true -> ProblemHighlightType.LIKE_UNUSED_SYMBOL
    diagnostic.tags?.contains(DiagnosticTag.Deprecated) == true -> ProblemHighlightType.LIKE_DEPRECATED
    else -> null
  }
}
