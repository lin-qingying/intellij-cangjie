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

package com.linqingying.lsp.api

import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
import org.eclipse.lsp4j.InitializeResult

/**
 * 插件可以通过重写 [LspServerDescriptor.lspServerListener] 来注册他们的 [LspServerListener]。
 */
interface LspServerListener {
  /**
   * 一旦 IDE 收到来自 LSP 服务器的 [initialize](https://microsoft.github.io/language-server-protocol/specification/#initialize)
   * 请求的响应，它会发送 [initialized](https://microsoft.github.io/language-server-protocol/specification/#initialized)
   * 通知给服务器，并调用此函数。
   */
  @RequiresBackgroundThread
  @RequiresReadLockAbsence
  fun serverInitialized(params: InitializeResult) {
  }

  /**
   * 正常关闭的示例：
   * - 项目正在关闭
   * - 点击了语言服务状态栏小部件中的 'Restart' 按钮
   *
   * 意外关闭的示例：
   * - 尚未收到对 [initialize](https://microsoft.github.io/language-server-protocol/specification/#initialize)
   * 请求的响应
   * - LSP 服务器进程已终止
   */
  @RequiresBackgroundThread
  @RequiresReadLockAbsence
  fun serverStopped(shutdownNormally: Boolean) {
  }
}
