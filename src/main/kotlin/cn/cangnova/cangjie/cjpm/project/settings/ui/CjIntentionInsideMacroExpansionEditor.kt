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

package cn.cangnova.cangjie.ide.project.settings.ui


import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.impl.EmptySoftWrapModel
import com.intellij.openapi.editor.impl.ImaginaryEditor
import com.intellij.psi.PsiFile
import kotlin.math.min

class CjIntentionInsideMacroExpansionEditor(
    val psiFileCopy: PsiFile,
    val originalFile: PsiFile,
    val originalEditor: Editor,
    val initialMappedOffset: Int?,
    val context: CjIntentionInsideMacroExpansionContext?,
) : ImaginaryEditor(psiFileCopy.project, psiFileCopy.viewProvider.document!!) {

    init {
        if (initialMappedOffset != null) {
            caretModel.moveToOffset(initialMappedOffset)
        }
    }

    override fun notImplemented(): RuntimeException = IntentionInsideMacroExpansionEditorUnsupportedOperationException()

    override fun isViewer(): Boolean = true

    override fun isOneLineMode(): Boolean = false

    override fun getSettings(): EditorSettings {
        return originalEditor.settings
    }

    override fun logicalPositionToOffset(pos: LogicalPosition): Int {
        val document = document
        val lineStart = document.getLineStartOffset(pos.line)
        val lineEnd = document.getLineEndOffset(pos.line)
        return min(lineEnd, lineStart + pos.column)
    }

    override fun logicalToVisualPosition(logicalPos: LogicalPosition): VisualPosition {
        // No folding support: logicalPos is always the same as visual pos
        return VisualPosition(logicalPos.line, logicalPos.column)
    }

    override fun visualToLogicalPosition(visiblePos: VisualPosition): LogicalPosition {
        return LogicalPosition(visiblePos.line, visiblePos.column)
    }

    override fun offsetToLogicalPosition(offset: Int): LogicalPosition {
        val clamped = offset.coerceIn(0, document.textLength)
        val document = document
        val line = document.getLineNumber(clamped)
        val col = clamped - document.getLineStartOffset(line)
        return LogicalPosition(line, col)
    }

    override fun getSoftWrapModel(): SoftWrapModel = EmptySoftWrapModel()
}

class IntentionInsideMacroExpansionEditorUnsupportedOperationException
    : UnsupportedOperationException("It's unexpected to invoke this method on macro expansion fake editor")
