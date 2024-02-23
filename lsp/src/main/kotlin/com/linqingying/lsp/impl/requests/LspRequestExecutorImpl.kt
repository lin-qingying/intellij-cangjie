package com.linqingying.lsp.impl.requests

import com.intellij.injected.editor.DocumentWindow
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.ex.DocumentEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.VirtualFile


import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.linqingying.lsp.api.customization.requests.LspRequest
import com.linqingying.lsp.api.customization.requests.LspRequestExecutor
import com.linqingying.lsp.api.requests.LspClientNotification
import com.linqingying.lsp.impl.LspServerImpl
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.LocationLink
import org.eclipse.lsp4j.TextDocumentIdentifier
import java.util.concurrent.*


open class LspRequestExecutorImpl(private val lspServer: LspServerImpl) : LspRequestExecutor {
    companion object {
        val LOG = Logger.getInstance(LspRequestExecutorImpl::class.java)
    }

    private val hoverResultCache: HoverResultCache = HoverResultCache()

    private val SYNC_RESPONSE_TIMEOUT: Long = TimeUnit.SECONDS.toMillis(10L)

    private val executorService: ExecutorService =
        AppExecutorUtil.createBoundedApplicationPoolExecutor("LSP Executor: " + this.lspServer.descriptor, 1)

    override fun getDocumentIdentifier(file: VirtualFile): TextDocumentIdentifier =
        TextDocumentIdentifier(lspServer.descriptor.getFileUri(file))

    @RequiresReadLock
    @RequiresBackgroundThread
    @JvmOverloads
    fun getHoverInformation(
        file: VirtualFile,
        offset: Int,
        timeoutMs: Int = 1000
    ): TextRangeAndMarkupContent? {
        return this.hoverResultCache.getHoverInformation(file, offset, timeoutMs)

    }

    @RequiresBackgroundThread
    fun getResolvedCompletionItem(completionItem: CompletionItem): CompletionItem {
        val resolvedItem = sendRequestSync(LspResolveCompletionItemRequest(lspServer, completionItem))
        return resolvedItem ?: completionItem
    }


    internal fun <Result> sendRequestAsyncButWaitForResponseWithCheckCanceled(
        lspRequest: LspRequest<*, Result>,
        resultConsumer: (Result?) -> Unit
    ) {
        processRequest(lspRequest, resultConsumer)?.let { future ->
            waitForResponse(future, lspRequest.toString())
        }
    }

    override fun getDocumentVersion(document: Document): Int {
        return if ((if (document is DocumentEx) document else null) != null) (if (document is DocumentEx) document else null)!!.modificationSequence else document.modificationStamp.toInt()
    }


    @RequiresReadLock
    @RequiresBackgroundThread
    fun getElementDefinitions(file: VirtualFile, offset: Int): List<LocationLink> {
        val request = sendOffsetBasedRequestSync(file, offset, 1000) { hostFile, hostDocument, hostOffset ->
            LspDefinitionRequest(lspServer, hostFile, hostDocument, hostOffset)
        }

        return request ?: emptyList()
//        return processRequest(file, offset) { hostFile: VirtualFile, hostDocument: Document, hostOffset: Int ->
//            LspDefinitionRequest(lspServer, hostFile, hostDocument, hostOffset)
//        } ?: emptyList()
    }


    override fun sendNotification(notification: LspClientNotification) {
        synchronized(executorService) {
            if (!executorService.isShutdown) {
                executorService.execute { notification.sendNotification() }
            }
        }
    }

    @RequiresReadLock
    @RequiresBackgroundThread
    fun getCompletionItems(file: VirtualFile, offset: Int, isAutoPopup: Boolean): List<CompletionItem> {
        return processRequest(file, offset) { hostFile, hostDocument, hostOffset ->
            LspCompletionRequest(lspServer, hostFile, hostDocument, hostOffset, isAutoPopup)
        } ?: emptyList()


    }

    @RequiresReadLock
    @RequiresBackgroundThread
    private fun <Result> processRequest(
        file: VirtualFile,
        offset: Int,
        request: (VirtualFile, Document, Int) -> LspRequest<*, Result>
    ): Result? {

        val document = FileDocumentManager.getInstance().getDocument(file) ?: run {
            LOG.error("No document for file $file")
            return null
        }

        val actualFile = (file as? VirtualFileWindow)?.delegate ?: file
        val actualDocument = (document as? DocumentWindow)?.delegate ?: document
        val actualOffset = (document as? DocumentWindow)?.injectedToHost(offset) ?: offset

        return sendRequestSync(request(actualFile, actualDocument, actualOffset))

    }


