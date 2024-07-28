package com.linqingying.lsp.impl

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.containers.MultiMap
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler
import java.nio.file.PathMatcher

internal class LspDynamicCapabilities {


    companion object {


        @JvmField
        val prepareCallHierarchy = "textDocument/prepareCallHierarchy" to CallHierarchyRegistrationOptions::class.java

        @JvmField
        val codeAction = "textDocument/codeAction" to CodeActionRegistrationOptions::class.java

        @JvmField
        val codeLens = "textDocument/codeLens" to CodeLensRegistrationOptions::class.java

        @JvmField
        val completion = "textDocument/completion" to CompletionRegistrationOptions::class.java

        @JvmField
        val declaration = "textDocument/declaration" to DeclarationRegistrationOptions::class.java

        @JvmField
        val definition = "textDocument/definition" to DefinitionRegistrationOptions::class.java

        @JvmField
        val diagnostic = "textDocument/diagnostic" to DiagnosticRegistrationOptions::class.java

        @JvmField
        val didChangeWatchedFiles =
            "workspace/didChangeWatchedFiles" to DidChangeWatchedFilesRegistrationOptions::class.java

        @JvmField
        val formatting = "textDocument/formatting" to DocumentFormattingRegistrationOptions::class.java

        @JvmField
        val documentHighlight = "textDocument/documentHighlight" to DocumentHighlightRegistrationOptions::class.java

        @JvmField
        val documentLink = "textDocument/documentLink" to DocumentLinkRegistrationOptions::class.java

        @JvmField
        val onTypeFormatting =
            "textDocument/onTypeFormatting" to DocumentOnTypeFormattingRegistrationOptions::class.java

        @JvmField
        val rangeFormatting = "textDocument/rangeFormatting" to DocumentRangeFormattingRegistrationOptions::class.java

        @JvmField
        val documentSymbol = "textDocument/documentSymbol" to DocumentSymbolRegistrationOptions::class.java

        @JvmField
        val executeCommand = "workspace/executeCommand" to ExecuteCommandRegistrationOptions::class.java

        @JvmField
        val hover = "textDocument/hover" to HoverRegistrationOptions::class.java

        @JvmField
        val implementation = "textDocument/implementation" to ImplementationRegistrationOptions::class.java

        @JvmField
        val inlayHint = "textDocument/inlayHint" to InlayHintRegistrationOptions::class.java

        @JvmField
        val inlineValue = "textDocument/inlineValue" to InlineValueRegistrationOptions::class.java

        @JvmField
        val linkedEditingRange = "textDocument/linkedEditingRange" to LinkedEditingRangeRegistrationOptions::class.java

        @JvmField
        val moniker = "textDocument/moniker" to MonikerRegistrationOptions::class.java

        @JvmField
        val references = "textDocument/references" to ReferenceRegistrationOptions::class.java

        @JvmField
        val selectionRange = "textDocument/selectionRange" to SelectionRangeRegistrationOptions::class.java

        @JvmField
        val signatureHelp = "textDocument/signatureHelp" to SignatureHelpRegistrationOptions::class.java

        @JvmField
        val didChange = "textDocument/didChange" to TextDocumentChangeRegistrationOptions::class.java

        @JvmField
        val didOpen = "textDocument/didOpen" to TextDocumentRegistrationOptions::class.java

        @JvmField
        val willSave = "textDocument/willSave" to TextDocumentRegistrationOptions::class.java

        @JvmField
        val willSaveWaitUntil = "textDocument/willSaveWaitUntil" to TextDocumentRegistrationOptions::class.java

        @JvmField
        val didClose = "textDocument/didClose" to TextDocumentRegistrationOptions::class.java

        @JvmField
        val didSave = "textDocument/didSave" to TextDocumentSaveRegistrationOptions::class.java

        @JvmField
        val typeDefinition = "textDocument/typeDefinition" to TypeDefinitionRegistrationOptions::class.java

        @JvmField
        val prepareTypeHierarchy = "textDocument/prepareTypeHierarchy" to TypeHierarchyRegistrationOptions::class.java

        @JvmField
        val symbol = "workspace/symbol" to WorkspaceSymbolRegistrationOptions::class.java

        //        textDocument/semanticTokens/full
        @JvmField
        val semanticTokensFull = "textDocument/semanticTokens/full" to SemanticTokensServerFull::class.java

        private var capabilityToLsp4jRegistrationOptionsClass = mapOf(
            prepareCallHierarchy,
            codeAction,
            codeLens,
            completion,
            declaration,
            definition,
            diagnostic,
            didChangeWatchedFiles,
            formatting,
            documentHighlight,
            documentLink,
            onTypeFormatting,
            rangeFormatting,
            documentSymbol,
            executeCommand,
            hover,
            implementation,
            inlayHint,
            inlineValue,
            linkedEditingRange,
            moniker,
            references,
            selectionRange,
            signatureHelp,
            didChange,
            didOpen,
            willSave,
            willSaveWaitUntil,
            didClose,
            didSave,
            typeDefinition,
            prepareTypeHierarchy,
            symbol,
            semanticTokensFull
        )
        val LOG = Logger.getInstance(LspDynamicCapabilities::class.java)

        private fun checkCondition(func: Function1<Any, Boolean>, obj: Any): Boolean {
            return func.invoke(obj)
        }
    }

