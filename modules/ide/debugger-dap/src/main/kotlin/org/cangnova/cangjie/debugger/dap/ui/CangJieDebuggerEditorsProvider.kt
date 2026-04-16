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

package org.cangnova.cangjie.debugger.dap.ui

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider

/**
 * 仓颉调试器编辑器提供者
 *
 * 提供表达式求值和条件断点的编辑器
 */
class CangJieDebuggerEditorsProvider : XDebuggerEditorsProvider() {

    override fun getFileType(): FileType {
        // TODO: 返回仓颉文件类型
        // return CangJieFileType.INSTANCE
        return com.intellij.openapi.fileTypes.PlainTextFileType.INSTANCE
    }

    override fun createDocument(
        project: Project,
        expression: com.intellij.xdebugger.XExpression,
        sourcePosition: XSourcePosition?,
        mode: EvaluationMode
    ): Document {
        return EditorFactory.getInstance().createDocument(expression.expression)
    }


}