    private fun <Result> processRequest(
        request: LspRequest<*, Result>,
        resultConsumer: (Result?) -> Unit
    ): CompletableFuture<Result?>? {
        val future = this.processRequest(request)
        return if (future == null) {
            resultConsumer.invoke(null)
            null
        } else {
            //            future.whenComplete(::processRequest)
            future.whenComplete { result, throwable ->
                if (throwable != null) {
                    LOG.warn("Request $request failed", throwable)
                    resultConsumer.invoke(null)
                } else {
                    resultConsumer.invoke(result)
                }
            }
            future
        }
    }

    private fun <ServerResponse, Result> processRequest(request: LspRequest<ServerResponse, Result>): CompletableFuture<Result?>? {
        if (!this.lspServer.hasCapabilities()) {
            LOG.debug("Server not initialized yet, skipping request $request")
            return null
        } else {

            synchronized(executorService) {
                if (!executorService.isShutdown) {
                    val resultFuture = CompletableFuture<Result?>()
                    executorService.execute {
                        ProgressManager.checkCanceled()
                        try {
                            val response = request.sendRequest()
                            val result = request.preprocessResponse(response.get())
                            resultFuture.complete(result)
                        } catch (e: Exception) {
                            resultFuture.completeExceptionally(e)
                        }

                    }
                    return resultFuture
                }
            }
            return null

//            val executor = this.executorService
//            synchronized(executor) {
//                // 这里可能需要添加一些代码
//            }
//
//            val future: CompletableFuture<Result?>?
//            try {
//                if (executor.isShutdown) {
//                    return null
//                }
//
//                future = CompletableFuture()
//                executor.execute {
//                    ProgressManager.checkCanceled()
//                    try {
//                        val response = request.sendRequest()
//                        val result = request.preprocessResponse(response.get())
//                        future.complete(result)
//                    } catch (e: Exception) {
//                        future.completeExceptionally(e)
//                    }
//
//
//
//                }
//            } finally {
//                // 这里可能需要添加一些代码
//            }
//
//            return future
        }
    }

    @RequiresBackgroundThread
    private fun <T> waitForResponse(future: Future<T>, debugName: String): T? {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < SYNC_RESPONSE_TIMEOUT) {
            ProgressManager.checkCanceled()

            try {
                return future.get(10L, TimeUnit.MILLISECONDS)
            } catch (e: TimeoutException) {
                // Ignore and continue waiting
            } catch (e: ExecutionException) {
                LOG.warn("Error response from server: ${e.cause}", null)
                return null
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }

        val timeoutSeconds = SYNC_RESPONSE_TIMEOUT / 1000
        LOG.info("No response from the server in $timeoutSeconds seconds for: $debugName")
        return null
    }