    private val capabilityToInfo: MultiMap<String, CapabilityInfo> =
        MultiMap.createConcurrent<String, CapabilityInfo>().apply {

        }

    init {
//        capabilityToInfo.putValue(
//            didChangeWatchedFiles.first, CapabilityInfo(
//                didChangeWatchedFiles.first, null, null
//            )
//        )

    }

    private val patternToPathMatcherCache: MutableMap<String, PathMatcher> = mutableMapOf()
    private val gson: Gson = MessageJsonHandler(emptyMap()).gson

    fun hasCapability(capability: String): Boolean {

        return this.capabilityToInfo.containsKey(capability)
    }

    fun hasCapability(capabilityAndOptionsClass: Pair<String, Class<*>>): Boolean {

        return this.hasCapability(capabilityAndOptionsClass.first)

    }

    /**
     * 获取指定能力的注册选项
     */
    fun <T> getCapabilityRegistrationOptions(capabilityAndOptionsClass: Pair<String, Class<T>>): List<T> {
        val capabilityInfos = capabilityToInfo[capabilityAndOptionsClass.first]
        val registrationOptions = capabilityInfos.mapNotNull { it.lsp4jRegistrationOptions }
        return registrationOptions.filterIsInstance(capabilityAndOptionsClass.second)
    }


    fun getPathMatcherCaching(globPattern: String): PathMatcher {

        requireNotNull(globPattern) { "globPattern must not be null" }
        return patternToPathMatcherCache.getOrPut(globPattern) { getPathMatcher(globPattern) }
    }

    private fun processRegistration(registration: Registration): Any? {
        val registerOptions = registration.registerOptions
        val optionsAsJson = registerOptions as? JsonObject ?: return null
        val lsp4jRegistrationOptionsClass =
            capabilityToLsp4jRegistrationOptionsClass[registration.method]

        return try {
            gson.fromJson((optionsAsJson as JsonElement), lsp4jRegistrationOptionsClass)

        } catch (e: JsonSyntaxException) {

            if (lsp4jRegistrationOptionsClass != null) {
                LOG.warn("Failed to convert options of `${registration.method}` to ${lsp4jRegistrationOptionsClass.simpleName} class: $e\n$optionsAsJson")
            }
            null
        }
    }

    fun registerCapability(registration: Registration) {
        val capabilityInfo =
            (if (registration.registerOptions is JsonObject) registration.registerOptions as JsonObject else null)?.let {
                CapabilityInfo(
                    registration.id,
                    it,
                    processRegistration(registration)
                )
            }
        capabilityToInfo.putValue(registration.method, capabilityInfo)
    }

    fun unregisterCapability(unregistration: Unregistration) {
        val capabilities = capabilityToInfo[unregistration.method]
        capabilities.removeIf { it.registrationId == unregistration.id }
        if (capabilities.isEmpty()) {
            capabilityToInfo.remove(unregistration.method)
        }
    }

    data class CapabilityInfo(
        val registrationId: String, val rawRegistrationOptions: JsonObject?,
        val lsp4jRegistrationOptions: Any?
    )

}


