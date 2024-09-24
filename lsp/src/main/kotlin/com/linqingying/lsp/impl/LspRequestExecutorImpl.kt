package com.linqingying.lsp.impl

import com.intellij.injected.editor.DocumentWindow
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.linqingying.lsp.api.LspServerState
import com.linqingying.lsp.api.requests.LspClientNotification
import com.linqingying.lsp.api.requests.LspRequest
import com.linqingying.lsp.api.requests.LspRequestExecutor
import com.linqingying.lsp.impl.completion.asCompletionList
import com.linqingying.lsp.impl.completion.createCompletionParams
import com.linqingying.lsp.util.getLsp4jPosition
import kotlinx.coroutines.future.await
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.*


class LspRequestExecutorImpl(private val lspServer: LspServerImpl) : LspRequestExecutor {
    companion object {
        val LOG = Logger.getInstance(LspRequestExecutorImpl::class.java)
    }

    private inner class HoverResultCache {
        private var lastFilePath: String = ""

        private var lastPsiModificationCount: Long = -1L

        private var lastResult: TextRangeAndMarkupContent? = null
        fun clearCache() {
            this.lastResult = null
            this.lastPsiModificationCount = -1L
            this.lastFilePath = ""
        }

        @RequiresReadLock
        @RequiresBackgroundThread
        @Synchronized
        fun getHoverInformation(
            file: VirtualFile,
            offset: Int,
            timeoutMs: Int
        ): TextRangeAndMarkupContent? {
            ProgressManager.checkCanceled()

            if (file is VirtualFileWindow) {
                Logger.getInstance(HoverResultCache::class.java).error("VirtualFileWindow not expected here")
                return null
            }

            val project = lspServer.project
            val currentResult = lastResult
            val modificationCount = PsiManager.getInstance(project).modificationTracker.modificationCount
            val filePath = file.path

            if (currentResult != null && lastPsiModificationCount == modificationCount && lastFilePath == filePath && currentResult.textRange.contains(
                    offset
                )
            ) {
                return currentResult
            } else {
                val hoverParams =
                    this@LspRequestExecutorImpl.getDocumentInfo(file, offset) { hostFile, hostDocument, hostOffset ->
                        HoverParams(
                            lspServer.getDocumentIdentifier(hostFile),
                            getLsp4jPosition(hostDocument, hostOffset)
                        )
                    }

                val hover = sendRequestSync(timeoutMs) { server ->
                    server.textDocumentService.hover(hoverParams)
                }

                return hover?.let {
                    val result = TextRangeAndMarkupContent.fromHover(it, file, offset)
                    lastResult = result
                    lastPsiModificationCount = modificationCount
                    lastFilePath = filePath
                    result
                }
            }
        }
    }

    private val hoverResultCache: HoverResultCache = HoverResultCache()

    private val executorService: ExecutorService =
        AppExecutorUtil.createBoundedApplicationPoolExecutor("LSP Executor: " + lspServer.descriptor, 1)


    override fun sendNotification(notification: LspClientNotification) {

        sendNotification { notification.sendNotification() }
    }


    internal fun sendNotification(lsp4jSender: (LanguageServer) -> Unit) {

        synchronized(executorService) {

            if (!this.executorService.isShutdown) {
                this.executorService.execute {
                    lsp4jSender(lspServer.lsp4jServer)
                }
            }

        }
    }

    @RequiresReadLock
    @RequiresBackgroundThread
    private fun <Lsp4jParams> getDocumentDetailsForRequest(
        file: VirtualFile,
        offset: Int,
        lsp4jParamsCreator: (VirtualFile, Document, Int) -> Lsp4jParams
    ): Lsp4jParams? {
        val document = runReadAction {
            FileDocumentManager.getInstance().getDocument(file)
        } ?: run {
            lspServer.logError("No document for file $file")
            return null
        }

        val virtualFile = when (file) {
            is VirtualFileWindow -> file.delegate
            else -> file
        }

        val actualDocument = when (document) {
            is DocumentWindow -> document.delegate
            else -> document
        }

        val adjustedOffset = if (document is DocumentWindow) {
            document.injectedToHost(offset)
        } else {
            offset
        }

        return lsp4jParamsCreator(virtualFile, actualDocument, adjustedOffset)
    }

