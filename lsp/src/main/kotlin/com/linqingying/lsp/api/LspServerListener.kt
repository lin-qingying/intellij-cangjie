
package com.linqingying.lsp.api

import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
import org.eclipse.lsp4j.InitializeResult

/**
 * Plugins can register their [LspServerListener] by overriding [LspServerDescriptor.lspServerListener].
 */
interface LspServerListener {
  /**
   * Once the IDE receives a response from the LSP server to the
   * [initialize](https://microsoft.github.io/language-server-protocol/specification/#initialize) request,
   * it sends the [initialized](https://microsoft.github.io/language-server-protocol/specification/#initialized)
   * notification to the server and calls this function.
   */
  @RequiresBackgroundThread
  @RequiresReadLockAbsence
  fun serverInitialized(params: InitializeResult) {
  }
}