    @RequiresBackgroundThread
    private fun <T> V(
        future: Future<T?>,
        cancelOnPCE: Boolean,
        debugName: String,
        timeoutMs: Int
    ): T? {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                ProgressManager.checkCanceled()
            } catch (ex: ProcessCanceledException) {
                if (cancelOnPCE) {
                    future.cancel(true)
                }
                throw ex
            }
            try {
                return future.get(10L, TimeUnit.MILLISECONDS)
            } catch (ex: TimeoutException) {
                // Continue waiting
            } catch (ex: ExecutionException) {
                lspServer.logWarn("Error response from server: ${ex.cause}", null)
                return null
            } catch (ex: InterruptedException) {
                throw RuntimeException(ex)
            }
        }
        lspServer.logInfo("No response from the server in $timeoutMs ms for: $debugName")
        future.cancel(true)
        return null

    }



    @RequiresReadLock
    @RequiresBackgroundThread
    private fun <Result> sendOffsetBasedRequestSync(
        file: VirtualFile,
        offset: Int,
        timeoutMs: Int = 1000,
        request: (VirtualFile, Document, Int) -> LspRequest<*, Result>
    ): Result? {
        val document = FileDocumentManager.getInstance().getDocument(file)
        if (document == null) {
            lspServer.logError("No document for file $file")
            return null
        } else {
            val virtualFile = (file as? VirtualFileWindow)?.delegate ?: file
            val doc = (document as? DocumentWindow)?.delegate ?: document
            val hostOffset = (document as? DocumentWindow)?.injectedToHost(offset) ?: offset
            return sendRequestSync(request(virtualFile, doc, hostOffset), timeoutMs)
        }
    }


    @RequiresBackgroundThread
    fun <ServerResponse, Result> sendRequestSync(
        lspRequest: LspRequest<ServerResponse, Result>,
        timeoutMs: Int
    ): Result? {
        val future = processRequest(lspRequest)
        return if (future != null) {
            waitForResponse(future, true, lspRequest.toString(), timeoutMs)
        } else {
            null
        }
    }

    private fun <T> waitForResponse(
        future: Future<T>,
        cancelOnInterrupt: Boolean,
        requestInfo: String,
        timeoutMs: Int
    ): T? {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                ProgressManager.checkCanceled()
            } catch (e: ProcessCanceledException) {
                if (cancelOnInterrupt) {
                    future.cancel(true)
                }
                throw e
            }

            try {
                return future.get(10L, TimeUnit.MILLISECONDS)
            } catch (e: TimeoutException) {
                // Continue waiting
            } catch (e: ExecutionException) {
                lspServer.logWarn("Error response from server: ${e.cause}", null)
                return null
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }

        lspServer.logInfo("No response from the server in $timeoutMs ms for: $requestInfo")
        future.cancel(true)
        return null
    }

//    private fun <ServerResponse, Result> performRequest(lspRequest: LspRequest<ServerResponse, Result>): CompletableFuture<Result>? {
//        if (!lspServer.hasCapabilities()) {
//            lspServer.logDebug("Server not initialized yet, skipping request $lspRequest")
//            return null
//        } else {
//            synchronized(executorService) {
//                if (!executorService.isShutdown) {
//                    val completableFuture = CompletableFuture<Result>()
//                    executorService.execute(LspRequestExecutorImpl::V)
//                    return completableFuture
//                }
//            }
//        }
//        return null
//    }


    override fun <Result> sendRequestAsync(lspRequest: LspRequest<*, Result>, resultConsumer: (Result?) -> Unit) {
        processRequest(lspRequest) { resultConsumer.invoke(it) }
    }

    @RequiresBackgroundThread
    override fun <ServerResponse, Result> sendRequestSync(lspRequest: LspRequest<ServerResponse, Result>): Result? {
        val future = processRequest(lspRequest)
        return future.let { waitForResponse(future as Future<*>, lspRequest.toString()) as Result? }

    }

    private inner class HoverResultCache {
        private var lastFilePath: String = ""

        private var lastPsiModificationCount: Long = -1L

        private var lastResult: TextRangeAndMarkupContent? = null

        @RequiresReadLock
        @RequiresBackgroundThread
        fun getHoverInformation(
            file: VirtualFile,
            offset: Int,
            timeoutMs: Int
        ): TextRangeAndMarkupContent? {
            ProgressManager.checkCanceled()
            if (file is VirtualFileWindow) {
                val logger = Logger.getInstance(HoverResultCache::class.java)
                logger.error("VirtualFileWindow not expected here")
                return null
            } else {
                val project = lspServer.project
                val psiManager = PsiManager.getInstance(project)
                val modificationCount = psiManager.modificationTracker.modificationCount
                val filePath = file.path
                var lastResult = this.lastResult

                if (lastResult != null && lastPsiModificationCount == modificationCount &&
                    lastFilePath == filePath && lastResult.textRange.contains(offset)
                ) {
                    return lastResult
                } else {
                    val lspRequest = sendOffsetBasedRequestSync(
                        file,
                        offset,
                        timeoutMs
                    ) { hostFile, hostDocument, hostOffset ->
                        LspHoverRequest(lspServer, hostFile, hostDocument, hostOffset)
                    }

                    lastResult = lspRequest
                    lastPsiModificationCount = modificationCount
                    lastFilePath = filePath
                    return lspRequest
                }
            }
        }
    }

    fun shutdownNow() {
        this.executorService.shutdownNow()
    }
}