    override fun getDocumentIdentifier(file: VirtualFile): TextDocumentIdentifier {
        return lspServer.getDocumentIdentifier(file)
    }

    @RequiresReadLock
    @RequiresBackgroundThread
    private fun <Lsp4jParams> getDocumentInfo(
        file: VirtualFile,
        offset: Int,
        lsp4jParamsCreator: (VirtualFile, Document, Int) -> Lsp4jParams
    ): Lsp4jParams? {
        val document = FileDocumentManager.getInstance().getDocument(file) ?: run {
            lspServer.logError("No document for file $file")
            return null
        }

        val virtualFile = (file as? VirtualFileWindow)?.delegate ?: file
        val documentToUse = (document as? DocumentWindow)?.delegate ?: document
        val adjustedOffset = (document as? DocumentWindow)?.injectedToHost(offset) ?: offset

        return lsp4jParamsCreator(virtualFile, documentToUse, adjustedOffset)
    }

    @RequiresReadLock
    @RequiresBackgroundThread
    fun getCompletionList(
        file: VirtualFile,
        offset: Int,
        isAutoPopup: Boolean
    ): CompletionList? {
        val either = sendRequestSync { server: LanguageServer ->

            server.textDocumentService.completion(createCompletionParams(file, offset, isAutoPopup))


        } ?: return null

        return either.asCompletionList()
    }

    @RequiresReadLock
    @RequiresBackgroundThread
    fun getHoverInformation(
        file: VirtualFile,
        offset: Int,
        timeoutMs: Int = 10000
    ): TextRangeAndMarkupContent? {
        return hoverResultCache.getHoverInformation(file, offset, timeoutMs)

    }

    private fun createCompletionParams(
        file: VirtualFile,
        offset: Int,
        isAutoPopup: Boolean
    ): CompletionParams? {
        return this.getDocumentDetailsForRequest(file, offset) { hostFile, hostDocument, hostOffset ->

            createCompletionParams(
                lspServer,
                hostFile,
                hostDocument,
                hostOffset,
                isAutoPopup
            )
        }
    }

    @RequiresReadLock
    @RequiresBackgroundThread
    fun getCompletionListAsync(
        file: VirtualFile,
        offset: Int,
        isAutoPopup: Boolean
    ): CompletableFuture<CompletionList?> {
        val completionParams = createCompletionParams(file, offset, isAutoPopup)

        val future = submitRequest { languageServer: LanguageServer ->

            languageServer.textDocumentService.completion(completionParams)


        } ?: CompletableFuture.completedFuture(null)

        return future.thenApply {
            it?.asCompletionList()

        }
    }


    private fun <Lsp4jResponse> submitRequest(lsp4jSender: (LanguageServer) -> CompletableFuture<Lsp4jResponse>): CompletableFuture<Lsp4jResponse?>? {
        if (lspServer.state != LspServerState.Running) {
            lspServer.logDebug("Server not initialized yet, skipping request " + lsp4jSender.javaClass.name)
            return null
        } else {
            synchronized(executorService) {
                if (!executorService.isShutdown) {
                    val resultFuture = CompletableFuture<Lsp4jResponse?>()

                    executorService.execute {
                        lsp4jSender(lspServer.lsp4jServer).apply {
                            whenComplete { response, t ->
                                t?.let {
                                    resultFuture.completeExceptionally(it)
                                } ?: run {
                                    resultFuture.complete(response)
                                }
                            }.whenComplete { _, throwable ->

//                                if (resultFuture.isCancelled) {
//                                    取消
//                                    sendCancelRequest(this)
//                                }

                            }
                        }
                    }
                    return resultFuture
                }
            }
        }
        return null
    }



    private fun <Lsp4jResponse> executeRequest(
        lsp4jSender: (LanguageServer) -> CompletableFuture<Lsp4jResponse>,
        lsp4jResponseConsumer: (Lsp4jResponse?) -> Unit
    ): CompletableFuture<Lsp4jResponse?>? {
        val future = this.submitRequest(lsp4jSender)
        if (future == null) {
            lsp4jResponseConsumer(null)
            return null
        } else {
            future.whenComplete { lsp4jresponse, _ ->
                lsp4jResponseConsumer(lsp4jresponse)
            }
            return future
        }

    }

