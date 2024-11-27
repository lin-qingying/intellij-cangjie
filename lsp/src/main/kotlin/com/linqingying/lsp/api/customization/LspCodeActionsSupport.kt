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

import com.linqingying.lsp.api.LspServer
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.Diagnostic
import org.jetbrains.annotations.ApiStatus

/**
 * 处理从 LSP 服务器接收到的 [CodeAction](https://microsoft.github.io/language-server-protocol/specification#codeAction) 对象。
 */

open class LspCodeActionsSupport {

  /**
   * 是否让 IDE 向 LSP 服务器发送
   * [textDocument/codeAction](https://microsoft.github.io/language-server-protocol/specification/#textDocument_codeAction)
   * 请求，以接收特定 [Diagnostic]（参见 [CodeAction.diagnostics]）的快速修复建议。
   */
  open val quickFixesSupport = true

  /**
   * 为特定的 [CodeAction] 创建快速修复。
   * 如果不希望为该 [codeAction] 提供快速修复，具体实现可以返回 `null`。
   *
   * @param codeAction LSP 服务器的
   * [textDocument/codeAction](https://microsoft.github.io/language-server-protocol/specification/#textDocument_codeAction) 请求结果。
   * 请求是针对特定 [Diagnostic]（参见 [CodeAction.diagnostics]）的快速修复建议。
   */
  open fun createQuickFix(lspServer: LspServer, codeAction: CodeAction): LspIntentionAction? =
    LspIntentionAction(lspServer, codeAction)

  /**
   * 是否让 IDE 向 LSP 服务器发送
   * [textDocument/codeAction](https://microsoft.github.io/language-server-protocol/specification/#textDocument_codeAction)
   * 请求，以接收基于当前文本选择和光标位置的代码编辑建议，而不是特定 [Diagnostic] 的相关修复。
   *
   * 参见 [Intention actions](https://www.jetbrains.com/help/idea/intention-actions.html)。
   */
  open val intentionActionsSupport = true

  /**
   * 为特定的 [CodeAction] 创建 [意图操作](https://www.jetbrains.com/help/idea/intention-actions.html)。
   * 如果不希望为该 [codeAction] 提供意图操作，具体实现可以返回 `null`。
   *
   * @param codeAction LSP 服务器的
   * [textDocument/codeAction](https://microsoft.github.io/language-server-protocol/specification/#textDocument_codeAction) 请求结果。
   * 请求是基于当前文本选择和光标位置的代码编辑建议，而不是特定 [Diagnostic] 的相关修复。
   */
  open fun createIntentionAction(lspServer: LspServer, codeAction: CodeAction): LspIntentionAction? =
    LspIntentionAction(lspServer, codeAction)
}