    @Deprecated("Use LspServer.sendRequest")
    override fun <Lsp4jResponse, Result> sendRequestAsync(
        lspRequest: LspRequest<Lsp4jResponse, Result>,
        resultConsumer: (Result?) -> Unit
    ) {
        executeRequest(
            { lspRequest.sendRequest() }, { lsp4jResponse ->

                resultConsumer(lsp4jResponse?.let { lspRequest.preprocessResponse(it) })
            }
        )


    }

    @RequiresBackgroundThread
    private fun <T> waitForFutureResponse(
        future: Future<T?>,
        cancelOnPCE: Boolean,
        debugName: String,
        timeoutMs: Int
    ): T? {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs.toLong()) {
            try {
                ProgressManager.checkCanceled()
            } catch (e: ProcessCanceledException) {
                if (cancelOnPCE) {
                    future.cancel(true)
                }
                throw e
            }

            try {
                return future.get(10L, TimeUnit.MILLISECONDS)
            } catch (e: TimeoutException) {
                // Ignore and continue waiting
            } catch (e: ExecutionException) {
                lspServer.logWarn(

                    "Error response from server: ${e.cause}", null

                )
                return null
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }

        this.lspServer.logInfo("No response from the server in $timeoutMs ms for: $debugName")
        future.cancel(true)
        return null
    }


    @RequiresBackgroundThread
    internal fun <Lsp4jResponse> sendRequestSync(
        timeoutMs: Int = 1000,
        lsp4jSender: (LanguageServer) -> CompletableFuture<Lsp4jResponse>
    ): Lsp4jResponse? {
        val future = this.submitRequest(lsp4jSender) ?: return null


        return waitForFutureResponse(future, true, lsp4jSender.javaClass.name, timeoutMs)
    }

    internal suspend fun <Lsp4jResponse> sendRequest(lsp4jSender: (LanguageServer) -> CompletableFuture<Lsp4jResponse>): Lsp4jResponse? {
        val future = this.submitRequest(lsp4jSender) ?: return null


        return future.await()


    }

    internal fun shutdownNow() {
        val var1 = this.executorService
        synchronized(var1) {
            val var2 = false
            val var4: List<*> = executorService.shutdownNow()
        }

        this.hoverResultCache.clearCache()
    }

    internal fun <Lsp4jResponse> sendRequestAsyncButWaitForResponseWithCheckCanceled(
        lsp4jSender: (LanguageServer) -> CompletableFuture<Lsp4jResponse>,
        lsp4jResponseConsumer: (Lsp4jResponse?) -> Unit
    ) {
        val future = executeRequest(lsp4jSender, lsp4jResponseConsumer)
        future?.let {
            val senderClassName = lsp4jSender::class.java.name
            waitForFutureResponse(it, false, senderClassName, 10_000)
        }
    }

    @RequiresReadLock
    @RequiresBackgroundThread
    fun getElementDefinitions(
        file: VirtualFile,
        offset: Int
    ): List<LocationLink> {
        val definitionParams = getDocumentDetailsForRequest(file, offset) { hostFile, hostDocument, hostOffset ->

            DefinitionParams(
                lspServer.getDocumentIdentifier(hostFile),
                getLsp4jPosition(hostDocument, hostOffset)
            )
        }

        val response = sendRequestSync { languageServer ->

            languageServer.textDocumentService.definition(definitionParams)
        }

        return if (response == null) {
            emptyList()
        } else {
            when {
                response.isRight -> {
                    val rightValue = response.right

                    rightValue.distinct()
                }

                else -> {
                    val leftValue = response.left

                    val locations = leftValue.map { location ->
                        LocationLink(location.uri, location.range, location.range)
                    }
                    locations.distinct()
                }
            }
        }
    }

    @Deprecated(message = "Use LspServer.sendRequest")
    override fun <Lsp4jResponse, Result> sendRequestSync(lspRequest: LspRequest<Lsp4jResponse, Result>): Result? {
        return sendRequestSync {
            lspRequest.sendRequest()
        }?.let {
            lspRequest.preprocessResponse(it)
        }

    }
}
